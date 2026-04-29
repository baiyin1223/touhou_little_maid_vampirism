package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class FindBloodTargetBehavior extends Behavior<EntityMaid> {

    private int nextCheckTickCount = 0;
    private long vampireBubbleKey = -1;
    private boolean isFirstContainerWarning = true;
    private long lastContainerWarningTime = -1;

    public FindBloodTargetBehavior() {
        super(Map.of(), Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        // 检查女仆是否为吸血鬼
        boolean isVampire = maid.getData(ModAttachments.VAMPIRE_MAID.get()).isVampire();
        if (!isVampire) {
            String langKey = "chat_bubble.touhou_little_maid_vampirism.only_vampire";
            vampireBubbleKey = maid.getChatBubbleManager().addTextChatBubbleIfTimeout(langKey, vampireBubbleKey);
            return false;
        }

        if (maid.isMaidInSittingPose()) {
            return false;
        }

        if (!hasAvailableContainer(maid)) {
            long currentTime = level.getGameTime();
            String langKey = "chat_bubble.touhou_little_maid_vampirism.no_container";

            if (isFirstContainerWarning) {
                isFirstContainerWarning = false;
                lastContainerWarningTime = currentTime;
                TextChatBubbleData bubble = TextChatBubbleData.create(
                    200,
                    Component.translatable(langKey),
                    IChatBubbleData.TYPE_2,
                    0
                );
                maid.getChatBubbleManager().addChatBubble(bubble);
            } else if (lastContainerWarningTime < 0 || currentTime - lastContainerWarningTime >= 600) {
                lastContainerWarningTime = currentTime;
                TextChatBubbleData bubble = TextChatBubbleData.create(
                    100,
                    Component.translatable(langKey),
                    IChatBubbleData.TYPE_2,
                    0
                );
                maid.getChatBubbleManager().addChatBubble(bubble);
            }
            return false;
        }

        if (!canAcceptBloodBottle(maid)) {
            return false;
        }

        // 前置条件满足，允许启动
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        nextCheckTickCount = 0; // 立即允许首次扫描
        // 清除可能残留的旧目标（活动切换后可能残留无效引用）
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        // 立即尝试寻找目标
        int range = TaskConfig.COLLECT_BLOOD_RANGE.get();
        AABB searchBox = maid.getBoundingBox().inflate(range);
        var mobs = level.getEntitiesOfClass(PathfinderMob.class, searchBox,
                mob -> isValidTarget(maid, mob));

        mobs.stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr))
                .ifPresent(target -> {
                    maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                });
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        // 持续运行，在 tick() 中扫描和管理目标
        return true;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (maid.isMaidInSittingPose()) {
            maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }

        // 验证当前目标
        Optional<LivingEntity> currentTarget = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (currentTarget.isPresent()) {
            if (!isValidTarget(maid, currentTarget.get())) {
                maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                // 目标失效后立即重置扫描冷却，避免等待 20-40 tick
                nextCheckTickCount = 0;
            } else {
                return; // 当前目标仍然有效，无需扫描
            }
        }

        // 当前无有效目标，执行频率限制扫描
        if (nextCheckTickCount > 0) {
            nextCheckTickCount--;
            return;
        }
        nextCheckTickCount = 20 + maid.getRandom().nextInt(20);

        // 检查背包是否有空间
        if (!canAcceptBloodBottle(maid)) {
            return; // 背包满载，暂停扫描
        }

        // 扫描新目标
        int range = TaskConfig.COLLECT_BLOOD_RANGE.get();
        AABB searchBox = maid.getBoundingBox().inflate(range);
        var mobs = level.getEntitiesOfClass(PathfinderMob.class, searchBox,
                mob -> isValidTarget(maid, mob));

        mobs.stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr))
                .ifPresent(target -> {
                    maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                });
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        // 清除 ATTACK_TARGET，防止残留无效目标影响后续重启
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        nextCheckTickCount = 0;
    }

    private boolean isValidTarget(EntityMaid maid, LivingEntity target) {
        if (target == maid) {
            return false;
        }
        if (!target.isAlive()) {
            return false;
        }
        // 排除其他吸血鬼女仆
        if (target instanceof EntityMaid otherMaid) {
            boolean isVampire = otherMaid.getData(ModAttachments.VAMPIRE_MAID.get()).isVampire();
            if (isVampire) {
                return false;
            }
        }

        // 检查目标是否在搜索范围内
        int range = TaskConfig.COLLECT_BLOOD_RANGE.get();
        if (maid.distanceToSqr(target) > (double) range * range) {
            return false;
        }

        if (!(target instanceof net.minecraft.world.entity.PathfinderMob pathfinderTarget)) {
            return false;
        }
        var ext = VampirismAPI.getExtendedCreatureVampirism(pathfinderTarget).orElse(null);
        return ext != null && ext.getBlood() > 1;
    }

    private boolean hasAvailableContainer(EntityMaid maid) {
        var inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isAvailableContainer(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查背包是否有空间存放新血瓶。
     * 满足以下任一条件即返回 true：
     * 1. 有空槽位
     * 2. 有未满的血瓶（可原地填充，不需要额外空间）
     */
    public static boolean canAcceptBloodBottle(EntityMaid maid) {
        var inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                return true; // 有空槽位
            }
            // 有未满的血瓶（可原地填充）
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null && "vampirism:blood_bottle".equals(key.toString()) && !VampirismHelper.isBloodBottleFull(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAvailableContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            return true;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && "vampirism:blood_bottle".equals(key.toString())) {
            return !VampirismHelper.isBloodBottleFull(stack);
        }
        return false;
    }
}
