package com.github.catbert.tlmv.config.subconfig;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BloodConfig {
    public static ForgeConfigSpec.BooleanValue BLOOD_DECAY_ENABLED;
    public static ForgeConfigSpec.IntValue LOW_BLOOD_WEAKNESS_THRESHOLD;
    public static ForgeConfigSpec.IntValue BLOOD_STARVATION_HP_DAMAGE;
    public static ForgeConfigSpec.IntValue BLOOD_STARVATION_HP_INTERVAL;
    public static ForgeConfigSpec.BooleanValue AUTO_FEED_ENABLED;
    public static ForgeConfigSpec.IntValue AUTO_FEED_INTERVAL;
    public static ForgeConfigSpec.IntValue AUTO_FEED_RANGE;
    public static ForgeConfigSpec.IntValue AUTO_FEED_EXTRACT_AMOUNT;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("blood");
        BLOOD_DECAY_ENABLED = builder
                .comment("Whether vampire maid blood naturally decays over time")
                .define("bloodDecayEnabled", true);
        LOW_BLOOD_WEAKNESS_THRESHOLD = builder
                .comment("Blood level threshold for weakness effect")
                .defineInRange("lowBloodWeaknessThreshold", 4, 0, 10);
        BLOOD_STARVATION_HP_DAMAGE = builder
                .comment("HP damage amount when blood level reaches 0")
                .defineInRange("bloodStarvationHpDamage", 1, 1, 20);
        BLOOD_STARVATION_HP_INTERVAL = builder
                .comment("Interval in ticks for HP damage when blood level is 0 (60 = 3 seconds)")
                .defineInRange("bloodStarvationHpInterval", 60, 20, 1200);
        AUTO_FEED_ENABLED = builder
                .comment("Whether vampire maid automatically hunts nearby creatures for blood when hungry and has no blood food")
                .define("autoFeedEnabled", true);
        AUTO_FEED_INTERVAL = builder
                .comment("Interval in ticks between auto-feed attempts (300 = 15 seconds)")
                .defineInRange("autoFeedInterval", 300, 200, 6000);
        AUTO_FEED_RANGE = builder
                .comment("Search range for auto-feed targets (blocks)")
                .defineInRange("autoFeedRange", 16, 8, 32);
        AUTO_FEED_EXTRACT_AMOUNT = builder
                .comment("Blood amount extracted per auto-feed action")
                .defineInRange("autoFeedExtractAmount", 8, 1, 10);
        builder.pop();
    }
}
