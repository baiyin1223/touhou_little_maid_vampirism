package com.github.catbert.tlmv.client;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.client.gui.MaidAltarScreen;
import com.github.catbert.tlmv.client.renderer.MaidAltarBESR;
import com.github.catbert.tlmv.init.ModBlockEntities;
import com.github.catbert.tlmv.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MAID_ALTAR.get(), MaidAltarScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MAID_ALTAR.get(), MaidAltarBESR::new);
    }
}
