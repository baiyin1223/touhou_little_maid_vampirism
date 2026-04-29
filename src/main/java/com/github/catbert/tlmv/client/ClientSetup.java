package com.github.catbert.tlmv.client;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.client.gui.MaidAltarScreen;
import com.github.catbert.tlmv.client.renderer.MaidAltarBESR;
import com.github.catbert.tlmv.init.ModBlockEntities;
import com.github.catbert.tlmv.init.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.MAID_ALTAR.get(), MaidAltarScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MAID_ALTAR.get(), MaidAltarBESR::new);
    }
}
