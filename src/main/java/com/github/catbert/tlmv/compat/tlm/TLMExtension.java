package com.github.catbert.tlmv.compat.tlm;

import com.github.catbert.tlmv.meal.BloodMeal;
import com.github.catbert.tlmv.meal.VampireFoodBlocker;
import com.github.catbert.tlmv.task.CollectBloodTask;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;

@LittleMaidExtension
public class TLMExtension implements ILittleMaid {

    @Override
    public void addMaidMeal(MaidMealManager manager) {
        manager.addMaidMeal(MaidMealType.WORK_MEAL, new BloodMeal());
        manager.addMaidMeal(MaidMealType.HEAL_MEAL, new BloodMeal());
        manager.addMaidMeal(MaidMealType.HOME_MEAL, new BloodMeal());

        // 将 VampireFoodBlocker 插入到每种 meal type 列表的 index 0
        VampireFoodBlocker blocker = new VampireFoodBlocker();
        MaidMealManager.getMaidMeals(MaidMealType.WORK_MEAL).add(0, blocker);
        MaidMealManager.getMaidMeals(MaidMealType.HEAL_MEAL).add(0, blocker);
        MaidMealManager.getMaidMeals(MaidMealType.HOME_MEAL).add(0, blocker);
    }

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new CollectBloodTask());
    }
}
