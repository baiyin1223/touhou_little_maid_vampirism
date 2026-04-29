package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncVampireMaidPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncVampireMaidPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "sync_vampire_maid"));

    public static final StreamCodec<FriendlyByteBuf, SyncVampireMaidPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncVampireMaidPacket::write, SyncVampireMaidPacket::new);

    private final int entityId;
    private final boolean isVampire;
    private final int vampireLevel;

    public SyncVampireMaidPacket(int entityId, boolean isVampire, int vampireLevel) {
        this.entityId = entityId;
        this.isVampire = isVampire;
        this.vampireLevel = vampireLevel;
    }

    public SyncVampireMaidPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.isVampire = buf.readBoolean();
        this.vampireLevel = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(isVampire);
        buf.writeInt(vampireLevel);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncVampireMaidPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId);
                if (entity != null) {
                    VampireMaidCapability cap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
                    cap.setVampire(packet.isVampire);
                    cap.setVampireLevel(packet.vampireLevel);
                    TLMVMain.LOGGER.debug("[SyncVampireMaidPacket] Synced vampire state for entity {}: isVampire={}, level={}",
                            packet.entityId, packet.isVampire, packet.vampireLevel);
                }
            }
        });
    }
}
