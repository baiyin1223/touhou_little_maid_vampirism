package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class BloodMeal implements IMaidMeal {

    @Override
    public boolean canMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        // 1. 必须是吸血鬼女仆
        if (!isVampireMaid(maid)) return false;
        // 2. 必须是血液食物
        if (!VampireMaidFoodFilter.isBloodFood(stack)) return false;
        // 3. 通过 Vampirism API 检查血值是否需要补充
        return VampirismAPI.getExtendedCreatureVampirism(maid)
                .map(ext -> ext.getBlood() < ext.getMaxBlood())
                .orElse(false);
    }

    @Override
    public void onMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        // 所有血液食物统一走 TLM 原版进食动画
        // 血值更新和物品消耗统一在 MaidFoodHandler.onItemUseFinish() 中处理
        // 以确保与 LivingEntityUseItemEvent 生命周期一致
        if (!maid.isUsingItem()) {
            maid.startUsingItem(hand);
        }
        TLMVMain.LOGGER.debug("[BloodMeal] Vampire maid started using blood food: hand={}, item={}", hand, stack.getItem());
    }

    private boolean isVampireMaid(EntityMaid maid) {
        return ModCapabilities.getVampireMaid(maid)
                .map(VampireMaidCapability::isVampire)
                .orElse(false);
    }
}
