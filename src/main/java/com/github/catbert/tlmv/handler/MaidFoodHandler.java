package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.meal.VampireMaidFoodFilter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaidFoodHandler {

    private static final double NOTIFY_RADIUS_SQR = 100;

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        ItemStack item = event.getItem();
        TLMVMain.LOGGER.debug("[MaidFoodHandler] ItemUseStart: entity={}, item={}", entity.getName().getString(), item.getItem());

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            TLMVMain.LOGGER.debug("[MaidFoodHandler] Capability found, isVampire={}", cap.isVampire());
            if (cap.isVampire()) {
                // 只取消非血液食物，血液食物正常通过以触发 TLM 原版进食动画
                if (!VampireMaidFoodFilter.isBloodFood(item)) {
                    event.setCanceled(true);
                    TLMVMain.LOGGER.info("[MaidFoodHandler] Canceling non-blood food for vampire maid: {}", item.getItem());
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
                    notifyNearbyPlayers(entity);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (cap.isVampire()) {
                ItemStack item = event.getItem();
                // 只取消非血液食物的 tick，血液食物正常通过
                if (!VampireMaidFoodFilter.isBloodFood(item)) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        ItemStack stack = event.getItem();
        if (!VampireMaidFoodFilter.isBloodFood(stack)) {
            return;
        }

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (cap.isVampire() && entity instanceof PathfinderMob mob) {
                VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
                    int restoreAmount = getRestoreAmount(stack);

                    // 在 resultStack 上处理物品消耗，确保最终 setItemInHand 正确
                    ItemStack result = event.getResultStack();
                    if (!result.isEmpty()) {
                        if (isBloodBottle(result)) {
                            int currentDamage = result.getDamageValue();
                            int bloodInBottle = currentDamage; // damage 直接就是血量
                            int bloodToConsume = Math.min(3, bloodInBottle);

                            // 也要考虑女仆还需要多少血
                            int bloodNeeded = ext.getMaxBlood() - ext.getBlood();
                            bloodToConsume = Math.min(bloodToConsume, bloodNeeded);

                            int remainingBlood = bloodInBottle - bloodToConsume;

                            if (remainingBlood <= 0) {
                                result = new ItemStack(Items.GLASS_BOTTLE);
                            } else {
                                int newDamage = remainingBlood;
                                result.setDamageValue(newDamage);
                            }
                            restoreAmount = bloodToConsume;
                            TLMVMain.LOGGER.debug("[MaidFoodHandler] Blood bottle consumed: amount={}, damage={}/{}",
                                    bloodToConsume, result.getDamageValue(), result.getMaxDamage());
                        } else {
                            result.shrink(1);
                        }
                        event.setResultStack(result);
                    }

                    int newBlood = Math.min(ext.getBlood() + restoreAmount, ext.getMaxBlood());
                    ext.setBlood(newBlood);

                    // 同步 lastKnownBlood 防止反自动恢复系统回滚喂食效果
                    ModCapabilities.getVampireMaid(entity).ifPresent(feedCap -> {
                        feedCap.setLastKnownBlood(newBlood);
                    });

                    try {
                        ext.getClass().getMethod("sync").invoke(ext);
                    } catch (Exception e) {
                        TLMVMain.LOGGER.warn("Failed to sync blood value", e);
                    }
                    TLMVMain.LOGGER.debug("[MaidFoodHandler] Vampire maid blood restored: +{} => {}/{}",
                            restoreAmount, newBlood, ext.getMaxBlood());
                });

                entity.stopUsingItem();

                // 添加爱心粒子和好感度提升
                if (entity instanceof EntityMaid maid) {
                    maid.spawnHeartParticle();
                    maid.setFavorability(maid.getFavorability() + 1);
                }

                // 移除原版机制可能添加的负面效果
                entity.removeEffect(MobEffects.CONFUSION);
                entity.removeEffect(MobEffects.POISON);
                entity.removeEffect(MobEffects.HUNGER);
                TLMVMain.LOGGER.debug("[MaidFoodHandler] Cleared negative effects after blood food: {}", stack.getItem());
            }
        });
    }

    private static boolean isBloodBottle(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "vampirism:blood_bottle".equals(key.toString());
    }

    private static int getRestoreAmount(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return 2;

        String name = key.toString();

        if (name.contains("heart")) {
            return 20;
        }

        if (name.contains("pure_blood")) {
            return 10;
        }

        return 4;
    }

    private static void notifyNearbyPlayers(LivingEntity entity) {
        for (Player player : entity.level().players()) {
            if (player instanceof ServerPlayer && player.distanceToSqr(entity) < NOTIFY_RADIUS_SQR) {
                player.sendSystemMessage(
                        Component.translatable("message.touhou_little_maid_vampirism.vampire_maid_reject_food"));
            }
        }
    }
}
