package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 在 TLM meal 系统层面拦截吸血鬼女仆对非血液食物的进食。
 * 必须插入到每种 meal type 列表的 index 0，以抢在默认 meal 之前匹配。
 * canMaidEat 返回 true 后，onMaidEat 不做任何操作，从而阻止好感度、粒子等副作用。
 */
public class VampireFoodBlocker implements IMaidMeal {

    @Override
    public boolean canMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        // 仅对吸血鬼女仆生效
        boolean isVampire = ModCapabilities.getVampireMaid(maid)
                .map(VampireMaidCapability::isVampire)
                .orElse(false);
        if (!isVampire) return false;

        // 血液食物放行，交给后续的 BloodMeal 处理
        if (VampireMaidFoodFilter.isBloodFood(stack)) return false;

        // 吸血鬼女仆 + 非血液食物 → 拦截，阻止默认 meal 执行
        return true;
    }

    @Override
    public void onMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        // 空实现：不触发 startUsingItem、不增加好感度、不发送粒子
    }
}
