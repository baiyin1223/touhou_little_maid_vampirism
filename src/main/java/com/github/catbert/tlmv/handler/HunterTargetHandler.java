package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class HunterTargetHandler {

    private static Class<?> hunterBaseClass;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!VampirismHelper.isVampirismLoaded()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!isHunterEntity(mob)) {
            return;
        }

        mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
            mob,
            EntityMaid.class,
            5,
            true,
            false,
            living -> {
                if (living instanceof EntityMaid maid) {
                    VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
                    // 仅攻击吸血鬼女仆，排除猎人女仆
                    return cap.isVampire() && !cap.isHunter();
                }
                return false;
            }
        ));

        // 替换 HurtByTargetGoal：猎人女仆攻击猎人时，猎人不还手
        replaceHurtByTarget(mob);
    }

    /**
     * 拦截猎人对猎人女仆的反击。当猎人女仆攻击猎人时，阻止猎人将猎人女仆设为攻击目标。
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!VampirismHelper.isVampirismLoaded()) return;

        if (!(event.getSource().getEntity() instanceof EntityMaid maid)) return;
        if (!isHunterEntityMob(event.getEntity())) return;

        VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
        if (cap.isHunter()) {
            // 猎人女仆攻击猎人 → 阻止反击。但正常的战斗仍进行（伤害不取消，只是不让猎人把猎人女仆设为目标）
            if (event.getEntity() instanceof Mob hunterMob) {
                LivingEntity currentTarget = hunterMob.getTarget();
                if (currentTarget == maid) {
                    hunterMob.setTarget(null);
                }
            }
        }
    }

    /**
     * 替换猎人的 HurtByTargetGoal，排除猎人女仆使其不会因被攻击而反击。
     */
    private static void replaceHurtByTarget(Mob mob) {
        if (!(mob instanceof PathfinderMob pm)) return;

        // Remove all HurtByTargetGoal instances from targetSelector
        pm.targetSelector.getAvailableGoals().stream()
                .filter(w -> w.getGoal() instanceof HurtByTargetGoal)
                .toList()
                .forEach(w -> pm.targetSelector.removeGoal(w.getGoal()));

        // Add filtered version
        pm.targetSelector.addGoal(1, new HurtByTargetGoal(pm) {
            @Override
            public boolean canUse() {
                if (super.canUse()) {
                    LivingEntity target = mob.getLastHurtByMob();
                    if (target instanceof EntityMaid maid) {
                        VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
                        if (cap.isHunter()) {
                            return false; // 猎人女仆攻击 → 不反击
                        }
                    }
                    if (mob.getLastHurtByMob() instanceof EntityMaid maid2) {
                        VampireMaidCapability cap2 = maid2.getData(ModAttachments.VAMPIRE_MAID.get());
                        return !cap2.isHunter();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private static boolean isHunterEntity(Mob mob) {
        try {
            if (hunterBaseClass == null) {
                hunterBaseClass = Class.forName("de.teamlapen.vampirism.entity.hunter.HunterBaseEntity");
            }
            return hunterBaseClass.isInstance(mob);
        } catch (ClassNotFoundException e) {
            TLMVMain.LOGGER.warn("[TLMV] HunterBaseEntity class not found, falling back to class name check");
            String className = mob.getClass().getName();
            return className.startsWith("de.teamlapen.vampirism") && className.contains("hunter");
        }
    }

    /**
     * Same as isHunterEntity but works with LivingEntity (not Mob).
     */
    private static boolean isHunterEntityMob(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return false;
        return isHunterEntity(mob);
    }
}
