package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;

public class HunterTaskBubbleBehavior extends Behavior<EntityMaid> {

    private long bubbleId = -1L;

    public HunterTaskBubbleBehavior() {
        super(ImmutableMap.of());
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || cap.isHunter()) return;
        bubbleId = maid.getChatBubbleManager().addTextChatBubbleIfTimeout(
                "chat_bubble.touhou_little_maid_vampirism.only_hunter", bubbleId);
    }
}
