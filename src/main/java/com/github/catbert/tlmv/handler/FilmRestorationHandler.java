package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.catbert.tlmv.network.TLMVNetwork;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class FilmRestorationHandler {

    static final String CAP_KEY = ModCapabilities.VAMPIRE_MAID_CAP_ID.toString();
    private static final Map<UUID, CompoundTag> pendingRestore = new HashMap<>();

    @SubscribeEvent
    public static void onFilmToMaid(MaidAndItemTransformEvent.ToMaid event) {
        CompoundTag data = event.getData();

        if (!data.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag forgeCaps = data.getCompound("ForgeCaps");
        if (!forgeCaps.contains(CAP_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag capData = forgeCaps.getCompound(CAP_KEY);
        ModCapabilities.getVampireMaid(event.getMaid()).ifPresent(cap -> cap.deserializeNBT(capData));
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        CompoundTag capData = pendingRestore.remove(maid.getUUID());
        if (capData == null) return;
        ModCapabilities.getVampireMaid(maid).ifPresent(cap -> {
            cap.deserializeNBT(capData);
            Entity entity = maid;
            TLMVNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                    new SyncVampireMaidPacket(entity.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel()));
        });
    }

    /**
     * 从 ForgeCaps NBT 数据中恢复吸血鬼女仆 capability。
     * 供外部或未来事件监听调用。
     */
    public static void restoreFromNbt(net.minecraft.world.entity.Entity maid, CompoundTag data) {
        if (!data.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag forgeCaps = data.getCompound("ForgeCaps");
        if (!forgeCaps.contains(CAP_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag capData = forgeCaps.getCompound(CAP_KEY);
        ModCapabilities.getVampireMaid(maid).ifPresent(cap -> {
            cap.deserializeNBT(capData);
            TLMVNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                    new SyncVampireMaidPacket(maid.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel()));
        });
    }
}
