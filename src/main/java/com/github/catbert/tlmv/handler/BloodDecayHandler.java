package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.init.ModDamageTypes;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class BloodDecayHandler {

    /**
     * 主 tick 逻辑：反自动恢复 + 血量衰减 + blood=0 HP伤害
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;
        if (!(living instanceof EntityMaid maid)) return;

        ModCapabilities.getVampireMaid(maid).ifPresent(cap -> {
            if (!cap.isVampire()) return;
            if (!BloodConfig.BLOOD_DECAY_ENABLED.get()) return;

            VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> {
                int currentBlood = ext.getBlood();
                int maxBlood = ext.getMaxBlood();

                // === 反自动恢复 ===
                int lastKnown = cap.getLastKnownBlood();
                if (lastKnown < 0) {
                    // 首次初始化
                    cap.setLastKnownBlood(currentBlood);
                    lastKnown = currentBlood;
                } else if (currentBlood > lastKnown) {
                    // Vampirism 自动恢复了，重置回去
                    ext.setBlood(lastKnown);
                    syncBlood(ext);
                    currentBlood = lastKnown;
                } else if (currentBlood < lastKnown) {
                    // 被外部降低了（例如被其他模组吸血），接受新值
                    cap.setLastKnownBlood(currentBlood);
                    lastKnown = currentBlood;
                }

                // === 血量衰减逻辑 ===
                if (currentBlood > 0) {
                    cap.setStarvationTimer(0); // 有血时重置饥饿计时器

                    if (currentBlood > 6) {
                        // 高血量：每 200 ticks (10s) 检测一次，1/6 概率减 1
                        int decayTimer = cap.getBloodDecayTimer() + 1;
                        if (decayTimer >= 200) {
                            decayTimer = 0;
                            if (maid.getRandom().nextInt(6) == 0) {
                                int newBlood = currentBlood - 1;
                                ext.setBlood(newBlood);
                                cap.setLastKnownBlood(newBlood);
                                syncBlood(ext);
                            }
                        }
                        cap.setBloodDecayTimer(decayTimer);
                        cap.setSlowDecayTimer(0); // 重置慢速计时器
                    } else {
                        // 低血量 (<=6)：根据等级决定衰减速度
                        int level = cap.getVampireLevel();
                        cap.setBloodDecayTimer(0); // 重置高速计时器

                        if (level >= 5) {
                            // 等级5：不再减少
                            cap.setSlowDecayTimer(0);
                        } else {
                            int interval = (level >= 3) ? 12000 : 6000; // 10分钟 vs 5分钟
                            int slowTimer = cap.getSlowDecayTimer() + 1;
                            if (slowTimer >= interval) {
                                slowTimer = 0;
                                int newBlood = currentBlood - 1;
                                ext.setBlood(newBlood);
                                cap.setLastKnownBlood(newBlood);
                                syncBlood(ext);
                            }
                            cap.setSlowDecayTimer(slowTimer);
                        }
                    }
                } else {
                    // === blood = 0：HP 伤害 ===
                    cap.setBloodDecayTimer(0);
                    cap.setSlowDecayTimer(0);

                    int starvTimer = cap.getStarvationTimer() + 1;
                    int hpInterval = BloodConfig.BLOOD_STARVATION_HP_INTERVAL.get();
                    if (starvTimer >= hpInterval) {
                        starvTimer = 0;
                        float damage = BloodConfig.BLOOD_STARVATION_HP_DAMAGE.get();
                        float currentHp = maid.getHealth();
                        if (currentHp - damage <= 0) {
                            // 致死一击：使用 hurt() 触发正确的死亡消息
                            DamageSource source = new DamageSource(
                                maid.level().registryAccess()
                                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                                    .getOrThrow(ModDamageTypes.BLOOD_STARVATION)
                            );
                            maid.hurt(source, Float.MAX_VALUE);
                        } else {
                            // 真实伤害：直接扣血，无视一切减伤
                            maid.setHealth(currentHp - damage);
                            // 触发受伤动画和音效
                            maid.level().broadcastEntityEvent(maid, (byte) 2);
                        }
                    }
                    cap.setStarvationTimer(starvTimer);
                }
            });
        });
    }

    /**
     * 拦截 Vampirism 对吸血鬼女仆施加的虚弱和缓慢效果
     */
    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof EntityMaid)) return;

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (cap.isVampire()) {
                var effect = event.getEffectInstance().getEffect();
                if (effect == MobEffects.WEAKNESS || effect == MobEffects.MOVEMENT_SLOWDOWN) {
                    event.setResult(Event.Result.DENY);
                }
            }
        });
    }

    /**
     * 拦截 Vampirism 在 blood=0 时对吸血鬼女仆造成的致死伤害
     * 只拦截非我方的伤害（我方的 blood_starvation 正常通过）
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof EntityMaid)) return;

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (!cap.isVampire()) return;

            DamageSource source = event.getSource();
            // 如果是我方的 blood_starvation 伤害，正常通过
            if (source.typeHolder().is(ModDamageTypes.BLOOD_STARVATION)) {
                return;
            }

            // 检查是否来自 Vampirism 的血量相关伤害
            // Vampirism 使用 "vampirism:blood_starvation" 或类似的 DamageType
            // 通用策略：当女仆 blood=0 时，拦截来自 Vampirism 的伤害
            VampirismAPI.getExtendedCreatureVampirism((PathfinderMob) entity).ifPresent(ext -> {
                if (ext.getBlood() <= 0) {
                    String msgId = source.getMsgId();
                    // 拦截 Vampirism 的血饥饿伤害（DamageType msgId 包含 vampirism 或 blood）
                    if (msgId != null && (msgId.contains("vampirism") || msgId.contains("blood"))) {
                        event.setCanceled(true);
                    }
                }
            });
        });
    }

    /**
     * 通过反射调用 sync() 同步血量到客户端
     */
    private static void syncBlood(Object ext) {
        try {
            ext.getClass().getMethod("sync").invoke(ext);
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("Failed to sync blood value", e);
        }
    }
}
