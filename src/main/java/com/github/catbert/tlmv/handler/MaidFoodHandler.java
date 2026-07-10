package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.meal.ExternalBloodFoodRegistry;
import com.github.catbert.tlmv.meal.VampireMaidFoodFilter;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaidFoodHandler {

    // 正在进食外部血液食物注册表物品的吸血鬼女仆 UUID
    // 用于在 MobEffectEvent.Applicable 中拦截 VampiresDelight 等模组对非吸血鬼施加的负面效果
    private static final Set<UUID> EXTERNAL_FOOD_EATING = new HashSet<>();


    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        ItemStack item = event.getItem();
        TLMVMain.LOGGER.debug("[MaidFoodHandler] ItemUseStart: entity={}, item={}", entity.getName().getString(), item.getItem());

        VampireMaidCapability cap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        TLMVMain.LOGGER.debug("[MaidFoodHandler] Capability found, isVampire={}", cap.isVampire());
        if (cap.isVampire()) {
            // 只取消非血液食物，血液食物正常通过以触发 TLM 原版进食动画
            if (!VampireMaidFoodFilter.isBloodFood(item)) {
                event.setCanceled(true);
                TLMVMain.LOGGER.info("[MaidFoodHandler] Canceling non-blood food for vampire maid: {}", item.getItem());
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            } else {
                // 外部注册表食物：标记正在进食，用于拦截负面效果
                ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item.getItem());
                if (itemKey != null && ExternalBloodFoodRegistry.isBloodFood(itemKey)) {
                    EXTERNAL_FOOD_EATING.add(entity.getUUID());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        VampireMaidCapability tickCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        if (tickCap.isVampire()) {
            ItemStack item = event.getItem();
            // 只取消非血液食物的 tick，血液食物正常通过
            if (!VampireMaidFoodFilter.isBloodFood(item)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        ItemStack stack = event.getItem();
        if (!VampireMaidFoodFilter.isBloodFood(stack)) {
            return;
        }

        VampireMaidCapability finishCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        if (finishCap.isVampire() && entity instanceof PathfinderMob mob) {
            VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
                int restoreAmount = getRestoreAmount(stack);

                // 在 resultStack 上处理物品消耗，确保最终 setItemInHand 正确
                ItemStack result = event.getResultStack();
                if (!result.isEmpty()) {
                    if (isBloodBottle(result)) {
                        int bloodInBottle = VampirismHelper.getBloodAmount(result);
                        int bloodToConsume = Math.min(3, bloodInBottle);

                        // 也要考虑女仆还需要多少血
                        int bloodNeeded = ext.getMaxBlood() - ext.getBlood();
                        bloodToConsume = Math.min(bloodToConsume, bloodNeeded);

                        int remainingBlood = bloodInBottle - bloodToConsume;

                        if (remainingBlood <= 0) {
                            result = new ItemStack(Items.GLASS_BOTTLE);
                        } else {
                            VampirismHelper.setBloodAmount(result, remainingBlood);
                        }
                        restoreAmount = bloodToConsume;
                        TLMVMain.LOGGER.debug("[MaidFoodHandler] Blood bottle consumed: amount={}, remaining={}",
                                bloodToConsume, remainingBlood);
                    }
                    // 非血瓶物品：eat() 已 shrink(1)，无需额外消耗
                    event.setResultStack(result);
                }

                int newBlood = Math.min(ext.getBlood() + restoreAmount, ext.getMaxBlood());
                ext.setBlood(newBlood);

                // 同步 lastKnownBlood 防止反自动恢复系统回滚喂食效果
                VampireMaidCapability feedCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
                feedCap.setLastKnownBlood(newBlood);

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
                // 恢复 HP（模拟 TLM DefaultMaidHealSelfMeal 回血逻辑，血液食物无 FoodProperties 故需手动调用）
                if (!isBloodBottle(stack)) {
                    maid.heal(4.0F);
                }
            }

            // 移除原版机制可能添加的负面效果
            entity.removeEffect(MobEffects.CONFUSION);
            entity.removeEffect(MobEffects.POISON);
            entity.removeEffect(MobEffects.HUNGER);
            TLMVMain.LOGGER.debug("[MaidFoodHandler] Cleared negative effects after blood food: {}", stack.getItem());
        }

        // 清除外部食物进食标记
        EXTERNAL_FOOD_EATING.remove(entity.getUUID());
    }

    /**
     * 拦截 VampiresDelight 等外部模组对非吸血鬼消费者施加的负面食物效果。
     * 仅在吸血鬼女仆进食 {@link ExternalBloodFoodRegistry} 中注册的物品时生效，
     * 不会误拦截来自其他来源（如药水、指令）的相同效果。
     */
    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!EXTERNAL_FOOD_EATING.contains(entity.getUUID())) return;

        var effect = event.getEffectInstance().getEffect();
        // VampiresDelight 的 VDFoodValues 中定义的人类负面效果：
        // CONFUSION (所有 NASTY 变体), BLINDNESS (NASTY_BLINDNESS), DARKNESS (NASTY_DARKNESS),
        // HUNGER (NASTY_BLOOD_DOUGH), POISON (NASTY_POISON, ORCHID_TEA_HUMAN)
        if (effect == MobEffects.CONFUSION
                || effect == MobEffects.BLINDNESS
                || effect == MobEffects.DARKNESS
                || effect == MobEffects.HUNGER
                || effect == MobEffects.POISON) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            TLMVMain.LOGGER.debug("[MaidFoodHandler] Blocked external food negative effect for vampire maid");
        }
    }

    private static boolean isBloodBottle(ItemStack stack) {
        return VampirismHelper.isBloodBottle(stack);
    }

    private static int getRestoreAmount(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return 2;

        // 优先查询外部血液食物注册表（如 VampiresDelight 等第三方模组精确映射）
        int externalValue = ExternalBloodFoodRegistry.getBloodValue(key);
        if (externalValue >= 0) {
            return externalValue;
        }

        String name = key.toString();

        if (name.contains("heart")) {
            return 20;
        }

        if (name.contains("pure_blood")) {
            return 10;
        }

        return 4;
    }
}

