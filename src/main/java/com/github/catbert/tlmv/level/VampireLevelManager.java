package com.github.catbert.tlmv.level;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class VampireLevelManager {

    private static final UUID MOD_HP = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456701");
    private static final UUID MOD_ATK = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456702");
    private static final UUID MOD_ARMOR = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456703");
    private static final UUID MOD_KB = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef0123456704");

    // Delta values (bonus above vanilla base): HP base=20, ATK base=2, ARMOR base=0, KB base=0
    private static final double[] HP_DELTAS = {0, 20, 40, 60, 80};
    private static final double[] ATK_DELTAS = {0, 2, 4, 6, 8};
    private static final double[] ARMOR_DELTAS = {4, 8, 12, 16, 20};
    private static final double[] KB_DELTAS = {2, 4, 6, 8, 10};

    public static void applyLevel(EntityMaid maid, int level) {
        applyLevelAttributes(maid, level);
        applyLevelBuffs(maid, level);
    }

    public static void applyLevelAttributes(EntityMaid maid, int level) {
        double oldMaxHealth = maid.getMaxHealth();

        removeLevelModifiers(maid);

        if (level <= 0) return;

        int idx = Math.max(0, Math.min(level - 1, 4));

        addModifier(maid, Attributes.MAX_HEALTH, MOD_HP, "vampire_hp", HP_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.ATTACK_DAMAGE, MOD_ATK, "vampire_atk", ATK_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.ARMOR, MOD_ARMOR, "vampire_armor", ARMOR_DELTAS[idx], AttributeModifier.Operation.ADDITION);
        addModifier(maid, Attributes.KNOCKBACK_RESISTANCE, MOD_KB, "vampire_kb", KB_DELTAS[idx], AttributeModifier.Operation.ADDITION);

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
