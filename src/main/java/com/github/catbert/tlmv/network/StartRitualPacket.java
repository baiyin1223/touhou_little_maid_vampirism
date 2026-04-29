package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.blockentity.MaidAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartRitualPacket {
    private final BlockPos pos;

    public StartRitualPacket(BlockPos pos) {
        this.pos = pos;
    }

    public StartRitualPacket(net.minecraft.network.FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.getBlockEntity(pos) instanceof MaidAltarBlockEntity altar) {
                MaidAltarBlockEntity.Result result = altar.checkCanActivate();
                if (result == MaidAltarBlockEntity.Result.OK) {
                    altar.startRitual();
                } else {
                    player.sendSystemMessage(getResultMessage(result));
                }
                player.closeContainer();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private Component getResultMessage(MaidAltarBlockEntity.Result result) {
        MutableComponent message = switch (result) {
            case NO_MAID -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.no_maid");
            case WRONG_LEVEL -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.wrong_level");
            case NIGHT_ONLY -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.night_only");
            case STRUCTURE_WRONG -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.structure_wrong");
            case ITEMS_MISSING -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.items_missing");
            case IS_RUNNING -> Component.translatable("gui.touhou_little_maid_vampirism.ritual.is_running");
            default -> Component.literal("Unknown error");
        };
        return message;
    }
}
