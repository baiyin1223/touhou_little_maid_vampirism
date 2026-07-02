package com.github.catbert.tlmv.level;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 猎人女仆等级管理器。管理 1-5 级的属性增益和 buff 效果。
 */
public class HunterLevelManager {

    public static void applyLevel(EntityMaid maid, int level) {
        applyLevelAttributes(maid, level);
        applyLevelBuffs(maid, level);
    }

    public static void applyLevelAttributes(EntityMaid maid, int level) {
        double oldMaxHealth = maid.getMaxHealth();

        // Hunter: slightly lower HP/armor than vampire, but higher attack
        double[] healthValues = {20, 30, 45, 60, 80};
        double[] armorValues = {2, 6, 10, 14, 18};
        double[] knockbackValues = {1, 3, 5, 7, 9};
        double[] attackValues = {3, 5, 8, 10, 13};

        int idx = Math.max(0, Math.min(level - 1, 4));

        maid.getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthValues[idx]);
        maid.getAttribute(Attributes.ARMOR).setBaseValue(armorValues[idx]);
        maid.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(knockbackValues[idx]);
        maid.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackValues[idx]);

        double newMaxHealth = maid.getMaxHealth();
        if (newMaxHealth > oldMaxHealth) {
            maid.setHealth(maid.getMaxHealth());
        } else {
            maid.setHealth(Math.min(maid.getHealth(), maid.getMaxHealth()));
        }
    }

    public static void applyLevelBuffs(EntityMaid maid, int level) {
        maid.removeEffect(MobEffects.REGENERATION);
        maid.removeEffect(MobEffects.DAMAGE_BOOST);
        maid.removeEffect(MobEffects.MOVEMENT_SPEED);
        maid.removeEffect(MobEffects.FIRE_RESISTANCE);

        switch (level) {
            case 2 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            }
            case 3 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            }
            case 4 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1, true, false));
            }
            case 5 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 2, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1, true, false));
            }
            default -> {
                // Level 1: no buffs
            }
        }
    }
}
