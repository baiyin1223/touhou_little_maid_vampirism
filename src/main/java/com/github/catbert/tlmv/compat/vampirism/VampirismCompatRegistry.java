package com.github.catbert.tlmv.compat.vampirism;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VampirismCompatRegistry {

    @SubscribeEvent
    public static void onEnqueue(final InterModEnqueueEvent event) {
        if (!VampirismHelper.isVampirismLoaded()) {
            return;
        }

        event.enqueueWork(() -> {
            try {
                VampirismAPI.entityRegistry().addCustomExtendedCreature(EntityMaid.class, MaidExtendedCreature::new);
                TLMVMain.LOGGER.info("[TLMV] Registered MaidExtendedCreature for EntityMaid");
            } catch (Exception e) {
                TLMVMain.LOGGER.error("[TLMV] Failed to register MaidExtendedCreature", e);
            }
        });
    }
}
