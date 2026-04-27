package com.github.catbert.tlmv.config.subconfig;

import net.minecraftforge.common.ForgeConfigSpec;

public final class InfectionConfig {
    public static ForgeConfigSpec.BooleanValue ENABLE_MONSTER_INFECTION;
    public static ForgeConfigSpec.BooleanValue ENABLE_FANG_ITEM_INFECTION;
    public static ForgeConfigSpec.IntValue INFECTION_DURATION;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("infection");
        ENABLE_MONSTER_INFECTION = builder
                .comment("Whether vampire monsters can infect maids by attacking")
                .define("enableMonsterInfection", false);
        ENABLE_FANG_ITEM_INFECTION = builder
                .comment("Whether vampire fang items can infect maids")
                .define("enableFangItemInfection", true);
        INFECTION_DURATION = builder
                .comment("Sanguinare buff duration in ticks (default 10 minutes)")
                .defineInRange("infectionDuration", 12000, 1200, 72000);
        builder.pop();
    }
}
