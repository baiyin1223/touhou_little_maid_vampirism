package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncVampireMaidPacket {
    private final int entityId;
    private final boolean isVampire;
    private final int vampireLevel;

    public SyncVampireMaidPacket(int entityId, boolean isVampire, int vampireLevel) {
        this.entityId = entityId;
        this.isVampire = isVampire;
        this.vampireLevel = vampireLevel;
    }

    public static void encode(SyncVampireMaidPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.isVampire);
        buf.writeInt(msg.vampireLevel);
    }

    public static SyncVampireMaidPacket decode(FriendlyByteBuf buf) {
        return new SyncVampireMaidPacket(buf.readInt(), buf.readBoolean(), buf.readInt());
    }

    public static void handle(SyncVampireMaidPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
                if (entity != null) {
                    ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
                        cap.setVampire(msg.isVampire);
                        cap.setVampireLevel(msg.vampireLevel);
                        TLMVMain.LOGGER.debug("[SyncVampireMaidPacket] Synced vampire state for entity {}: isVampire={}, level={}",
                                msg.entityId, msg.isVampire, msg.vampireLevel);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
