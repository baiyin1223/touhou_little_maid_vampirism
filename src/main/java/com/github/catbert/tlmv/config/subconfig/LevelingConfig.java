package com.github.catbert.tlmv.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LevelingConfig {

    // Level 1 -> 2
    public static ModConfigSpec.IntValue L1_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue L1_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue L1_HUMAN_HEART_QUANTITY;
    public static ModConfigSpec.IntValue L1_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue L1_STRUCTURE_POINTS;

    // Level 2 -> 3
    public static ModConfigSpec.IntValue L2_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue L2_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue L2_HUMAN_HEART_QUANTITY;
    public static ModConfigSpec.IntValue L2_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue L2_STRUCTURE_POINTS;

    // Level 3 -> 4
    public static ModConfigSpec.IntValue L3_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue L3_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue L3_HUMAN_HEART_QUANTITY;
    public static ModConfigSpec.IntValue L3_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue L3_STRUCTURE_POINTS;

    // Level 4 -> 5
    public static ModConfigSpec.IntValue L4_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue L4_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue L4_HUMAN_HEART_QUANTITY;
    public static ModConfigSpec.IntValue L4_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue L4_STRUCTURE_POINTS;

    public static void init(ModConfigSpec.Builder builder) {
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

        // Hunter maid leveling
        builder.push("hunter_level_1_to_2");
        HL1_IRON_QUANTITY = builder.comment("Required iron ingots for hunter maid 1->2").defineInRange("ironQuantity", 10, 0, 64);
        HL1_GOLD_QUANTITY = builder.comment("Required gold ingots for hunter maid 1->2").defineInRange("goldQuantity", 5, 0, 64);
        HL1_PURE_BLOOD_LEVEL = builder.comment("Required pure blood level for hunter maid 1->2").defineInRange("pureBloodLevel", 1, 0, 4);
        HL1_PURE_BLOOD_QUANTITY = builder.comment("Required pure blood quantity for hunter maid 1->2").defineInRange("pureBloodQuantity", 5, 0, 64);
        HL1_VAMPIRE_BOOK_QUANTITY = builder.comment("Required vampire book quantity for hunter maid 1->2").defineInRange("vampireBookQuantity", 1, 0, 64);
        HL1_SOUL_ORB_QUANTITY = builder.comment("Required soul orb quantity for hunter maid 1->2").defineInRange("soulOrbQuantity", 3, 0, 64);
        builder.pop();

        builder.push("hunter_level_2_to_3");
        HL2_IRON_QUANTITY = builder.defineInRange("ironQuantity", 15, 0, 64);
        HL2_GOLD_QUANTITY = builder.defineInRange("goldQuantity", 10, 0, 64);
        HL2_PURE_BLOOD_LEVEL = builder.defineInRange("pureBloodLevel", 2, 0, 4);
        HL2_PURE_BLOOD_QUANTITY = builder.defineInRange("pureBloodQuantity", 10, 0, 64);
        HL2_VAMPIRE_BOOK_QUANTITY = builder.defineInRange("vampireBookQuantity", 1, 0, 64);
        HL2_SOUL_ORB_QUANTITY = builder.defineInRange("soulOrbQuantity", 5, 0, 64);
        builder.pop();

        builder.push("hunter_level_3_to_4");
        HL3_IRON_QUANTITY = builder.defineInRange("ironQuantity", 20, 0, 64);
        HL3_GOLD_QUANTITY = builder.defineInRange("goldQuantity", 15, 0, 64);
        HL3_PURE_BLOOD_LEVEL = builder.defineInRange("pureBloodLevel", 3, 0, 4);
        HL3_PURE_BLOOD_QUANTITY = builder.defineInRange("pureBloodQuantity", 15, 0, 64);
        HL3_VAMPIRE_BOOK_QUANTITY = builder.defineInRange("vampireBookQuantity", 1, 0, 64);
        HL3_SOUL_ORB_QUANTITY = builder.defineInRange("soulOrbQuantity", 8, 0, 64);
        builder.pop();

        builder.push("hunter_level_4_to_5");
        HL4_IRON_QUANTITY = builder.defineInRange("ironQuantity", 30, 0, 64);
        HL4_GOLD_QUANTITY = builder.defineInRange("goldQuantity", 20, 0, 64);
        HL4_PURE_BLOOD_LEVEL = builder.defineInRange("pureBloodLevel", 4, 0, 4);
        HL4_PURE_BLOOD_QUANTITY = builder.defineInRange("pureBloodQuantity", 20, 0, 64);
        HL4_VAMPIRE_BOOK_QUANTITY = builder.defineInRange("vampireBookQuantity", 1, 0, 64);
        HL4_SOUL_ORB_QUANTITY = builder.defineInRange("soulOrbQuantity", 12, 0, 64);
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

    // ===== Hunter Maid Leveling =====

    // Level 1 -> 2
    public static ModConfigSpec.IntValue HL1_IRON_QUANTITY;
    public static ModConfigSpec.IntValue HL1_GOLD_QUANTITY;
    public static ModConfigSpec.IntValue HL1_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue HL1_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue HL1_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue HL1_SOUL_ORB_QUANTITY;

    // Level 2 -> 3
    public static ModConfigSpec.IntValue HL2_IRON_QUANTITY;
    public static ModConfigSpec.IntValue HL2_GOLD_QUANTITY;
    public static ModConfigSpec.IntValue HL2_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue HL2_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue HL2_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue HL2_SOUL_ORB_QUANTITY;

    // Level 3 -> 4
    public static ModConfigSpec.IntValue HL3_IRON_QUANTITY;
    public static ModConfigSpec.IntValue HL3_GOLD_QUANTITY;
    public static ModConfigSpec.IntValue HL3_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue HL3_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue HL3_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue HL3_SOUL_ORB_QUANTITY;

    // Level 4 -> 5
    public static ModConfigSpec.IntValue HL4_IRON_QUANTITY;
    public static ModConfigSpec.IntValue HL4_GOLD_QUANTITY;
    public static ModConfigSpec.IntValue HL4_PURE_BLOOD_LEVEL;
    public static ModConfigSpec.IntValue HL4_PURE_BLOOD_QUANTITY;
    public static ModConfigSpec.IntValue HL4_VAMPIRE_BOOK_QUANTITY;
    public static ModConfigSpec.IntValue HL4_SOUL_ORB_QUANTITY;

    public static record HunterLevelRequirements(int ironQuantity, int goldQuantity, int pureBloodLevel, int pureBloodQuantity, int vampireBookQuantity, int soulOrbQuantity) {
    }

    public static HunterLevelRequirements getHunterRequirements(int targetLevel) {
        return switch (targetLevel) {
            case 2 -> new HunterLevelRequirements(
                    HL1_IRON_QUANTITY.get(), HL1_GOLD_QUANTITY.get(),
                    HL1_PURE_BLOOD_LEVEL.get(), HL1_PURE_BLOOD_QUANTITY.get(),
                    HL1_VAMPIRE_BOOK_QUANTITY.get(), HL1_SOUL_ORB_QUANTITY.get()
            );
            case 3 -> new HunterLevelRequirements(
                    HL2_IRON_QUANTITY.get(), HL2_GOLD_QUANTITY.get(),
                    HL2_PURE_BLOOD_LEVEL.get(), HL2_PURE_BLOOD_QUANTITY.get(),
                    HL2_VAMPIRE_BOOK_QUANTITY.get(), HL2_SOUL_ORB_QUANTITY.get()
            );
            case 4 -> new HunterLevelRequirements(
                    HL3_IRON_QUANTITY.get(), HL3_GOLD_QUANTITY.get(),
                    HL3_PURE_BLOOD_LEVEL.get(), HL3_PURE_BLOOD_QUANTITY.get(),
                    HL3_VAMPIRE_BOOK_QUANTITY.get(), HL3_SOUL_ORB_QUANTITY.get()
            );
            case 5 -> new HunterLevelRequirements(
                    HL4_IRON_QUANTITY.get(), HL4_GOLD_QUANTITY.get(),
                    HL4_PURE_BLOOD_LEVEL.get(), HL4_PURE_BLOOD_QUANTITY.get(),
                    HL4_VAMPIRE_BOOK_QUANTITY.get(), HL4_SOUL_ORB_QUANTITY.get()
            );
            default -> null;
        };
    }
}
