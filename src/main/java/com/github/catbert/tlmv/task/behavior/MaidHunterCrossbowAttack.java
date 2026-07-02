package com.github.catbert.tlmv.task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class MaidHunterCrossbowAttack extends Behavior<EntityMaid> {

    private boolean extraFired = false;

    public MaidHunterCrossbowAttack() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid maid) {
        return isHoldingChargedCrossbow(maid);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTime) {
        LivingEntity target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null) return;

        ItemStack crossbow = maid.getMainHandItem();
        if (!(crossbow.getItem() instanceof CrossbowItem)) return;

        boolean isEnhanced = !crossbow.getItem().equals(net.minecraft.world.item.Items.CROSSBOW);
        extraFired = false;

        if (isEnhanced && !crossbow.isEmpty()) {
            maid.performCrossbowAttack(maid, 1.6F * 3.15F);

            if (crossbow.getItem() instanceof CrossbowItem) {
                CrossbowItem.setCharged(crossbow, true);
                extraFired = true;
                maid.performCrossbowAttack(maid, 1.6F * 3.15F);
            }
        } else {
            maid.performCrossbowAttack(maid, 1.6F * 3.15F);
        }

        CrossbowItem.setCharged(crossbow, false);
        ProjectileUtil.getWeaponHoldingHand(maid, item -> item instanceof CrossbowItem);
    }

    private static boolean isHoldingChargedCrossbow(EntityMaid maid) {
        return maid.getMainHandItem().getItem() instanceof CrossbowItem && CrossbowItem.isCharged(maid.getMainHandItem());
    }
}
