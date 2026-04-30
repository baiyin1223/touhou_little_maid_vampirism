package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class ClearMaidVampirePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearMaidVampirePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "clear_maid_vampire"));

    public static final StreamCodec<FriendlyByteBuf, ClearMaidVampirePacket> STREAM_CODEC =
            StreamCodec.ofMember(ClearMaidVampirePacket::write, ClearMaidVampirePacket::new);

    private final BlockPos altarPos;

    public ClearMaidVampirePacket(BlockPos pos) {
        this.altarPos = pos;
    }

    public ClearMaidVampirePacket(FriendlyByteBuf buf) {
        this.altarPos = buf.readBlockPos();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(altarPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearMaidVampirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Faction check
            FactionPlayerHandler fph = FactionPlayerHandler.get(player);
            if (!fph.isInFaction(VReference.VAMPIRE_FACTION)) return;

            Level level = player.level();

            // Distance check (anti-cheat)
            if (player.blockPosition().distSqr(packet.altarPos) > 100) return; // 10 blocks

            // Verify block is altar_cleansing
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(packet.altarPos).getBlock());
            if (!"vampirism".equals(blockId.getNamespace()) || !"altar_cleansing".equals(blockId.getPath())) {
                return;
            }

            // Search for maid entities in 5 block radius
            AABB searchBox = new AABB(packet.altarPos).inflate(5);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                return "touhou_little_maid".equals(entityId.getNamespace());
            });

            int clearedCount = 0;
            for (LivingEntity entity : entities) {
                // Check ownership
                if (entity instanceof TamableAnimal tamable) {
                    if (!tamable.isOwnedBy(player)) continue;
                } else {
                    continue;
                }

                // Check and clear vampire status
                VampireMaidCapability cap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
                if (!cap.isVampire()) continue;

                // Reset all vampire data
                cap.setVampire(false);
                cap.setHadSanguinare(false);
                cap.setVampireLevel(0);
                cap.setLastKnownBlood(0);
                cap.setBloodDecayTimer(0);
                cap.setSlowDecayTimer(0);
                cap.setStarvationTimer(0);
                cap.setGarlicHpTicker(0);
                cap.setGarlicBloodTicker(0);
                cap.setApplyingSlowness(false);
                cap.resetAutoFeedState();

                // Reset Vampirism blood to 0
                if (entity instanceof PathfinderMob pathfinderMob) {
                    VampirismAPI.getExtendedCreatureVampirism(pathfinderMob).ifPresent(ext -> {
                        ext.setBlood(0);
                        try {
                            ext.getClass().getMethod("sync").invoke(ext);
                        } catch (Exception ignored) {
                        }
                    });
                }

                // Sync capability to client
                PacketDistributor.sendToPlayersTrackingEntity(entity,
                        new SyncVampireMaidPacket(entity.getId(), false, 0));

                clearedCount++;
            }

            // Send feedback
            if (clearedCount > 0) {
                player.sendSystemMessage(Component.translatable(
                        "message.touhou_little_maid_vampirism.clear_maid_success", clearedCount));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.touhou_little_maid_vampirism.clear_maid_not_found"));
            }
        });
    }
}
