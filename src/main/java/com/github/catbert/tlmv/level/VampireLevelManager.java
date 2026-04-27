package com.github.catbert.tlmv.level;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class VampireLevelManager {

    public static void applyLevel(EntityMaid maid, int level) {
        applyLevelAttributes(maid, level);
        applyLevelBuffs(maid, level);
    }

    public static void applyLevelAttributes(EntityMaid maid, int level) {
        double oldMaxHealth = maid.getMaxHealth();

        double[] healthValues = {20, 40, 60, 80, 100};
        double[] armorValues = {4, 8, 12, 16, 20};
        double[] knockbackValues = {2, 4, 6, 8, 10};
        double[] attackValues = {2, 4, 6, 8, 10};

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
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, true, false));
            }
            case 3 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 2, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            }
            case 4 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 2, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            }
            case 5 -> {
                maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 2, true, false));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            }
            default -> {
                // 1级及默认无buff
            }
        }
    }
}
