package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.init.ModDamageTypes;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class BloodDecayHandler {

    private static MobEffect getGarlicEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("vampirism", "garlic"));
    }

    /**
     * 主 tick 逻辑：反自动恢复 + 血量衰减 + blood=0 HP伤害
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;
        if (!(living instanceof EntityMaid maid)) return;

        var capOpt = ModCapabilities.getVampireMaid(maid);
        if (!capOpt.isPresent()) return;
        var cap = capOpt.orElse(null);
        if (cap == null || !cap.isVampire()) return;
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

                    // === blood = 0：施加 Slowness II（比大蒜的 Slowness I 更强） ===
                    MobEffectInstance currentSlowness = maid.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    if (currentSlowness == null || currentSlowness.getAmplifier() < 1) {
                        cap.setApplyingSlowness(true);
                        maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, false, true));
                        cap.setApplyingSlowness(false);
                    }

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
                            // 使用 hurt() 触发标准受伤动画和音效流程
                            // 然后用 setHealth 精确控制最终血量（避免被护甲/保护等减伤影响实际扣血量）
                            DamageSource source = new DamageSource(
                                maid.level().registryAccess()
                                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                                    .getOrThrow(ModDamageTypes.BLOOD_STARVATION)
                            );
                            float expectedHealth = currentHp - damage;
                            maid.hurt(source, damage);
                            // hurt() 可能被护甲/保护减伤，用 setHealth 确保扣血量准确
                            if (maid.getHealth() != expectedHealth) {
                                maid.setHealth(expectedHealth);
                            }
                        }
                    }
                    cap.setStarvationTimer(starvTimer);
                }

                // === 大蒜效果检测 ===
                MobEffect garlicEffect = getGarlicEffect();
                if (garlicEffect != null && maid.hasEffect(garlicEffect) && BloodConfig.GARLIC_DAMAGE_ENABLED.get()) {
                    int level = cap.getVampireLevel();

                    if (level >= 5) {
                        // 等级5：完全免疫大蒜效果（所有惩罚全部跳过）
                        cap.setGarlicHpTicker(0);
                        cap.setGarlicBloodTicker(0);
                        maid.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    } else {
                        // 施加/维持 Slowness I
                        if (!maid.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                            cap.setApplyingSlowness(true);
                            maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
                            cap.setApplyingSlowness(false);
                        }

                        // HP 伤害（等级 >= 3 免疫）
                        if (level < 3) {
                            int garlicHpTicker = cap.getGarlicHpTicker() + 1;
                            if (garlicHpTicker >= BloodConfig.GARLIC_HP_INTERVAL.get()) {
                                garlicHpTicker = 0;
                                float damage = BloodConfig.GARLIC_HP_DAMAGE.get().floatValue();
                                float currentHp = maid.getHealth();
                                DamageSource source = new DamageSource(
                                    maid.level().registryAccess()
                                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                                        .getOrThrow(ModDamageTypes.GARLIC_DAMAGE)
                                );
                                if (currentHp - damage <= 0) {
                                    maid.hurt(source, Float.MAX_VALUE);
                                } else {
                                    maid.hurt(source, damage);
                                }
                            }
                            cap.setGarlicHpTicker(garlicHpTicker);
                        } else {
                            cap.setGarlicHpTicker(0);
                        }

                        // Blood 加速消耗（等级 < 5 都受）
                        int garlicBloodTicker = cap.getGarlicBloodTicker() + 1;
                        if (garlicBloodTicker >= BloodConfig.GARLIC_BLOOD_DECAY_INTERVAL.get()) {
                            garlicBloodTicker = 0;
                            int newBlood = Math.max(0, ext.getBlood() - 1);
                            ext.setBlood(newBlood);
                            cap.setLastKnownBlood(newBlood);
                            syncBlood(ext);
                        }
                        cap.setGarlicBloodTicker(garlicBloodTicker);
                    }
                } else {
                    cap.setGarlicHpTicker(0);
                    cap.setGarlicBloodTicker(0);
                }
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
                if (effect == MobEffects.WEAKNESS || (effect == MobEffects.MOVEMENT_SLOWDOWN && !cap.isApplyingSlowness())) {
                    event.setResult(Event.Result.DENY);
                }
            }
        });
    }

    /**
     * 拦截 Vampirism 在 blood=0 时对吸血鬼女仆造成的致死伤害
     * 只拦截非我方的伤害（我方的 blood_starvation 正常通过）
     * 使用 LivingAttackEvent 在 hurt() 流程最早阶段拦截，避免受伤动画被触发
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof EntityMaid maid)) return;

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (!cap.isVampire()) {
                return;
            }

            DamageSource source = event.getSource();
            String msgId = source.getMsgId();
            boolean isBloodStarvation = source.typeHolder().is(ModDamageTypes.BLOOD_STARVATION);
            boolean isGarlicDamage = source.typeHolder().is(ModDamageTypes.GARLIC_DAMAGE);

            // 如果是我方的 blood_starvation 伤害，正常通过
            if (isBloodStarvation) {
                return;
            }
            // 如果是我方的 garlic_damage 伤害，正常通过
            if (isGarlicDamage) {
                return;
            }

            // 额外通过 msgId 双重保险（适配 "modid.message_id" 格式，如 touhou_little_maid_vampirism.blood_starvation）
            if (msgId != null && (msgId.contains("blood_starvation") || msgId.contains("garlic_damage"))) {
                return;
            }

            // 检查是否来自 Vampirism 的 blood_loss 伤害
            // Vampirism 在 blood=0 时使用的 DamageType msgId 是 "blood_loss"（ResourceKey: vampirism:blood_loss）
            VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> {
                int blood = ext.getBlood();
                if (blood <= 0) {
                    if ("blood_loss".equals(msgId)) {
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
