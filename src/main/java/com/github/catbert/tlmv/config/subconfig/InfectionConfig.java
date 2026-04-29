package com.github.catbert.tlmv.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class InfectionConfig {
    public static ModConfigSpec.BooleanValue ENABLE_MONSTER_INFECTION;
    public static ModConfigSpec.BooleanValue ENABLE_FANG_ITEM_INFECTION;
    public static ModConfigSpec.IntValue INFECTION_DURATION;

    public static void init(ModConfigSpec.Builder builder) {
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
