package com.github.catbert.tlmv.compat.cloth;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";

    @SubscribeEvent
    public static void onEnqueue(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(CLOTH_CONFIG) && FMLEnvironment.dist == Dist.CLIENT) {
                MenuIntegration.registerModsPage();
            }
        });
    }
}
