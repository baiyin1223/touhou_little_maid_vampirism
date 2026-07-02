package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.level.HunterLevelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RevertHunterMaidPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RevertHunterMaidPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "revert_hunter_maid"));

    public static final StreamCodec<FriendlyByteBuf, RevertHunterMaidPacket> STREAM_CODEC =
            StreamCodec.ofMember(RevertHunterMaidPacket::write, RevertHunterMaidPacket::new);

    private final int maidId;

    public RevertHunterMaidPacket(int maidId) {
        this.maidId = maidId;
    }

    public RevertHunterMaidPacket(FriendlyByteBuf buf) {
        this.maidId = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(maidId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RevertHunterMaidPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Entity entity = player.level().getEntity(msg.maidId);
            if (!(entity instanceof LivingEntity target)) return;

            // Consume injection_sanguinare from main hand (creative exempt)
            if (!player.isCreative()) {
                ItemStack held = player.getMainHandItem();
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(held.getItem());
                if (key != null && "vampirism:injection_sanguinare".equals(key.toString())) {
                    held.shrink(1);
                }
            }

            var cap = target.getData(ModAttachments.VAMPIRE_MAID.get());
            if (!cap.isHunter()) return;

            // Clear hunter data
            cap.setHunter(false);
            cap.setHunterLevel(0);
            if (target instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid) {
                HunterLevelManager.applyLevelAttributes(maid, 0);
                maid.removeAllEffects();
            }

            // Kill maid (player revives with film)
            if (target instanceof LivingEntity living) {
                living.setHealth(0);
            }

            // Sync
            PacketDistributor.sendToPlayersTrackingEntity(target,
                    new SyncVampireMaidPacket(target.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel()));

            player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.revert_hunter_done"));
        });
    }
}
