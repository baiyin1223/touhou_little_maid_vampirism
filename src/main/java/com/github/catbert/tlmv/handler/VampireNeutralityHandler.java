package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class VampireNeutralityHandler {

    private static Field targetTypeField;
    private static Field randomIntervalField;
    private static Field targetConditionsField;
    private static Field selectorField;

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

        if (!isVampireFactionEntity(mob)) {
            return;
        }

        initReflectionFields();

        List<WrappedGoal> toReplace = new ArrayList<>();
        for (WrappedGoal wrappedGoal : mob.targetSelector.getAvailableGoals()) {
            Goal goal = wrappedGoal.getGoal();
            if (!(goal instanceof NearestAttackableTargetGoal<?> targetGoal)) {
                continue;
            }

            Class<?> type;
            try {
                type = (Class<?>) targetTypeField.get(targetGoal);
            } catch (IllegalAccessException e) {
                TLMVMain.LOGGER.warn("[TLMV] Failed to get targetType from NearestAttackableTargetGoal", e);
                continue;
            }

            if (!PathfinderMob.class.isAssignableFrom(type)) {
                continue;
            }

            toReplace.add(wrappedGoal);
        }

        for (WrappedGoal wrappedGoal : toReplace) {
            NearestAttackableTargetGoal<?> targetGoal = (NearestAttackableTargetGoal<?>) wrappedGoal.getGoal();

            int randomInterval;
            try {
                randomInterval = (int) randomIntervalField.get(targetGoal);
            } catch (IllegalAccessException e) {
                TLMVMain.LOGGER.warn("[TLMV] Failed to get randomInterval from NearestAttackableTargetGoal", e);
                randomInterval = 10;
            }

            Predicate<LivingEntity> originalPredicate = getOriginalPredicate(targetGoal);
            Predicate<LivingEntity> newPredicate = living -> {
                if (living instanceof EntityMaid maid) {
                    return !maid.getData(ModAttachments.VAMPIRE_MAID.get()).isVampire();
                }
                return originalPredicate == null || originalPredicate.test(living);
            };

            int priority = wrappedGoal.getPriority();

            mob.targetSelector.removeGoal(targetGoal);
            mob.targetSelector.addGoal(priority, new NearestAttackableTargetGoal<>(
                    mob,
                    PathfinderMob.class,
                    randomInterval,
                    true,
                    false,
                    newPredicate
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!VampirismHelper.isVampirismLoaded()) {
            return;
        }

        if (event.getSource() == null || event.getSource().getEntity() == null) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof IFactionEntity attacker)) {
            return;
        }

        if (attacker.getFaction() != VReference.VAMPIRE_FACTION) {
            return;
        }

        if (!(event.getEntity() instanceof EntityMaid maid)) {
            return;
        }

        boolean isVampire = maid.getData(ModAttachments.VAMPIRE_MAID.get()).isVampire();

        if (isVampire) {
            event.setCanceled(true);
        }
    }

    private static boolean isVampireFactionEntity(Mob mob) {
        if (!(mob instanceof IFactionEntity factionEntity)) {
            return false;
        }
        return factionEntity.getFaction() == VReference.VAMPIRE_FACTION;
    }

    private static void initReflectionFields() {
        if (targetTypeField != null) {
            return;
        }
        try {
            targetTypeField = findFieldWithFallback(NearestAttackableTargetGoal.class, "targetType", Class.class);
            randomIntervalField = findFieldWithFallback(NearestAttackableTargetGoal.class, "randomInterval", int.class);
            targetConditionsField = findFieldWithFallback(NearestAttackableTargetGoal.class, "targetConditions", TargetingConditions.class);
            selectorField = findFieldWithFallback(TargetingConditions.class, "selector", Predicate.class);
        } catch (Exception e) {
            TLMVMain.LOGGER.error("[TLMV] Failed to initialize reflection fields for VampireNeutralityHandler", e);
        }
    }

    private static Field findFieldWithFallback(Class<?> clazz, String mappedName, Class<?> fieldType) {
        try {
            Field field = ObfuscationReflectionHelper.findField(clazz, mappedName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new RuntimeException("Could not find field of type " + fieldType.getName() + " in " + clazz.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static Predicate<LivingEntity> getOriginalPredicate(NearestAttackableTargetGoal<?> goal) {
        if (targetConditionsField == null || selectorField == null) {
            return null;
        }
        try {
            TargetingConditions conditions = (TargetingConditions) targetConditionsField.get(goal);
            if (conditions == null) {
                return null;
            }
            return (Predicate<LivingEntity>) selectorField.get(conditions);
        } catch (IllegalAccessException e) {
            TLMVMain.LOGGER.warn("[TLMV] Failed to get original predicate", e);
            return null;
        }
    }
}
