package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;
import java.util.Optional;

public class MoveToBloodTargetBehavior extends Behavior<EntityMaid> {

    public MoveToBloodTargetBehavior() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 600);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (maid.isMaidInSittingPose()) {
            return false;
        }
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return targetOpt.isPresent() && isValidTarget(maid, targetOpt.get());
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            maid.getNavigation().moveTo(target, 0.5);
        });
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty() || !isValidTarget(maid, targetOpt.get())) {
            maid.getNavigation().stop();
            return;
        }

        LivingEntity target = targetOpt.get();
        double distanceSqr = maid.distanceToSqr(target);
        // 到达 2.5 格以内时停止移动
        if (distanceSqr <= 2.5 * 2.5) {
            maid.getNavigation().stop();
        } else if (!maid.getNavigation().isInProgress()) {
            maid.getNavigation().moveTo(target, 0.5);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        if (maid.isMaidInSittingPose()) {
            return false;
        }
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) {
            return false;
        }
        LivingEntity target = targetOpt.get();
        if (!isValidTarget(maid, target)) {
            return false;
        }
        double distanceSqr = maid.distanceToSqr(target);
        int range = TaskConfig.COLLECT_BLOOD_RANGE.get();
        return distanceSqr <= range * range;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getNavigation().stop();
    }

    private boolean isValidTarget(EntityMaid maid, LivingEntity target) {
        if (target == maid) {
            return false;
        }
        if (!target.isAlive()) {
            return false;
        }
        return maid.distanceToSqr(target) <= TaskConfig.COLLECT_BLOOD_RANGE.get() * TaskConfig.COLLECT_BLOOD_RANGE.get();
    }
}
