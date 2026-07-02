package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCrossbowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

/**
 * 猎人弩攻击行为。Lv.3+ 使用 Vampirism 更高级弩（非基础弩）时额外追加一支箭矢。
 */
public class MaidHunterCrossbowAttack extends MaidCrossbowAttack {

    private boolean extraFired = false;

    @Override
    public void crossbowAttack(EntityMaid shooter, LivingEntity target) {
        ItemStack crossbow = shooter.getMainHandItem();
        boolean isAdvanced = isNonBasic(crossbow);
        boolean canAdvance = shooter.getData(ModAttachments.VAMPIRE_MAID.get()).getHunterLevel() >= 3;

        super.crossbowAttack(shooter, target);

        // Lv.3+ vampirism 非基础弩：原版速度修复（Vampirism 丢弃了 3.15×倍率）+ 追加额外箭矢
        if (isAdvanced && canAdvance && !extraFired) {
            // 检查箭矢是否刚刚射出（charged 被清空）
            if (crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).isEmpty()) {
                Vec3 look = shooter.getViewVector(1.0F);
                AbstractArrow extra = ProjectileUtil.getMobArrow(shooter, new ItemStack(Items.ARROW), 1.0F, crossbow);
                if (shooter.level() instanceof ServerLevel serverLevel) {
                    // 原版标准倍率: 3.15F × 1.6F = 5.04F
                    extra.shoot(look.x, look.y + 0.1, look.z, 1.6F * 3.15F, 3.0F);
                    serverLevel.addFreshEntity(extra);
                }
                extraFired = true;
            }
        }
    }

    private boolean isNonBasic(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return false;
        String path = key.getPath();
        return "vampirism".equals(key.getNamespace()) && !path.contains("basic");
    }
}

