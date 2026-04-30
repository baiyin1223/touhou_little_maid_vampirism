package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class FilmRestorationHandler {

    static final String CAP_KEY = ModCapabilities.VAMPIRE_MAID_CAP_ID.toString();

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
        ModCapabilities.getVampireMaid(event.getMaid()).ifPresent(cap -> {
            cap.deserializeNBT(capData);
            TLMVMain.LOGGER.debug("Restored vampire maid capability from film: isVampire={}, level={}",
                    cap.isVampire(), cap.getVampireLevel());
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
            TLMVMain.LOGGER.debug("Restored vampire maid capability from film: isVampire={}, level={}",
                    cap.isVampire(), cap.getVampireLevel());
        });
    }
}
