package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.meal.VampireMaidFoodFilter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class AutoFeedHandler {

    private static final int MOVE_TIMEOUT = 200; // 10秒移动超时
    private static final double EXTRACT_DISTANCE = 2.5; // 汲取距离
    private static long soundStartTime = -1;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;
        if (!(living instanceof EntityMaid maid)) return;

        // 1秒(20 ticks)后停止吸血音效
        if (living.level() instanceof ServerLevel serverLevel && soundStartTime > 0) {
            long gameTime = serverLevel.getGameTime();
            if (gameTime - soundStartTime >= 20) {
                stopFeedingSound(serverLevel);
                soundStartTime = -1;
            }
        }

        ModCapabilities.getVampireMaid(maid).ifPresent(cap -> {
            if (!cap.isVampire()) return;
            if (!BloodConfig.AUTO_FEED_ENABLED.get()) return;

            // 女仆坐下时停止自动觅血
            if (maid.isMaidInSittingPose()) {
                if (cap.getAutoFeedState() != 0) {
                    cap.resetAutoFeedState();
                    maid.getNavigation().stop();
                }
                return;
            }

            // 如果状态不是 EXTRACTING 但音效正在播放，立即停止
            if (cap.getAutoFeedState() != 2 && soundStartTime > 0) {
                if (living.level() instanceof ServerLevel serverLevel) {
                    stopFeedingSound(serverLevel);
                }
                soundStartTime = -1;
            }

            VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> {
                int currentBlood = ext.getBlood();
                int maxBlood = ext.getMaxBlood();

                // 血量已满，重置状态
                if (currentBlood >= maxBlood) {
                    if (cap.getAutoFeedState() != 0) {
                        cap.resetAutoFeedState();
                        maid.getNavigation().stop();
                    }
                    return;
                }

                // 背包有血液食物时，不需要自动觅血（让 VampireMaidTickHandler 处理进食）
                if (hasBloodFoodInInventory(maid)) {
                    if (cap.getAutoFeedState() != 0) {
                        cap.resetAutoFeedState();
                        maid.getNavigation().stop();
                    }
                    cap.setAutoFeedTimer(0);
                    return;
                }

                int state = cap.getAutoFeedState();

                switch (state) {
                    case 0 -> handleIdleState(maid, cap, ext);
                    case 1 -> handleMovingState(maid, cap, ext);
                    case 2 -> handleExtractingState(maid, cap, ext);
                    default -> cap.resetAutoFeedState();
                }
            });
        });
    }

    // === IDLE 状态：等待计时器到达后搜索目标 ===
    private static void handleIdleState(EntityMaid maid, VampireMaidCapability cap, Object ext) {
        int timer = cap.getAutoFeedTimer() + 1;
        if (timer < BloodConfig.AUTO_FEED_INTERVAL.get()) {
            cap.setAutoFeedTimer(timer);
            return;
        }
        cap.setAutoFeedTimer(0);

        // 搜索最近的有效目标
        int range = BloodConfig.AUTO_FEED_RANGE.get();
        AABB searchBox = maid.getBoundingBox().inflate(range);
        var target = maid.level().getEntitiesOfClass(PathfinderMob.class, searchBox,
                        mob -> isValidAutoFeedTarget(maid, mob))
                .stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr))
                .orElse(null);

        if (target != null) {
            cap.setAutoFeedTargetUUID(target.getUUID());
            cap.setAutoFeedState(1); // 切换到 MOVING
            cap.setAutoFeedMoveTimer(0);
            maid.getNavigation().moveTo(target, 0.5);
            TLMVMain.LOGGER.debug("[AutoFeedHandler] Found auto-feed target: {}, moving to it", target.getName().getString());
        }
    }

    // === MOVING 状态：导航到目标 ===
    private static void handleMovingState(EntityMaid maid, VampireMaidCapability cap, Object ext) {
        UUID targetUUID = cap.getAutoFeedTargetUUID();
        if (targetUUID == null) {
            cap.resetAutoFeedState();
            return;
        }

        // 查找目标实体
        LivingEntity target = findEntityByUUID(maid, targetUUID);
        if (target == null || !isValidAutoFeedTarget(maid, target)) {
            cap.resetAutoFeedState();
            maid.getNavigation().stop();
            return;
        }

        // 检查移动超时
        int moveTimer = cap.getAutoFeedMoveTimer() + 1;
        cap.setAutoFeedMoveTimer(moveTimer);
        if (moveTimer > MOVE_TIMEOUT) {
            TLMVMain.LOGGER.debug("[AutoFeedHandler] Move timeout, giving up target");
            cap.resetAutoFeedState();
            maid.getNavigation().stop();
            return;
        }

        double distance = maid.distanceTo(target);
        if (distance <= EXTRACT_DISTANCE) {
            // 到达目标，切换到汲取状态
            maid.getNavigation().stop();
            cap.setAutoFeedState(2); // EXTRACTING
        } else if (!maid.getNavigation().isInProgress()) {
            // 导航中断，重新导航
            maid.getNavigation().moveTo(target, 0.5);
        }
    }

    // === EXTRACTING 状态：汲取血液并直接恢复自身血量 ===
    private static void handleExtractingState(EntityMaid maid, VampireMaidCapability cap, Object extObj) {
        UUID targetUUID = cap.getAutoFeedTargetUUID();
        if (targetUUID == null) {
            cap.resetAutoFeedState();
            return;
        }

        LivingEntity target = findEntityByUUID(maid, targetUUID);
        if (target == null || !target.isAlive() || maid.distanceTo(target) > EXTRACT_DISTANCE + 1) {
            cap.resetAutoFeedState();
            return;
        }

        if (!(target instanceof PathfinderMob pathfinderTarget)) {
            cap.resetAutoFeedState();
            return;
        }

        VampirismAPI.getExtendedCreatureVampirism(pathfinderTarget).ifPresent(targetExt -> {
            int targetBlood = targetExt.getBlood();
            if (targetBlood <= 1) {
                cap.resetAutoFeedState();
                return;
            }

            // 计算提取量
            int extractAmount = BloodConfig.AUTO_FEED_EXTRACT_AMOUNT.get();
            int actualExtract = Math.min(extractAmount, targetBlood - 1);

            // 从目标扣除血量
            targetExt.setBlood(targetBlood - actualExtract);
            syncBlood(targetExt);

            // 恢复女仆自身血量
            VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(maidExt -> {
                int currentBlood = maidExt.getBlood();
                int maxBlood = maidExt.getMaxBlood();
                int newBlood = Math.min(currentBlood + actualExtract, maxBlood);
                maidExt.setBlood(newBlood);

                // 协调反自动恢复系统
                cap.setLastKnownBlood(newBlood);

                syncBlood(maidExt);

                TLMVMain.LOGGER.debug("[AutoFeedHandler] Auto-feed success: +{} blood => {}/{}",
                        actualExtract, newBlood, maxBlood);
            });

            // 吸血音效
            ResourceLocation soundId = new ResourceLocation("vampirism", "entity.vampire_feeding");
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
            if (sound != null) {
                maid.level().playSound(null, maid.blockPosition(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
                soundStartTime = maid.level().getGameTime();
            }

            // 爱心粒子
            maid.spawnHeartParticle();
        });

        // 汲取完毕，回到 IDLE
        cap.resetAutoFeedState();
    }

    // === 辅助方法 ===

    private static boolean hasBloodFoodInInventory(EntityMaid maid) {
        IItemHandler inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && VampireMaidFoodFilter.isBloodFood(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidAutoFeedTarget(EntityMaid maid, LivingEntity target) {
        if (target == maid) return false;
        if (!target.isAlive()) return false;
        // 排除其他吸血鬼女仆
        if (target instanceof EntityMaid otherMaid) {
            boolean isVampire = ModCapabilities.getVampireMaid(otherMaid)
                    .map(VampireMaidCapability::isVampire)
                    .orElse(false);
            if (isVampire) return false;
        }
        if (!(target instanceof PathfinderMob pathfinderTarget)) return false;
        var ext = VampirismAPI.getExtendedCreatureVampirism(pathfinderTarget).orElse(null);
        return ext != null && ext.getBlood() > 1;
    }

    private static LivingEntity findEntityByUUID(EntityMaid maid, UUID uuid) {
        int range = BloodConfig.AUTO_FEED_RANGE.get() + 5; // 搜索范围略大于配置
        AABB searchBox = maid.getBoundingBox().inflate(range);
        return maid.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e.getUUID().equals(uuid))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static void syncBlood(Object ext) {
        try {
            ext.getClass().getMethod("sync").invoke(ext);
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("Failed to sync blood value", e);
        }
    }

    private static void stopFeedingSound(ServerLevel level) {
        ResourceLocation soundId = new ResourceLocation("vampirism", "entity.vampire_feeding");
        if (level.getServer() != null) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.NEUTRAL));
            }
        }
    }
}
