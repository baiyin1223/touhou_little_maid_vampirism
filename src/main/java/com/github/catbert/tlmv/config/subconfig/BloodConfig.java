package com.github.catbert.tlmv.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BloodConfig {
    public static ModConfigSpec.BooleanValue BLOOD_DECAY_ENABLED;
    public static ModConfigSpec.IntValue LOW_BLOOD_WEAKNESS_THRESHOLD;
    public static ModConfigSpec.IntValue BLOOD_STARVATION_HP_DAMAGE;
    public static ModConfigSpec.IntValue BLOOD_STARVATION_HP_INTERVAL;
    public static ModConfigSpec.BooleanValue AUTO_FEED_ENABLED;
    public static ModConfigSpec.IntValue AUTO_FEED_INTERVAL;
    public static ModConfigSpec.IntValue AUTO_FEED_RANGE;
    public static ModConfigSpec.IntValue AUTO_FEED_EXTRACT_AMOUNT;

    // Garlic effect settings
    public static ModConfigSpec.BooleanValue GARLIC_DAMAGE_ENABLED;
    public static ModConfigSpec.DoubleValue GARLIC_HP_DAMAGE;
    public static ModConfigSpec.IntValue GARLIC_HP_INTERVAL;
    public static ModConfigSpec.IntValue GARLIC_BLOOD_DECAY_INTERVAL;

    public static void init(ModConfigSpec.Builder builder) {
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

        // Garlic effect settings
        GARLIC_DAMAGE_ENABLED = builder
                .comment("Whether garlic effect damages vampire maids")
                .define("garlicDamageEnabled", true);
        GARLIC_HP_DAMAGE = builder
                .comment("HP damage amount per garlic tick")
                .defineInRange("garlicHpDamage", 1.0, 0.0, 20.0);
        GARLIC_HP_INTERVAL = builder
                .comment("Interval in ticks for garlic HP damage (50 = 2.5 seconds)")
                .defineInRange("garlicHpInterval", 50, 1, 1200);
        GARLIC_BLOOD_DECAY_INTERVAL = builder
                .comment("Interval in ticks for extra blood decay from garlic (200 = 10 seconds)")
                .defineInRange("garlicBloodDecayInterval", 200, 1, 1200);
        builder.pop();
    }
}
