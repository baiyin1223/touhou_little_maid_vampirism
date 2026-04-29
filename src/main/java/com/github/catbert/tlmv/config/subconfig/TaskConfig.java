package com.github.catbert.tlmv.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TaskConfig {
    public static ModConfigSpec.BooleanValue COLLECT_BLOOD_ENABLED;
    public static ModConfigSpec.IntValue COLLECT_BLOOD_RANGE;
    public static ModConfigSpec.IntValue COLLECT_BLOOD_EXTRACT;
    public static ModConfigSpec.IntValue COLLECT_BLOOD_COOLDOWN;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("task");

        builder.push("collect_blood");
        COLLECT_BLOOD_ENABLED = builder
                .comment("Enable blood collection task for vampire maids")
                .define("enabled", true);
        COLLECT_BLOOD_RANGE = builder
                .comment("Search range for blood collection target (blocks)")
                .defineInRange("searchRange", 16, 8, 32);
        COLLECT_BLOOD_EXTRACT = builder
                .comment("Max blood extracted per action")
                .defineInRange("extractAmount", 6, 1, 10);
        COLLECT_BLOOD_COOLDOWN = builder
                .comment("Cooldown between blood collection actions (ticks, 30 = 1.5s)")
                .defineInRange("cooldownTicks", 30, 10, 100);
        builder.pop();

        builder.pop();
    }
}
