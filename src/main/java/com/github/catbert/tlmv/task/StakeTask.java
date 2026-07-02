package com.github.catbert.tlmv.task;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.task.behavior.HunterTaskBubbleBehavior;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMeleeAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Predicate;

public class StakeTask implements IAttackTask {
    public static final ResourceLocation UID = new ResourceLocation("touhou_little_maid_vampirism", "stake_attack");

    @Override public ResourceLocation getUid() { return UID; }

    @Override
    public ItemStack getIcon() {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vampirism", "stake"));
        return item != null ? new ItemStack(item) : new ItemStack(Items.WOODEN_SWORD);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> startAttacking = StartAttacking.create(this::canStartAttacking, VampireTargetFinder::findVampireTarget);
        BehaviorControl<EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(target -> !canStartAttacking(maid) || farAway(maid, target));
        BehaviorControl<EntityMaid> attackTask = MaidMeleeAttack.create(20);

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
        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || cap.getHunterLevel() < 4) return false;
        return isVampireEntity(target);
    }

    @Override
    public boolean doExtraAttack(EntityMaid maid, Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || cap.getHunterLevel() < 4) return false;
        living.setHealth(0);
        living.die(maid.damageSources().mobAttack(maid));
        return true;
    }

    private boolean canStartAttacking(EntityMaid maid) { return isHunterMaid(maid) && hasStake(maid); }

    private boolean isHunterMaid(EntityMaid maid) {
        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        return cap != null && cap.isHunter();
    }

    private boolean hasStake(EntityMaid maid) { return isWeapon(maid, maid.getMainHandItem()); }

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > 4;
    }

    private boolean isVampireEntity(Entity target) {
        return VampireTargetFinder.isVampire(target instanceof LivingEntity l ? l : null);
    }

    @Override
    public boolean canAttack(EntityMaid maid, LivingEntity target) {
        if (VampireTargetFinder.isVampire(target)) return true;
        return IAttackTask.super.canAttack(maid, target);
    }

    @Override public SoundEvent getAmbientSound(EntityMaid maid) { return null; }
}
