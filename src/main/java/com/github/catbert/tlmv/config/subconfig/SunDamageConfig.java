package com.github.catbert.tlmv.config.subconfig;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SunDamageConfig {
    public static ForgeConfigSpec.BooleanValue ENABLE_SUN_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SUN_DAMAGE_AMOUNT;
    public static ForgeConfigSpec.IntValue SUN_DAMAGE_FREQUENCY;
    public static ForgeConfigSpec.BooleanValue RESPECT_SUNSCREEN;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("sun_damage");
        ENABLE_SUN_DAMAGE = builder
                .comment("Whether sun damage is enabled for vampire maids")
                .define("enableSunDamage", true);
        SUN_DAMAGE_AMOUNT = builder
                .comment("Amount of damage per sun tick")
                .defineInRange("sunDamageAmount", 1.0, 0.1, 10.0);
        SUN_DAMAGE_FREQUENCY = builder
                .comment("Frequency of sun damage in ticks")
                .defineInRange("sunDamageFrequency", 20, 5, 200);
        RESPECT_SUNSCREEN = builder
                .comment("Whether to respect Vampirism sunscreen buff")
                .define("respectSunscreen", true);
        builder.pop();
    }
}
