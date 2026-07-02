package com.github.catbert.tlmv.task;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.task.behavior.HunterTaskBubbleBehavior;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMeleeAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.TaskEquipUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Predicate;

/**
 * 猎人女仆木桩猎杀任务。使用 Vampirism stake 近战攻击吸血鬼，
 * Lv.4+ 可秒杀吸血鬼。
 */
public class StakeTask implements IAttackTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("touhou_little_maid_vampirism", "stake_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "stake"));
        return item != null ? new ItemStack(item) : new ItemStack(Items.WOODEN_SWORD);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> startAttacking = StartAttacking.create(this::canStartAttacking, VampireTargetFinder::findVampireTarget);
        BehaviorControl<EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(target -> !canStartAttacking(maid) || farAway(maid, target));
        BehaviorControl<EntityMaid> attackTask = MaidMeleeAttack.create(20);

        // 坐下时不添加移动行为，但仍可攻击范围内目标
        if (maid.isMaidInSittingPose()) {
            return Lists.newArrayList(
                    Pair.of(1, new HunterTaskBubbleBehavior()),
                    Pair.of(5, startAttacking),
                    Pair.of(5, stopAttacking),
                    Pair.of(5, attackTask)
            );
        }

        BehaviorControl<Mob> moveTask = SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.6f);
        return Lists.newArrayList(
                Pair.of(1, new HunterTaskBubbleBehavior()),
                Pair.of(5, startAttacking),
                Pair.of(5, stopAttacking),
                Pair.of(5, moveTask),
                Pair.of(5, attackTask)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        return createBrainTasks(maid);
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(
                Pair.of("is_hunter", this::isHunterMaid),
                Pair.of("has_stake", this::hasStake)
        );
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "vampirism:stake".equals(key != null ? key.toString() : null);
    }

    @Override
    public boolean hasExtraAttack(EntityMaid maid, Entity target) {
        if (!isHunterMaid(maid)) return false;
        int hunterLevel = maid.getData(ModAttachments.VAMPIRE_MAID.get()).getHunterLevel();
        if (hunterLevel < 4) return false;
        return isVampireEntity(target);
    }

    @Override
    public boolean doExtraAttack(EntityMaid maid, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        int hunterLevel = maid.getData(ModAttachments.VAMPIRE_MAID.get()).getHunterLevel();
        if (hunterLevel >= 4) {
            // Stake insta-kill for Lv.4+ hunter maids against vampires
            living.setHealth(0);
            living.die(maid.damageSources().mobAttack(maid));
            return true;
        }
        return false;
    }

    private boolean canStartAttacking(EntityMaid maid) {
        return isHunterMaid(maid) && hasStake(maid);
    }

    private boolean isHunterMaid(EntityMaid maid) {
        return maid.getData(ModAttachments.VAMPIRE_MAID.get()).isHunter();
    }

    private boolean hasStake(EntityMaid maid) {
        return isWeapon(maid, maid.getMainHandItem());
    }

    private boolean isVampireEntity(Entity target) {
        try {
            Class<?> apiClass = Class.forName("de.teamlapen.vampirism.api.VampirismAPI");
            Object registry = apiClass.getMethod("factionRegistry").invoke(null);
            Object faction = registry.getClass().getMethod("getFaction", Entity.class).invoke(registry, target);
            if (faction != null) {
                ResourceLocation id = (ResourceLocation) faction.getClass().getMethod("getID").invoke(faction);
                return "vampirism:vampire".equals(id.toString());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > 16;
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    @Override
    public boolean canAttack(EntityMaid maid, LivingEntity target) {
        // 猎人女仆应主动攻击吸血鬼，绕过 TLM DefaultMonsterType 将其归为 FRIENDLY (Npc) 的限制
        if (VampireTargetFinder.isVampire(target)) return true;
        return IAttackTask.super.canAttack(maid, target);
    }
}
