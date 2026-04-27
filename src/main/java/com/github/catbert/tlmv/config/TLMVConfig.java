package com.github.catbert.tlmv.config;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.config.subconfig.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Configuration entry point. Delegates to sub-configs following TLM's pattern.
 * Retains backward-compatible aliases for references not yet migrated.
 */
public class TLMVConfig {
    // Backward-compatible aliases for existing code references
    public static ForgeConfigSpec.BooleanValue ENABLE_MONSTER_INFECTION;
    public static ForgeConfigSpec.BooleanValue ENABLE_FANG_ITEM_INFECTION;
    public static ForgeConfigSpec.IntValue INFECTION_DURATION;

    public static ForgeConfigSpec.BooleanValue ENABLE_SUN_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SUN_DAMAGE_AMOUNT;
    public static ForgeConfigSpec.IntValue SUN_DAMAGE_FREQUENCY;
    public static ForgeConfigSpec.BooleanValue RESPECT_SUNSCREEN;

    public static ForgeConfigSpec.IntValue LOW_BLOOD_WEAKNESS_THRESHOLD;

    public static ForgeConfigSpec.BooleanValue COLLECT_BLOOD_ENABLED;
    public static ForgeConfigSpec.IntValue COLLECT_BLOOD_RANGE;
    public static ForgeConfigSpec.IntValue COLLECT_BLOOD_EXTRACT;
    public static ForgeConfigSpec.IntValue COLLECT_BLOOD_COOLDOWN;

    public static void register() {
        TLMVMain.LOGGER.info("Registering TLMVConfig...");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());
        TLMVMain.LOGGER.info("TLMVConfig registration completed.");

        // Populate backward-compatible aliases
        ENABLE_MONSTER_INFECTION = InfectionConfig.ENABLE_MONSTER_INFECTION;
        ENABLE_FANG_ITEM_INFECTION = InfectionConfig.ENABLE_FANG_ITEM_INFECTION;
        INFECTION_DURATION = InfectionConfig.INFECTION_DURATION;

        ENABLE_SUN_DAMAGE = SunDamageConfig.ENABLE_SUN_DAMAGE;
        SUN_DAMAGE_AMOUNT = SunDamageConfig.SUN_DAMAGE_AMOUNT;
        SUN_DAMAGE_FREQUENCY = SunDamageConfig.SUN_DAMAGE_FREQUENCY;
        RESPECT_SUNSCREEN = SunDamageConfig.RESPECT_SUNSCREEN;

        LOW_BLOOD_WEAKNESS_THRESHOLD = BloodConfig.LOW_BLOOD_WEAKNESS_THRESHOLD;

        COLLECT_BLOOD_ENABLED = TaskConfig.COLLECT_BLOOD_ENABLED;
        COLLECT_BLOOD_RANGE = TaskConfig.COLLECT_BLOOD_RANGE;
        COLLECT_BLOOD_EXTRACT = TaskConfig.COLLECT_BLOOD_EXTRACT;
        COLLECT_BLOOD_COOLDOWN = TaskConfig.COLLECT_BLOOD_COOLDOWN;
    }
}
