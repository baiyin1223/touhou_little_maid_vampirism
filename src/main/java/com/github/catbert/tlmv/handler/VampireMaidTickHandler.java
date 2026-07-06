package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.level.HunterLevelManager;
import com.github.catbert.tlmv.level.VampireLevelManager;
import com.github.catbert.tlmv.meal.VampireMaidFoodFilter;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.blockentity.SunscreenBeaconBlockEntity;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class VampireMaidTickHandler {

    // 记录正在由我们系统触发进食的女仆 UUID
    private static final Set<UUID> FEEDING_MAIDS = new HashSet<>();

    // VNU (VampiresNeedUmbrellas) 可选联动：零编译依赖，通过 Registry Name 检测
    private static final boolean VNU_LOADED = ModList.get().isLoaded("vampiresneedumbrellas");

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof EntityMaid maid)) return;

        VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());

        // 实体加入世界时（含胶片复活），重置 lastApplied 以触发 tick 中的重新应用
        cap.setLastAppliedVampireLevel(0);
        cap.setLastAppliedHunterLevel(0);
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) {
            return;
        }

        VampireMaidCapability cap = living.getData(ModAttachments.VAMPIRE_MAID.get());
        cap.tick(living);

        if (cap.isVampire() && living instanceof PathfinderMob mob) {
            // 进食后武器恢复补偿：检测进食结束后武器是否错误地到了副手
            if (mob instanceof EntityMaid maid2) {
                if (!maid2.isAlive()) {
                    FEEDING_MAIDS.remove(maid2.getUUID());
                } else if (FEEDING_MAIDS.contains(maid2.getUUID()) && !maid2.isUsingItem()) {
                    FEEDING_MAIDS.remove(maid2.getUUID());
                    ItemStack offHand = maid2.getOffhandItem();
                    ItemStack mainHand = maid2.getMainHandItem();
                    if (!offHand.isEmpty() && mainHand.isEmpty()) {
                        maid2.setItemInHand(InteractionHand.MAIN_HAND, offHand.copy());
                        maid2.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                        TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Recovered weapon from offhand to mainhand after feeding");
                    }
                }
            }

            // 等级变化检测：当前等级与上次已应用等级不同时，重新应用属性修正
            if (mob instanceof EntityMaid maid) {
                int currentLevel = cap.getVampireLevel();
                if (currentLevel != cap.getLastAppliedVampireLevel()) {
                    if (currentLevel > 0) {
                        VampireLevelManager.applyLevelAttributes(maid, currentLevel);
                    } else {
                        VampireLevelManager.removeLevelModifiers(maid);
                    }
                    cap.setLastAppliedVampireLevel(currentLevel);
                }
                // 每30秒刷新等级buff（药水效果，不受AttributeModifier影响）
                if (mob.tickCount % 600 == 0 && currentLevel > 0) {
                    VampireLevelManager.applyLevelBuffs(maid, currentLevel);
                }
            }

            // Sunscreen Beacon 检测（每 80 ticks）和 VNU 伞联动（每 tick）
            if (mob instanceof EntityMaid maid && mob.level() instanceof ServerLevel serverLevel) {
                checkSunscreenBeacon(maid, serverLevel);
                checkUmbrellaProtection(maid, serverLevel, cap);
            }

            // 每3秒检查血值，触发进食
            if (mob.tickCount % 60 == 0) {
                VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
                    int blood = ext.getBlood();
                    int maxBlood = ext.getMaxBlood();
                    // Skip if blood is -1 (entity type not registered in Vampirism blood data map)
                    if (blood >= 0 && maxBlood > 0 && blood < maxBlood) {
                        triggerMaidFeeding(mob);
                    }
                });
            }

            // 每5秒同步吸血鬼状态到客户端
            if (mob.tickCount % 100 == 0) {
                PacketDistributor.sendToPlayersTrackingEntity(mob,
                        new SyncVampireMaidPacket(mob.getId(), cap.isVampire(), cap.getVampireLevel())
                );
            }
        }

        // Hunter maid handling
        if (cap.isHunter() && living instanceof EntityMaid maid) {
            // 等级变化检测：当前等级与上次已应用等级不同时，重新应用属性修正
            int hunterLevel = cap.getHunterLevel();
            if (hunterLevel != cap.getLastAppliedHunterLevel()) {
                if (hunterLevel > 0) {
                    HunterLevelManager.applyLevelAttributes(maid, hunterLevel);
                } else {
                    HunterLevelManager.removeLevelModifiers(maid);
                }
                cap.setLastAppliedHunterLevel(hunterLevel);
            }
            // 每30秒刷新等级buff（药水效果）
            if (maid.tickCount % 600 == 0 && hunterLevel > 0) {
                HunterLevelManager.applyLevelBuffs(maid, hunterLevel);
            }
            // 每5秒同步猎人状态到客户端
            if (maid.tickCount % 100 == 0) {
                PacketDistributor.sendToPlayersTrackingEntity(maid,
                        new SyncVampireMaidPacket(maid.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel())
                );
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof LivingEntity living) {
            VampireMaidCapability trackCap = living.getData(ModAttachments.VAMPIRE_MAID.get());
            PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(),
                    new SyncVampireMaidPacket(target.getId(), trackCap.isVampire(), trackCap.getVampireLevel(), trackCap.isHunter(), trackCap.getHunterLevel())
            );
        }
    }

    private static void triggerMaidFeeding(PathfinderMob mob) {
        if (!(mob instanceof EntityMaid maid)) {
            return;
        }

        final int[] currentBlood = {-1};
        final int[] maxBlood = {-1};
        VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
            currentBlood[0] = ext.getBlood();
            maxBlood[0] = ext.getMaxBlood();
        });
        // Skip if blood is -1 (entity type not registered in Vampirism blood data map)
        if (currentBlood[0] < 0 || maxBlood[0] <= 0) {
            return;
        }
        boolean usingItem = maid.isUsingItem();
        if (usingItem) {
            if (maid.getTicksUsingItem() > 200) {
                TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Maid stuck in using state for too long, force stopping");
                maid.stopUsingItem();
            } else {
                TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Maid is already using item, skipping feeding trigger");
                return;
            }
        }
        IItemHandler inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            boolean isBloodFood = VampireMaidFoodFilter.isBloodFood(stack);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (isBloodFood) {
                boolean isBottle = isBloodBottle(stack);
                int extractCount = isBottle ? 1 : stack.getCount();
                ItemStack extracted = inv.extractItem(i, extractCount, false);
                if (!extracted.isEmpty()) {
                    if (isBloodBottle(extracted)) {
                        // 血瓶直接消耗，不走 startUsingItem（血瓶的使用动画不可靠）
                        handleBloodBottleDirectly(maid, extracted, inv);
                        return;
                    }
                    // 非血瓶的血液食物走正常 startUsingItem 路径
                    // 手部选择：优先空主手 → 否则副手（避免替换主手武器导致攻击目标丢失）
                    InteractionHand feedHand = maid.getMainHandItem().isEmpty()
                            ? InteractionHand.MAIN_HAND
                            : InteractionHand.OFF_HAND;
                    ItemStack handItem = maid.getItemInHand(feedHand).copy();
                    maid.setItemInHand(feedHand, extracted);
                    maid.memoryHandItemStack(handItem);
                    maid.startUsingItem(feedHand);
                    FEEDING_MAIDS.add(maid.getUUID());
                    TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Triggered feeding: hand={}, item={}", feedHand, extracted);
                    return;
                }
            }
        }
    }

    private static boolean isBloodBottle(ItemStack stack) {
        return VampirismHelper.isBloodBottle(stack);
    }

    private static void handleBloodBottleDirectly(EntityMaid maid, ItemStack bottle, IItemHandler inv) {
        // 播放手臂动画和饮用音效
        maid.swing(InteractionHand.MAIN_HAND);
        maid.level().playSound(null, maid.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F);

        VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> {
            int bloodInBottle = VampirismHelper.getBloodAmount(bottle);
            int bloodToConsume = Math.min(3, bloodInBottle);

            int bloodNeeded = ext.getMaxBlood() - ext.getBlood();
            bloodToConsume = Math.min(bloodToConsume, bloodNeeded);

            if (bloodToConsume > 0) {
                int newBlood = Math.min(ext.getBlood() + bloodToConsume, ext.getMaxBlood());
                ext.setBlood(newBlood);

                // 同步 lastKnownBlood 防止反自动恢复系统回滚喂食效果
                VampireMaidCapability feedCap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
                feedCap.setLastKnownBlood(newBlood);

                int remainingBlood = bloodInBottle - bloodToConsume;
                if (remainingBlood <= 0) {
                    // 血瓶消耗完毕，返回玻璃瓶
                    ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
                    for (int j = 0; j < inv.getSlots(); j++) {
                        glassBottle = inv.insertItem(j, glassBottle, false);
                        if (glassBottle.isEmpty()) break;
                    }
                    if (!glassBottle.isEmpty()) {
                        maid.spawnAtLocation(glassBottle);
                    }
                    TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Blood bottle fully consumed, returned glass bottle");
                } else {
                    VampirismHelper.setBloodAmount(bottle, remainingBlood);
                    // 将剩余血瓶放回背包
                    ItemStack remainder = bottle;
                    for (int j = 0; j < inv.getSlots(); j++) {
                        remainder = inv.insertItem(j, remainder, false);
                        if (remainder.isEmpty()) break;
                    }
                }

                // sync 反射
                try {
                    ext.getClass().getMethod("sync").invoke(ext);
                } catch (Exception e) {
                    TLMVMain.LOGGER.warn("Failed to sync blood value", e);
                }

                // 添加爱心粒子和好感度提升
                maid.spawnHeartParticle();
                maid.setFavorability(maid.getFavorability() + 1);

                TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Vampire maid blood restored via direct consume: +{} => {}/{}",
                        bloodToConsume, newBlood, ext.getMaxBlood());
            }
        });

        // 移除女仆身上的负面效果
        maid.removeEffect(MobEffects.CONFUSION);
        maid.removeEffect(MobEffects.POISON);
        maid.removeEffect(MobEffects.HUNGER);
    }

    // ========== Sunscreen Beacon 检测 ==========

    /**
     * 检测附近的 Vampirism Sunscreen Beacon，为吸血鬼女仆施加防晒效果。
     * 逻辑与 Vampirism 的 SunscreenBeaconBlockEntity.serverTick 保持一致：
     * 每 80 ticks 检查一次，XZ 平面距离，Y 轴无限制。
     */
    private static void checkSunscreenBeacon(EntityMaid maid, ServerLevel level) {
        if (level.getGameTime() % 80L != 0L) return;

        int dist = VampirismConfig.SERVER.sunscreenBeaconDistance.get();
        int distSq = dist * dist;

        BlockPos maidPosXZ = new BlockPos((int) maid.getX(), 0, (int) maid.getZ());

        int chunkRadius = (dist >> 4) + 1;
        int maidChunkX = maid.blockPosition().getX() >> 4;
        int maidChunkZ = maid.blockPosition().getZ() >> 4;

        for (int cx = maidChunkX - chunkRadius; cx <= maidChunkX + chunkRadius; cx++) {
            for (int cz = maidChunkZ - chunkRadius; cz <= maidChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof SunscreenBeaconBlockEntity) {
                        BlockPos beaconPosXZ = new BlockPos(entry.getKey().getX(), 0, entry.getKey().getZ());
                        if (maidPosXZ.distSqr(beaconPosXZ) < distSq) {
                            // 参数与 Vampirism beacon 一致：160 ticks, level 5, ambient, 无粒子
                            maid.addEffect(new MobEffectInstance(
                                    ModEffects.SUNSCREEN, 160, 5, true, false
                            ));
                            return;
                        }
                    }
                }
            }
        }
    }

    // ========== VNU 伞联动（零编译依赖） ==========

    /**
     * 通过 Registry Name 检测是否为 VNU 伞物品，不引用任何 VNU 类。
     * 匹配：namespace=vampiresneedumbrellas, path 包含 umbrella 且不包含 rod
     */
    private static boolean isUmbrellaItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "vampiresneedumbrellas".equals(id.getNamespace())
                && id.getPath().contains("umbrella")
                && !id.getPath().contains("rod");
    }

    /**
     * 检测女仆手持或背包中的伞，施加防晒效果并在阳光下消耗耐久。
     * 搜索顺序：主手 → 副手 → 背包
     * 效果参数与 VNU 的 SunscreenEffectInstance 一致：21 ticks, level 5, 隐藏
     */
    private static void checkUmbrellaProtection(EntityMaid maid, ServerLevel level, VampireMaidCapability cap) {
        if (!VNU_LOADED) return;

        ItemStack umbrella = ItemStack.EMPTY;

        if (isUmbrellaItem(maid.getMainHandItem())) {
            umbrella = maid.getMainHandItem();
        } else if (isUmbrellaItem(maid.getOffhandItem())) {
            umbrella = maid.getOffhandItem();
        } else {
            // 搜索女仆背包（使用 getAvailableInv，根据背包类型限制范围）
            IItemHandler inv = maid.getAvailableInv(false);
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (isUmbrellaItem(stack)) {
                    umbrella = stack;
                    break;
                }
            }
        }

        if (umbrella.isEmpty()) return;

        // 施加 sunscreen 效果（21 ticks, level 5, 非 ambient, 隐藏粒子和图标）
        maid.addEffect(new MobEffectInstance(ModEffects.SUNSCREEN, 21, 5, false, false));

        // 阳光下消耗伞的耐久（等级 5 自然免疫，无需消耗；创造伞无限耐久，跳过）
        if (isMaidInSunlight(maid) && cap.getVampireLevel() < 5 && umbrella.isDamageableItem()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(umbrella.getItem());
            if (!id.getPath().contains("creative")) {
                umbrella.hurtAndBreak(1, level, maid, (item) -> {});
            }
        }
    }

    /**
     * 判断女仆是否处于阳光直射下（与 SunDamageHandler 逻辑一致）
     */
    private static boolean isMaidInSunlight(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        if (!level.isDay()) return false;
        if (level.isRaining() && level.canSeeSky(pos)) return false;
        if (!level.canSeeSky(pos.above())) return false;
        // 检查 Vampirism 群系/维度配置（vampire_forest 等群系免疫阳光伤害）
        if (!VampirismAPI.sundamageRegistry().hasSunDamage(level, pos)) return false;
        return true;
    }
}
