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

public class VampireTargetFinder {

    private static final double VAMPIRE_SCAN_RANGE = 20.0;
    private static final TagKey<EntityType<?>> VAMPIRE_TAG = TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), new ResourceLocation("vampirism", "vampire"));

    public static Optional<? extends LivingEntity> findVampireTarget(EntityMaid maid) {
        LivingEntity lastAttacker = maid.getLastHurtByMob();
        if (lastAttacker != null && lastAttacker.isAlive() && maid.canSee(lastAttacker)) {
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

    public static boolean isVampire(LivingEntity entity) {
        if (entity.getType().is(VAMPIRE_TAG)) return true;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && "vampirism".equals(key.getNamespace()) && key.getPath().contains("vampire");
    }
}
