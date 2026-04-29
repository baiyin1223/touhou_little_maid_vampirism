package com.github.catbert.tlmv.compat.cloth;

import com.github.catbert.tlmv.config.subconfig.*;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;

public class MenuIntegration {

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create()
                .setTitle(Component.literal("Touhou Little Maid Vampirism"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);

        ConfigEntryBuilder entryBuilder = root.entryBuilder();

        infectionConfig(root, entryBuilder);
        sunDamageConfig(root, entryBuilder);
        bloodConfig(root, entryBuilder);
        taskConfig(root, entryBuilder);

        return root;
    }

    private static void infectionConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_vampirism.infection"));

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.infection.enable_monster"),
                        InfectionConfig.ENABLE_MONSTER_INFECTION.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.infection.enable_monster.tooltip"))
                .setSaveConsumer(InfectionConfig.ENABLE_MONSTER_INFECTION::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.infection.enable_fang"),
                        InfectionConfig.ENABLE_FANG_ITEM_INFECTION.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.infection.enable_fang.tooltip"))
                .setSaveConsumer(InfectionConfig.ENABLE_FANG_ITEM_INFECTION::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.infection.duration"),
                        InfectionConfig.INFECTION_DURATION.get(), 1200, 72000)
                .setDefaultValue(12000)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.infection.duration.tooltip"))
                .setSaveConsumer(InfectionConfig.INFECTION_DURATION::set)
                .build());
    }

    private static void sunDamageConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_vampirism.sun_damage"));

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.sun_damage.enable"),
                        SunDamageConfig.ENABLE_SUN_DAMAGE.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.sun_damage.enable.tooltip"))
                .setSaveConsumer(SunDamageConfig.ENABLE_SUN_DAMAGE::set)
                .build());

        category.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.touhou_little_maid_vampirism.sun_damage.amount"),
                        SunDamageConfig.SUN_DAMAGE_AMOUNT.get())
                .setDefaultValue(1.0)
                .setMin(0.1).setMax(10.0)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.sun_damage.amount.tooltip"))
                .setSaveConsumer(SunDamageConfig.SUN_DAMAGE_AMOUNT::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.sun_damage.frequency"),
                        SunDamageConfig.SUN_DAMAGE_FREQUENCY.get(), 5, 200)
                .setDefaultValue(20)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.sun_damage.frequency.tooltip"))
                .setSaveConsumer(SunDamageConfig.SUN_DAMAGE_FREQUENCY::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.sun_damage.respect_sunscreen"),
                        SunDamageConfig.RESPECT_SUNSCREEN.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.sun_damage.respect_sunscreen.tooltip"))
                .setSaveConsumer(SunDamageConfig.RESPECT_SUNSCREEN::set)
                .build());
    }

    private static void bloodConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_vampirism.blood"));

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.decay_enabled"),
                        BloodConfig.BLOOD_DECAY_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.decay_enabled.tooltip"))
                .setSaveConsumer(BloodConfig.BLOOD_DECAY_ENABLED::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.weakness_threshold"),
                        BloodConfig.LOW_BLOOD_WEAKNESS_THRESHOLD.get(), 0, 10)
                .setDefaultValue(4)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.weakness_threshold.tooltip"))
                .setSaveConsumer(BloodConfig.LOW_BLOOD_WEAKNESS_THRESHOLD::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.starvation_hp_damage"),
                        BloodConfig.BLOOD_STARVATION_HP_DAMAGE.get(), 1, 20)
                .setDefaultValue(1)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.starvation_hp_damage.tooltip"))
                .setSaveConsumer(BloodConfig.BLOOD_STARVATION_HP_DAMAGE::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.starvation_hp_interval"),
                        BloodConfig.BLOOD_STARVATION_HP_INTERVAL.get(), 20, 1200)
                .setDefaultValue(60)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.starvation_hp_interval.tooltip"))
                .setSaveConsumer(BloodConfig.BLOOD_STARVATION_HP_INTERVAL::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_enabled"),
                        BloodConfig.AUTO_FEED_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_enabled.tooltip"))
                .setSaveConsumer(BloodConfig.AUTO_FEED_ENABLED::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_interval"),
                        BloodConfig.AUTO_FEED_INTERVAL.get(), 200, 6000)
                .setDefaultValue(300)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_interval.tooltip"))
                .setSaveConsumer(BloodConfig.AUTO_FEED_INTERVAL::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_range"),
                        BloodConfig.AUTO_FEED_RANGE.get(), 8, 32)
                .setDefaultValue(16)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_range.tooltip"))
                .setSaveConsumer(BloodConfig.AUTO_FEED_RANGE::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_extract_amount"),
                        BloodConfig.AUTO_FEED_EXTRACT_AMOUNT.get(), 1, 10)
                .setDefaultValue(8)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.auto_feed_extract_amount.tooltip"))
                .setSaveConsumer(BloodConfig.AUTO_FEED_EXTRACT_AMOUNT::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_damage_enabled"),
                        BloodConfig.GARLIC_DAMAGE_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_damage_enabled.tooltip"))
                .setSaveConsumer(BloodConfig.GARLIC_DAMAGE_ENABLED::set)
                .build());

        category.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_hp_damage"),
                        BloodConfig.GARLIC_HP_DAMAGE.get())
                .setDefaultValue(1.0)
                .setMin(0.0).setMax(20.0)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_hp_damage.tooltip"))
                .setSaveConsumer(BloodConfig.GARLIC_HP_DAMAGE::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_hp_interval"),
                        BloodConfig.GARLIC_HP_INTERVAL.get(), 1, 1200)
                .setDefaultValue(50)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_hp_interval.tooltip"))
                .setSaveConsumer(BloodConfig.GARLIC_HP_INTERVAL::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_blood_decay_interval"),
                        BloodConfig.GARLIC_BLOOD_DECAY_INTERVAL.get(), 1, 1200)
                .setDefaultValue(200)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.blood.garlic_blood_decay_interval.tooltip"))
                .setSaveConsumer(BloodConfig.GARLIC_BLOOD_DECAY_INTERVAL::set)
                .build());
    }

    private static void taskConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_vampirism.task"));

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.enabled"),
                        TaskConfig.COLLECT_BLOOD_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.enabled.tooltip"))
                .setSaveConsumer(TaskConfig.COLLECT_BLOOD_ENABLED::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.range"),
                        TaskConfig.COLLECT_BLOOD_RANGE.get(), 8, 32)
                .setDefaultValue(16)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.range.tooltip"))
                .setSaveConsumer(TaskConfig.COLLECT_BLOOD_RANGE::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.extract"),
                        TaskConfig.COLLECT_BLOOD_EXTRACT.get(), 1, 10)
                .setDefaultValue(6)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.extract.tooltip"))
                .setSaveConsumer(TaskConfig.COLLECT_BLOOD_EXTRACT::set)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.cooldown"),
                        TaskConfig.COLLECT_BLOOD_COOLDOWN.get(), 10, 100)
                .setDefaultValue(30)
                .setTooltip(Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.cooldown.tooltip"))
                .setSaveConsumer(TaskConfig.COLLECT_BLOOD_COOLDOWN::set)
                .build());
    }

    public static void registerModsPage(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> getConfigBuilder()
                        .setParentScreen(parent)
                        .build()
        );
    }
}
