package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.capability.ModCapabilities;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class ClearMaidVampirePacket {
    private final BlockPos altarPos;

    public ClearMaidVampirePacket(BlockPos pos) {
        this.altarPos = pos;
    }

    public ClearMaidVampirePacket(FriendlyByteBuf buf) {
        this.altarPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(altarPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Faction check (anti-cheat: client-side check can be bypassed)
            boolean isVampirePlayer = FactionPlayerHandler.getOpt(player)
                    .map(handler -> handler.isInFaction(VReference.VAMPIRE_FACTION))
                    .orElse(false);
            if (!isVampirePlayer) return;

            Level level = player.level();

            // Distance check (anti-cheat)
            if (player.blockPosition().distSqr(altarPos) > 100) return; // 10 blocks

            // Verify block is altar_cleansing
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(level.getBlockState(altarPos).getBlock());
            if (blockId == null || !"vampirism".equals(blockId.getNamespace()) || !"altar_cleansing".equals(blockId.getPath())) {
                return;
            }

            // Search for maid entities in 5 block radius
            AABB searchBox = new AABB(altarPos).inflate(5);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return entityId != null && "touhou_little_maid".equals(entityId.getNamespace());
            });

            int[] clearedCount = {0};
            for (LivingEntity entity : entities) {
                // Check ownership
                if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                    if (!tamable.isOwnedBy(player)) continue;
                } else {
                    continue;
                }

                // Check and clear vampire status
                var cap = ModCapabilities.getVampireMaid(entity).orElse(null);
                if (cap == null || !cap.isVampire()) continue;

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
                if (entity instanceof net.minecraft.world.entity.PathfinderMob pathfinderMob) {
                    VampirismAPI.getExtendedCreatureVampirism(pathfinderMob).ifPresent(ext -> {
                        ext.setBlood(0);
                        try {
                            ext.getClass().getMethod("sync").invoke(ext);
                        } catch (Exception ignored) {
                        }
                    });
                }

                // Sync capability to client
                TLMVNetwork.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                        new SyncVampireMaidPacket(entity.getId(), false, 0)
                );

                clearedCount[0]++;
            }

            // Send feedback
            if (clearedCount[0] > 0) {
                player.sendSystemMessage(Component.translatable(
                        "message.touhou_little_maid_vampirism.clear_maid_success", clearedCount[0]));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.touhou_little_maid_vampirism.clear_maid_not_found"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
