package com.github.catbert.tlmv.task;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.task.behavior.HunterTaskBubbleBehavior;
import com.github.catbert.tlmv.task.behavior.MaidHunterCrossbowAttack;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCrossbowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.List;
import java.util.function.Predicate;

/**
 * 猎人女仆弩猎杀任务。类似 TLM 原版弩攻击，但限定猎人女仆，
 * 且优先攻击吸血鬼 faction 实体。
 */
public class HunterCrossbowTask implements IRangedAttackTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("touhou_little_maid_vampirism", "hunter_crossbow");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "basic_crossbow"));
        return item != null ? new ItemStack(item) : new ItemStack(Items.CROSSBOW);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> startAttacking = StartAttacking.create(this::canStartAttacking, VampireTargetFinder::findVampireTarget);
        BehaviorControl<EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(target -> !canStartAttacking(maid) || farAway(maid, target));
        BehaviorControl<EntityMaid> strafeTask = new MaidAttackStrafingTask();
        BehaviorControl<EntityMaid> shootTask = new MaidHunterCrossbowAttack();

        // 坐下时不添加移动/走位行为，仅攻击范围内目标
        if (maid.isMaidInSittingPose()) {
            return Lists.newArrayList(
                    Pair.of(1, new HunterTaskBubbleBehavior()),
                    Pair.of(5, startAttacking),
                    Pair.of(5, stopAttacking),
                    Pair.of(5, shootTask)
            );
        }

        BehaviorControl<EntityMaid> moveTask = MaidRangedWalkToTarget.create(0.6f);
        return Lists.newArrayList(
                Pair.of(1, new HunterTaskBubbleBehavior()),
                Pair.of(5, startAttacking),
                Pair.of(5, stopAttacking),
                Pair.of(5, moveTask),
                Pair.of(5, strafeTask),
                Pair.of(5, shootTask)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> startAttacking = StartAttacking.create(this::canStartAttacking, VampireTargetFinder::findVampireTarget);
        BehaviorControl<EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(target -> !canStartAttacking(maid) || farAway(maid, target));
        BehaviorControl<EntityMaid> shootTask = new MaidHunterCrossbowAttack();

        return Lists.newArrayList(
                Pair.of(1, new HunterTaskBubbleBehavior()),
                Pair.of(5, startAttacking),
                Pair.of(5, stopAttacking),
                Pair.of(5, shootTask)
        );
    }

    @Override
    public void performRangedAttack(EntityMaid shooter, LivingEntity target, float distanceFactor) {
        shooter.performCrossbowAttack(shooter, 1.6F);
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(
                Pair.of("is_hunter", this::isHunterMaid),
                Pair.of("has_crossbow", this::hasCrossBowHand)
        );
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem;
    }

    private boolean canStartAttacking(EntityMaid maid) {
        return isHunterMaid(maid) && hasCrossBowHand(maid) && hasAmmunition(maid);
    }

    private boolean isHunterMaid(EntityMaid maid) {
        return maid.getData(ModAttachments.VAMPIRE_MAID.get()).isHunter();
    }

    private boolean hasCrossBowHand(EntityMaid maid) {
        return maid.getMainHandItem().getItem() instanceof CrossbowItem;
    }

    private boolean hasAmmunition(EntityMaid maid) {
        return maid.getOffhandItem().getItem() instanceof FireworkRocketItem || findArrow(maid) >= 0;
    }

    private int findArrow(EntityMaid maid) {
        ItemStack mainHandItem = maid.getMainHandItem();
        if (mainHandItem.getItem() instanceof CrossbowItem crossbowItem) {
            CombinedInvWrapper handler = maid.getAvailableInv(true);
            return ItemsUtil.findStackSlot(handler, crossbowItem.getAllSupportedProjectiles());
        }
        return -1;
    }

    private boolean farAway(EntityMaid maid, LivingEntity target) {
        return !target.isAlive() || maid.distanceTo(target) > 16;
    }

    @Override
    public boolean canAttack(EntityMaid maid, LivingEntity target) {
        // 猎人女仆应主动攻击吸血鬼，绕过 TLM DefaultMonsterType 将其归为 FRIENDLY (Npc) 的限制
        if (VampireTargetFinder.isVampire(target)) return true;
        return IRangedAttackTask.super.canAttack(maid, target);
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }
}
