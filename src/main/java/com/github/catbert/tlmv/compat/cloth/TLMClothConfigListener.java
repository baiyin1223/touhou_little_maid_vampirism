package com.github.catbert.tlmv.compat.cloth;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.config.subconfig.*;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TLMClothConfigListener {

    @SubscribeEvent
    public static void onAddClothConfig(AddClothConfigEvent event) {
        ConfigBuilder root = event.getRoot();
        ConfigEntryBuilder entryBuilder = event.getEntryBuilder();

        ConfigCategory category = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_vampirism.title"));

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.infection.enable_fang"),
                        InfectionConfig.ENABLE_FANG_ITEM_INFECTION.get())
                .setDefaultValue(true)
                .setSaveConsumer(InfectionConfig.ENABLE_FANG_ITEM_INFECTION::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.sun_damage.enable"),
                        SunDamageConfig.ENABLE_SUN_DAMAGE.get())
                .setDefaultValue(true)
                .setSaveConsumer(SunDamageConfig.ENABLE_SUN_DAMAGE::set)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_vampirism.task.collect_blood.enabled"),
                        TaskConfig.COLLECT_BLOOD_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(TaskConfig.COLLECT_BLOOD_ENABLED::set)
                .build());
    }
}
