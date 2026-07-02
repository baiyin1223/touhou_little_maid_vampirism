package com.github.catbert.tlmv.compat.tlm;

import com.github.catbert.tlmv.meal.BloodMeal;
import com.github.catbert.tlmv.meal.VampireFoodBlocker;
import com.github.catbert.tlmv.task.CollectBloodTask;
import com.github.catbert.tlmv.task.HunterCrossbowTask;
import com.github.catbert.tlmv.task.StakeTask;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@LittleMaidExtension
public class TLMExtension implements ILittleMaid {

    private static final ResourceLocation CRUCIFIX_NORMAL = new ResourceLocation("vampirism", "crucifix_normal");
    private static final ResourceLocation CRUCIFIX_ENHANCED = new ResourceLocation("vampirism", "crucifix_enhanced");
    private static final ResourceLocation CRUCIFIX_ULTIMATE = new ResourceLocation("vampirism", "crucifix_ultimate");

    @Override
    public void bindMaidBauble(BaubleManager manager) {
        var normal = ForgeRegistries.ITEMS.getValue(CRUCIFIX_NORMAL);
        var enhanced = ForgeRegistries.ITEMS.getValue(CRUCIFIX_ENHANCED);
        var ultimate = ForgeRegistries.ITEMS.getValue(CRUCIFIX_ULTIMATE);
        if (normal != null) manager.bind(normal, new CrucifixMaidBauble(4, 1));
        if (enhanced != null) manager.bind(enhanced, new CrucifixMaidBauble(8, 3));
        if (ultimate != null) manager.bind(ultimate, new CrucifixMaidBauble(10, 5));
    }

    @Override
    public void addMaidMeal(MaidMealManager manager) {
        // 注册血液食物 meal
        manager.addMaidMeal(MaidMealType.WORK_MEAL, new BloodMeal());
        manager.addMaidMeal(MaidMealType.HEAL_MEAL, new BloodMeal());
        manager.addMaidMeal(MaidMealType.HOME_MEAL, new BloodMeal());

        // 将 VampireFoodBlocker 插入到每种 meal type 列表的 index 0
        // 使其在默认 meal 之前匹配，阻止吸血鬼女仆触发普通食物的好感度/粒子循环
        VampireFoodBlocker blocker = new VampireFoodBlocker();
        MaidMealManager.getMaidMeals(MaidMealType.WORK_MEAL).add(0, blocker);
        MaidMealManager.getMaidMeals(MaidMealType.HEAL_MEAL).add(0, blocker);
        MaidMealManager.getMaidMeals(MaidMealType.HOME_MEAL).add(0, blocker);
    }

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new CollectBloodTask());
        manager.add(new HunterCrossbowTask());
        manager.add(new StakeTask());
    }
}
