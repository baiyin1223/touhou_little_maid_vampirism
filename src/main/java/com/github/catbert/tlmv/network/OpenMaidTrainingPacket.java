package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.init.ModMenuTypes;
import com.github.catbert.tlmv.inventory.MaidTrainingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenMaidTrainingPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenMaidTrainingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "open_maid_training"));

    public static final StreamCodec<FriendlyByteBuf, OpenMaidTrainingPacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenMaidTrainingPacket::write, OpenMaidTrainingPacket::new);

    private final BlockPos trainerPos;

    public OpenMaidTrainingPacket(BlockPos trainerPos) {
        this.trainerPos = trainerPos;
    }

    public OpenMaidTrainingPacket(FriendlyByteBuf buf) {
        this.trainerPos = buf.readBlockPos();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(trainerPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMaidTrainingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inv, p) -> new MaidTrainingMenu(containerId, inv, packet.trainerPos),
                    Component.translatable("gui.touhou_little_maid_vampirism.maid_training")
            ), buf -> buf.writeBlockPos(packet.trainerPos));
        });
    }
}
