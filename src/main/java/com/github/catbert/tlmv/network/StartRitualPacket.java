package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.blockentity.MaidAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class StartRitualPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartRitualPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "start_ritual"));

    public static final StreamCodec<FriendlyByteBuf, StartRitualPacket> STREAM_CODEC =
            StreamCodec.ofMember(StartRitualPacket::write, StartRitualPacket::new);

    private final BlockPos pos;

    public StartRitualPacket(BlockPos pos) {
        this.pos = pos;
    }

    public StartRitualPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartRitualPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            Level level = player.level();
            if (level.getBlockEntity(packet.pos) instanceof MaidAltarBlockEntity altar) {
                MaidAltarBlockEntity.Result result = altar.checkCanActivate();
                if (result == MaidAltarBlockEntity.Result.OK) {
                    altar.startRitual();
                } else {
                    player.sendSystemMessage(getResultMessage(result));
                }
                player.closeContainer();
            }
        });
    }

    private static Component getResultMessage(MaidAltarBlockEntity.Result result) {
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
