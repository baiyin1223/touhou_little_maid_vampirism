package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * 猎人任务前置校验行为。若非猎人女仆被分配到猎人专属任务，
 * 使用 addTextChatBubbleIfTimeout 实现与"收集血液"任务一致的聊天气泡逻辑。
 */
public class HunterTaskBubbleBehavior extends Behavior<EntityMaid> {

    private long hunterBubbleKey = -1;

    public HunterTaskBubbleBehavior() {
        super(Map.of(), Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        boolean isHunter = maid.getData(ModAttachments.VAMPIRE_MAID.get()).isHunter();
        if (!isHunter) {
            String langKey = "chat_bubble.touhou_little_maid_vampirism.only_hunter";
            hunterBubbleKey = maid.getChatBubbleManager().addTextChatBubbleIfTimeout(langKey, hunterBubbleKey);
        }
        return !isHunter; // 非猎人女仆 → 激活此行为（阻止任务执行）
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // 不需要额外逻辑
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (!maid.getData(ModAttachments.VAMPIRE_MAID.get()).isHunter()) {
            String langKey = "chat_bubble.touhou_little_maid_vampirism.only_hunter";
            hunterBubbleKey = maid.getChatBubbleManager().addTextChatBubbleIfTimeout(langKey, hunterBubbleKey);
        }
    }
}

