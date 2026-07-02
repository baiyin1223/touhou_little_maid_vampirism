package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class FilmRestorationHandler {

    private static final String ATTACHMENTS_KEY = "neoforge:attachments";
    private static final String CAP_KEY = TLMVMain.MOD_ID + ":vampire_maid";
    private static final Map<UUID, VampireMaidCapability> pendingRestore = new HashMap<>();

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
            // Store for deferred restore after entity joins world
            pendingRestore.put(event.getMaid().getUUID(), restoredCap);
            // Apply immediately (pre-world-join)
            applyRestored(event.getMaid(), restoredCap);
        });
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        VampireMaidCapability restoredCap = pendingRestore.remove(maid.getUUID());
        if (restoredCap == null) return;
        applyRestored(maid, restoredCap);
        // Sync after entity fully joins world (tracked by players)
        VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
        PacketDistributor.sendToPlayersTrackingEntity(maid,
                new SyncVampireMaidPacket(maid.getId(), cap.isVampire(), cap.getVampireLevel(),
                        cap.isHunter(), cap.getHunterLevel()));
    }

    private static void applyRestored(EntityMaid maid, VampireMaidCapability restoredCap) {
        VampireMaidCapability currentCap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
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
        currentCap.setHunter(restoredCap.isHunter());
        currentCap.setHunterLevel(restoredCap.getHunterLevel());
    }
}
