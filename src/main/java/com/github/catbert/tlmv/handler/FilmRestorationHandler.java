package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class FilmRestorationHandler {

    private static final String ATTACHMENTS_KEY = "neoforge:attachments";
    private static final String CAP_KEY = TLMVMain.MOD_ID + ":vampire_maid";

    @SubscribeEvent
    public static void onFilmToMaid(MaidAndItemTransformEvent.ToMaid event) {
        CompoundTag data = event.getData();

        if (!data.contains(ATTACHMENTS_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag attachments = data.getCompound(ATTACHMENTS_KEY);
        if (!attachments.contains(CAP_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag capData = attachments.getCompound(CAP_KEY);
        VampireMaidCapability.CODEC.parse(NbtOps.INSTANCE, capData).result().ifPresent(restoredCap -> {
            VampireMaidCapability currentCap = event.getMaid().getData(ModAttachments.VAMPIRE_MAID.get());
            currentCap.setVampire(restoredCap.isVampire());
            currentCap.setHadSanguinare(restoredCap.hasHadSanguinare());
            currentCap.setVampireLevel(restoredCap.getVampireLevel());
            currentCap.setLastKnownBlood(restoredCap.getLastKnownBlood());
            currentCap.setBloodDecayTimer(restoredCap.getBloodDecayTimer());
            currentCap.setSlowDecayTimer(restoredCap.getSlowDecayTimer());
            currentCap.setStarvationTimer(restoredCap.getStarvationTimer());
            currentCap.setGarlicHpTicker(restoredCap.getGarlicHpTicker());
            currentCap.setGarlicBloodTicker(restoredCap.getGarlicBloodTicker());
            currentCap.setAutoFeedTimer(restoredCap.getAutoFeedTimer());
            currentCap.setAutoFeedTargetUUID(restoredCap.getAutoFeedTargetUUID());
            currentCap.setAutoFeedState(restoredCap.getAutoFeedState());
            currentCap.setAutoFeedMoveTimer(restoredCap.getAutoFeedMoveTimer());
            TLMVMain.LOGGER.debug("Restored vampire maid attachment from film: isVampire={}, level={}",
                    restoredCap.isVampire(), restoredCap.getVampireLevel());
        });
    }
}
