package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.TLMVMain;
import de.teamlapen.vampirism.items.VampirismItemBloodFoodItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.ForgeRegistries;

public class VampireMaidFoodFilter {

    public static boolean isBloodFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 排除剑类物品（如 Vampirism 的 heart_seeker/heart_striker 吸血鬼之剑）
        if (stack.getItem() instanceof SwordItem) {
            return false;
        }
        // 0. Vampirism 官方血液食物基类检测（覆盖 Vampirism 自身及 extends 该类的附属模组）
        if (stack.getItem() instanceof VampirismItemBloodFoodItem) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }

        // 1. 检查物品是否属于 vampirism 命名空间
        boolean namespaceCheck = "vampirism".equals(key.getNamespace());
        if (namespaceCheck) {
            // 2. 特别处理 blood_bottle：damage > 0 表示有血，damage == 0 表示空瓶
            if ("blood_bottle".equals(key.getPath())) {
                int damage = stack.getDamageValue();
                boolean hasBlood = damage > 0;
                return hasBlood;
            }
            // 排除 garlic_bread（非血液食物）
            if ("garlic_bread".equals(key.getPath())) {
                return false;
            }
            // 3. 其他 vampirism 物品（包括 human_heart、rotten_heart 等心脏物品）视为血液食物
            return true;
        }

        // 4. 检查物品是否有 vampirism 相关的标签
        boolean hasVampirismTag = stack.getTags().anyMatch(tag -> "vampirism".equals(tag.location().getNamespace()));
        if (hasVampirismTag) {
            return true;
        }

        // 5. 兜底：检查物品 ID 是否包含 "blood"、"heart" 等关键词（覆盖附属模组）
        String path = key.getPath().toLowerCase();
        boolean keywordCheck = path.contains("blood") || path.contains("heart");
        if (keywordCheck) {
            return true;
        }

        // 6. 外部血液食物注册表（如 VampiresDelight 等第三方模组的精确映射）
        if (ExternalBloodFoodRegistry.isBloodFood(key)) {
            return true;
        }

        return false;
    }
}
