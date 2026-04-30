package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class VampireFoodBlocker implements IMaidMeal {
    @Override
    public boolean canMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        boolean isVampire = maid.getData(ModAttachments.VAMPIRE_MAID.get()).isVampire();
        if (!isVampire) return false;
        if (VampireMaidFoodFilter.isBloodFood(stack)) return false;
        return true; // 吸血鬼女仆 + 非血液食物 → 拦截
    }

    @Override
    public void onMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        // 空实现：不触发 startUsingItem、不增加好感度、不发送粒子
    }
}
