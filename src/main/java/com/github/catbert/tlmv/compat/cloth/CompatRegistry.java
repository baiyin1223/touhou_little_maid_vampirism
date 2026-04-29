package com.github.catbert.tlmv.compat.cloth;

import com.github.catbert.tlmv.TLMVMain;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(CLOTH_CONFIG) && FMLEnvironment.dist == Dist.CLIENT) {
                ModContainer modContainer = ModList.get().getModContainerById(TLMVMain.MOD_ID).orElse(null);
                if (modContainer != null) {
                    MenuIntegration.registerModsPage(modContainer);
                }
            }
        });
    }
}
