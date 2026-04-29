package com.github.catbert.tlmv.meal;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class VampireMaidFoodFilter {

    public static boolean isBloodFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return false;
        }

        // 1. 检查物品是否属于 vampirism 命名空间
        boolean namespaceCheck = "vampirism".equals(key.getNamespace());
        if (namespaceCheck) {
            // 2. 特别处理 blood_bottle：通过 DataComponent 读取血量，blood > 0 表示有血
            if ("blood_bottle".equals(key.getPath())) {
                return VampirismHelper.hasBlood(stack);
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

        return false;
    }
}
