package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.level.VampireLevelManager;
import com.github.catbert.tlmv.meal.VampireMaidFoodFilter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class VampireMaidTickHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) {
            return;
        }

        ModCapabilities.getVampireMaid(living).ifPresent(cap -> {
            cap.tick(living);

            if (cap.isVampire()) {
                // 如果还没保存原始名称，保存它
                if (cap.getOriginalName() == null) {
                    Component currentName = living.getCustomName();
                    if (currentName != null) {
                        cap.setOriginalName(currentName.getString());
                    } else {
                        cap.setOriginalName(living.getName().getString());
                    }
                }

                // 更新显示名称（只在名称不匹配时才设置，避免每 tick 重复调用）
                String prefix = VampireMaidCapability.getVampireDisplayPrefix(cap.getVampireLevel());
                String expectedName = prefix + cap.getOriginalName();
                Component currentCustomName = living.getCustomName();
                if (currentCustomName == null || !currentCustomName.getString().equals(expectedName)) {
                    living.setCustomName(Component.literal(expectedName));
                }
            }

            if (cap.isVampire() && living instanceof PathfinderMob mob) {
                // 每30秒刷新等级buff，确保永久生效（应对死亡重生等情况）
                if (mob.tickCount % 600 == 0 && cap.getVampireLevel() > 0) {
                    if (mob instanceof EntityMaid maid) {
                        VampireLevelManager.applyLevelBuffs(maid, cap.getVampireLevel());
                    }
                }

                // 女仆首次检测到等级时（如世界加载后第一个 tick），也应用一次属性
                if (mob.tickCount % 600 == 1 && cap.getVampireLevel() > 0) {
                    if (mob instanceof EntityMaid maid) {
                        VampireLevelManager.applyLevelAttributes(maid, cap.getVampireLevel());
                    }
                }

                // 每3秒检查血值，触发进食
                if (mob.tickCount % 60 == 0) {
                    VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
                        if (ext.getBlood() < ext.getMaxBlood()) {
                            triggerMaidFeeding(mob);
                        }
                    });
                }
            }
        });
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
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
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
                    maid.setItemInHand(InteractionHand.MAIN_HAND, extracted);
                    maid.startUsingItem(InteractionHand.MAIN_HAND);
                    TLMVMain.LOGGER.debug("[VampireMaidTickHandler] Triggered feeding: {}", extracted);
                    return;
                }
            }
        }
    }

    private static boolean isBloodBottle(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "vampirism:blood_bottle".equals(key.toString());
    }

    private static void handleBloodBottleDirectly(EntityMaid maid, ItemStack bottle, IItemHandler inv) {
        // 播放手臂动画和饮用音效
        maid.swing(InteractionHand.MAIN_HAND);
        maid.level().playSound(null, maid.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F);

        VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> {
            int currentDamage = bottle.getDamageValue();
            int bloodInBottle = currentDamage; // damage 直接就是血量
            int bloodToConsume = Math.min(3, bloodInBottle);

            int bloodNeeded = ext.getMaxBlood() - ext.getBlood();
            bloodToConsume = Math.min(bloodToConsume, bloodNeeded);

            if (bloodToConsume > 0) {
                int newBlood = Math.min(ext.getBlood() + bloodToConsume, ext.getMaxBlood());
                ext.setBlood(newBlood);

                // 同步 lastKnownBlood 防止反自动恢复系统回滚喂食效果
                ModCapabilities.getVampireMaid(maid).ifPresent(feedCap -> {
                    feedCap.setLastKnownBlood(newBlood);
                });

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
                    int newDamage = remainingBlood;
                    bottle.setDamageValue(newDamage);
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
}
