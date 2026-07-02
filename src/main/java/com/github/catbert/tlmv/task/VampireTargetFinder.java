package com.github.catbert.tlmv.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * 吸血鬼目标查找工具。绕过 TLM 默认传感器延迟，
 * 通过实体类型注册名和 Vampirism 吸血鬼 tag 直接识别目标。
 */
public class VampireTargetFinder {

    private static final double VAMPIRE_SCAN_RANGE = 20.0;
    private static final TagKey<EntityType<?>> VAMPIRE_TAG = TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), ResourceLocation.fromNamespaceAndPath("vampirism", "vampire"));

    public static Optional<? extends LivingEntity> findVampireTarget(EntityMaid maid) {
        LivingEntity lastAttacker = maid.getLastHurtByMob();
        if (lastAttacker != null && lastAttacker.isAlive() && maid.canAttack(lastAttacker) && maid.canSee(lastAttacker)) {
            if (isVampire(lastAttacker)) {
                return Optional.of(lastAttacker);
            }
        }

        AABB searchBox = maid.getBoundingBox().inflate(VAMPIRE_SCAN_RANGE);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : maid.level().getEntitiesOfClass(PathfinderMob.class, searchBox, e -> e.isAlive())) {
            if (entity == maid) continue;
            if (!isVampire(entity)) continue;
            if (!maid.canSee(entity)) continue;

            double dist = maid.distanceToSqr(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        return Optional.ofNullable(closest);
    }

    /** 通过实体类型 tag 和注册名判断是否为吸血鬼（供外部 canAttack 使用） */
    public static boolean isVampire(LivingEntity entity) {
        // 方法1: Vampirism 的实体 tag（覆盖 vampire/vampire_imob/advanced_vampire 等所有变体）
        if (entity.getType().is(VAMPIRE_TAG)) return true;

        // 方法2: 兜底——注册名包含 vampire 且命名空间为 vampirism
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && "vampirism".equals(key.getNamespace()) && key.getPath().contains("vampire");
    }
}

