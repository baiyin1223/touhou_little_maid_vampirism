package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import com.github.catbert.tlmv.inventory.MaidTrainingMenu;

import java.util.function.Supplier;

public class OpenMaidTrainingPacket {
    private final BlockPos trainerPos;

    public OpenMaidTrainingPacket(BlockPos trainerPos) {
        this.trainerPos = trainerPos;
    }

    public static void encode(OpenMaidTrainingPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.trainerPos);
    }

    public static OpenMaidTrainingPacket decode(FriendlyByteBuf buf) {
        return new OpenMaidTrainingPacket(buf.readBlockPos());
    }

    public static void handle(OpenMaidTrainingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (containerId, inv, p) -> new MaidTrainingMenu(containerId, inv, msg.trainerPos),
                        Component.translatable("gui.touhou_little_maid_vampirism.maid_training")
                ), buf -> buf.writeBlockPos(msg.trainerPos));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
