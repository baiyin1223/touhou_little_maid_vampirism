package com.github.catbert.tlmv.compat.vampirism;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class VampirismCompatRegistry {

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        if (!VampirismHelper.isVampirismLoaded()) {
            return;
        }

        // In Vampirism 1.21, addCustomExtendedCreature was removed from IVampirismEntityRegistry.
        // ExtendedCreature is now managed through NeoForge's data attachment system.
        // MaidExtendedCreature functionality (canBeBitten override) is handled via
        // VampireNeutralityHandler instead.
        TLMVMain.LOGGER.info("[TLMV] Vampirism compat initialized (1.21 attachment-based system)");
    }
}
