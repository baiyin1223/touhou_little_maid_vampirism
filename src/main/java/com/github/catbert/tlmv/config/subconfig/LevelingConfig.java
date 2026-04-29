package com.github.catbert.tlmv.config.subconfig;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LevelingConfig {

    // Level 1 -> 2
    public static ForgeConfigSpec.IntValue L1_PURE_BLOOD_LEVEL;
    public static ForgeConfigSpec.IntValue L1_PURE_BLOOD_QUANTITY;
    public static ForgeConfigSpec.IntValue L1_HUMAN_HEART_QUANTITY;
    public static ForgeConfigSpec.IntValue L1_VAMPIRE_BOOK_QUANTITY;
    public static ForgeConfigSpec.IntValue L1_STRUCTURE_POINTS;

    // Level 2 -> 3
    public static ForgeConfigSpec.IntValue L2_PURE_BLOOD_LEVEL;
    public static ForgeConfigSpec.IntValue L2_PURE_BLOOD_QUANTITY;
    public static ForgeConfigSpec.IntValue L2_HUMAN_HEART_QUANTITY;
    public static ForgeConfigSpec.IntValue L2_VAMPIRE_BOOK_QUANTITY;
    public static ForgeConfigSpec.IntValue L2_STRUCTURE_POINTS;

    // Level 3 -> 4
    public static ForgeConfigSpec.IntValue L3_PURE_BLOOD_LEVEL;
    public static ForgeConfigSpec.IntValue L3_PURE_BLOOD_QUANTITY;
    public static ForgeConfigSpec.IntValue L3_HUMAN_HEART_QUANTITY;
    public static ForgeConfigSpec.IntValue L3_VAMPIRE_BOOK_QUANTITY;
    public static ForgeConfigSpec.IntValue L3_STRUCTURE_POINTS;

    // Level 4 -> 5
    public static ForgeConfigSpec.IntValue L4_PURE_BLOOD_LEVEL;
    public static ForgeConfigSpec.IntValue L4_PURE_BLOOD_QUANTITY;
    public static ForgeConfigSpec.IntValue L4_HUMAN_HEART_QUANTITY;
    public static ForgeConfigSpec.IntValue L4_VAMPIRE_BOOK_QUANTITY;
    public static ForgeConfigSpec.IntValue L4_STRUCTURE_POINTS;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("leveling");

        builder.push("level_1_to_2");
        L1_PURE_BLOOD_LEVEL = builder.comment("Required pure blood level for maid level 1 -> 2").defineInRange("pureBloodLevel", 2, 0, 4);
        L1_PURE_BLOOD_QUANTITY = builder.comment("Required pure blood quantity for maid level 1 -> 2").defineInRange("pureBloodQuantity", 10, 0, 64);
        L1_HUMAN_HEART_QUANTITY = builder.comment("Required human heart quantity for maid level 1 -> 2").defineInRange("humanHeartQuantity", 10, 0, 64);
        L1_VAMPIRE_BOOK_QUANTITY = builder.comment("Required vampire book quantity for maid level 1 -> 2").defineInRange("vampireBookQuantity", 1, 0, 64);
        L1_STRUCTURE_POINTS = builder.comment("Required structure points for maid level 1 -> 2").defineInRange("structurePoints", 18, 0, 100);
        builder.pop();

        builder.push("level_2_to_3");
        L2_PURE_BLOOD_LEVEL = builder.comment("Required pure blood level for maid level 2 -> 3").defineInRange("pureBloodLevel", 2, 0, 4);
        L2_PURE_BLOOD_QUANTITY = builder.comment("Required pure blood quantity for maid level 2 -> 3").defineInRange("pureBloodQuantity", 15, 0, 64);
        L2_HUMAN_HEART_QUANTITY = builder.comment("Required human heart quantity for maid level 2 -> 3").defineInRange("humanHeartQuantity", 20, 0, 64);
        L2_VAMPIRE_BOOK_QUANTITY = builder.comment("Required vampire book quantity for maid level 2 -> 3").defineInRange("vampireBookQuantity", 1, 0, 64);
        L2_STRUCTURE_POINTS = builder.comment("Required structure points for maid level 2 -> 3").defineInRange("structurePoints", 26, 0, 100);
        builder.pop();

        builder.push("level_3_to_4");
        L3_PURE_BLOOD_LEVEL = builder.comment("Required pure blood level for maid level 3 -> 4").defineInRange("pureBloodLevel", 3, 0, 4);
        L3_PURE_BLOOD_QUANTITY = builder.comment("Required pure blood quantity for maid level 3 -> 4").defineInRange("pureBloodQuantity", 20, 0, 64);
        L3_HUMAN_HEART_QUANTITY = builder.comment("Required human heart quantity for maid level 3 -> 4").defineInRange("humanHeartQuantity", 30, 0, 64);
        L3_VAMPIRE_BOOK_QUANTITY = builder.comment("Required vampire book quantity for maid level 3 -> 4").defineInRange("vampireBookQuantity", 1, 0, 64);
        L3_STRUCTURE_POINTS = builder.comment("Required structure points for maid level 3 -> 4").defineInRange("structurePoints", 35, 0, 100);
        builder.pop();

        builder.push("level_4_to_5");
        L4_PURE_BLOOD_LEVEL = builder.comment("Required pure blood level for maid level 4 -> 5").defineInRange("pureBloodLevel", 4, 0, 4);
        L4_PURE_BLOOD_QUANTITY = builder.comment("Required pure blood quantity for maid level 4 -> 5").defineInRange("pureBloodQuantity", 25, 0, 64);
        L4_HUMAN_HEART_QUANTITY = builder.comment("Required human heart quantity for maid level 4 -> 5").defineInRange("humanHeartQuantity", 40, 0, 64);
        L4_VAMPIRE_BOOK_QUANTITY = builder.comment("Required vampire book quantity for maid level 4 -> 5").defineInRange("vampireBookQuantity", 1, 0, 64);
        L4_STRUCTURE_POINTS = builder.comment("Required structure points for maid level 4 -> 5").defineInRange("structurePoints", 44, 0, 100);
        builder.pop();

        builder.pop();
    }

    public static LevelRequirements getRequirements(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> new LevelRequirements(
                    L1_PURE_BLOOD_LEVEL.get(), L1_PURE_BLOOD_QUANTITY.get(),
                    L1_HUMAN_HEART_QUANTITY.get(), L1_VAMPIRE_BOOK_QUANTITY.get(),
                    L1_STRUCTURE_POINTS.get()
            );
            case 3 -> new LevelRequirements(
                    L2_PURE_BLOOD_LEVEL.get(), L2_PURE_BLOOD_QUANTITY.get(),
                    L2_HUMAN_HEART_QUANTITY.get(), L2_VAMPIRE_BOOK_QUANTITY.get(),
                    L2_STRUCTURE_POINTS.get()
            );
            case 4 -> new LevelRequirements(
                    L3_PURE_BLOOD_LEVEL.get(), L3_PURE_BLOOD_QUANTITY.get(),
                    L3_HUMAN_HEART_QUANTITY.get(), L3_VAMPIRE_BOOK_QUANTITY.get(),
                    L3_STRUCTURE_POINTS.get()
            );
            case 5 -> new LevelRequirements(
                    L4_PURE_BLOOD_LEVEL.get(), L4_PURE_BLOOD_QUANTITY.get(),
                    L4_HUMAN_HEART_QUANTITY.get(), L4_VAMPIRE_BOOK_QUANTITY.get(),
                    L4_STRUCTURE_POINTS.get()
            );
            default -> null;
        };
    }

    public record LevelRequirements(int pureBloodLevel, int pureBloodQuantity, int humanHeartQuantity, int vampireBookQuantity, int structurePoints) {
    }
}
