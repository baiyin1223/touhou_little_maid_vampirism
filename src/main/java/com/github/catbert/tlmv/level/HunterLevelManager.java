package com.github.catbert.tlmv.level;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class HunterLevelManager {

    private static final UUID MOD_HP = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f01234567011");
    private static final UUID MOD_ATK = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f01234567012");
    private static final UUID MOD_ARMOR = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f01234567013");
    private static final UUID MOD_KB = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f01234567014");

    // Delta values (bonus above vanilla base): HP base=20, ATK base=2, ARMOR base=0, KB base=0
    private static final double[] HP_DELTAS = {0, 10, 25, 40, 60};
    private static final double[] ATK_DELTAS = {1, 3, 6, 8, 11};
    private static final double[] ARMOR_DELTAS = {2, 6, 10, 14, 18};
    private static final double[] KB_DELTAS = {1, 3, 5, 7, 9};

    public static void applyLevel(EntityMaid maid, int level) {
        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || !cap.isHunter()) return;
        cap.setHunterLevel(level);
        applyLevelAttributes(maid, level);
        applyLevelBuffs(maid, level);
    }

    public static void applyLevelAttributes(EntityMaid maid, int level) {
        double oldMaxHealth = maid.getMaxHealth();

        removeLevelModifiers(maid);

        if (level <= 0) return;

        int idx = Math.max(0, Math.min(level - 1, 4));

        addModifier(maid, Attributes.MAX_HEALTH, MOD_HP, "hunter_hp", HP_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.ATTACK_DAMAGE, MOD_ATK, "hunter_atk", ATK_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.ARMOR, MOD_ARMOR, "hunter_armor", ARMOR_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.KNOCKBACK_RESISTANCE, MOD_KB, "hunter_kb", KB_DELTAS[idx], AttributeModifier.Operation.ADDITION);

        double newMaxHealth = maid.getMaxHealth();
        if (newMaxHealth > oldMaxHealth) {
            maid.setHealth(maid.getMaxHealth());
        } else {
            maid.setHealth(Math.min(maid.getHealth(), maid.getMaxHealth()));
        }
    }

    public static void removeLevelModifiers(EntityMaid maid) {
        removeModifier(maid, Attributes.MAX_HEALTH, MOD_HP);
        removeModifier(maid, Attributes.ATTACK_DAMAGE, MOD_ATK);
        removeModifier(maid, Attributes.ARMOR, MOD_ARMOR);
        removeModifier(maid, Attributes.KNOCKBACK_RESISTANCE, MOD_KB);
    }

    private static void addModifier(EntityMaid maid, net.minecraft.world.entity.ai.attributes.Attribute attr,
                                     UUID id, String name, double amount, AttributeModifier.Operation op) {
        var instance = maid.getAttribute(attr);
        if (instance != null && amount != 0) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    private static void removeModifier(EntityMaid maid, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id) {
        var instance = maid.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(id);
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

