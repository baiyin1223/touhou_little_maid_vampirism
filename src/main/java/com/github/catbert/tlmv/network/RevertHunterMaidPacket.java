package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.level.HunterLevelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RevertHunterMaidPacket {
    private final int maidId;

    public RevertHunterMaidPacket(int maidId) {
        this.maidId = maidId;
    }

    public static void encode(RevertHunterMaidPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maidId);
    }

    public static RevertHunterMaidPacket decode(FriendlyByteBuf buf) {
        return new RevertHunterMaidPacket(buf.readInt());
    }

    public static void handle(RevertHunterMaidPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(msg.maidId);
            if (!(entity instanceof LivingEntity target)) return;

            // Consume injection_sanguinare from main hand (creative exempt)
            if (!player.isCreative()) {
                ItemStack held = player.getMainHandItem();
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
                if (key != null && "vampirism:injection_sanguinare".equals(key.toString())) {
                    held.shrink(1);
                }
            }

            var cap = ModCapabilities.getVampireMaid(target).orElse(null);
            if (cap == null || !cap.isHunter()) return;

            // Clear hunter data
            cap.setHunter(false);
            cap.setHunterLevel(0);
            cap.setPoisonousBlood(false);
            if (target instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid) {
                HunterLevelManager.applyLevelAttributes(maid, 0);
                maid.removeAllEffects();
            }

            // Kill maid (player revives with film)
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).setHealth(0);
            }

            // Sync
            TLMVNetwork.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> target),
                    new SyncVampireMaidPacket(target.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel())
            );

            player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.revert_hunter_done"));
        });
        ctx.get().setPacketHandled(true);
    }
}
