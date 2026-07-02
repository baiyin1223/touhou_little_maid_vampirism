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
    private final boolean isHunter;
    private final int hunterLevel;

    public SyncVampireMaidPacket(int entityId, boolean isVampire, int vampireLevel) {
        this(entityId, isVampire, vampireLevel, false, 0);
    }

    public SyncVampireMaidPacket(int entityId, boolean isVampire, int vampireLevel, boolean isHunter, int hunterLevel) {
        this.entityId = entityId;
        this.isVampire = isVampire;
        this.vampireLevel = vampireLevel;
        this.isHunter = isHunter;
        this.hunterLevel = hunterLevel;
    }

    public static void encode(SyncVampireMaidPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.isVampire);
        buf.writeInt(msg.vampireLevel);
        buf.writeBoolean(msg.isHunter);
        buf.writeInt(msg.hunterLevel);
    }

    public static SyncVampireMaidPacket decode(FriendlyByteBuf buf) {
        return new SyncVampireMaidPacket(buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readInt());
    }

    public static void handle(SyncVampireMaidPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
                if (entity != null) {
                    ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
                        cap.setVampire(msg.isVampire);
                        cap.setVampireLevel(msg.vampireLevel);
                        cap.setHunter(msg.isHunter);
                        cap.setHunterLevel(msg.hunterLevel);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
