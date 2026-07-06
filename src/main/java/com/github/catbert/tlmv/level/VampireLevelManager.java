package com.github.catbert.tlmv.level;

import com.github.catbert.tlmv.TLMVMain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class VampireLevelManager {

    private static final ResourceLocation MOD_HP = ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "vampire_hp");
    private static final ResourceLocation MOD_ATK = ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "vampire_atk");
    private static final ResourceLocation MOD_ARMOR = ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "vampire_armor");
    private static final ResourceLocation MOD_KB = ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "vampire_kb");

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

        // Always remove existing modifiers first (idempotent)
        removeLevelModifiers(maid);

        if (level <= 0) return; // No modifiers for level 0 — TLM favorability controls base

        int idx = Math.max(0, Math.min(level - 1, 4));

        addModifier(maid, Attributes.MAX_HEALTH, MOD_HP, HP_DELTAS[idx], AttributeModifier.Operation.ADD_VALUE);
        addModifier(maid, Attributes.ATTACK_DAMAGE, MOD_ATK, ATK_DELTAS[idx], AttributeModifier.Operation.ADD_VALUE);
        addModifier(maid, Attributes.ARMOR, MOD_ARMOR, ARMOR_DELTAS[idx], AttributeModifier.Operation.ADD_VALUE);
        addModifier(maid, Attributes.KNOCKBACK_RESISTANCE, MOD_KB, KB_DELTAS[idx], AttributeModifier.Operation.ADD_VALUE);

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

    private static void addModifier(EntityMaid maid, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        var instance = maid.getAttribute(attr);
        if (instance != null && amount != 0) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    private static void removeModifier(EntityMaid maid, Holder<Attribute> attr, ResourceLocation id) {
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
