package com.github.catbert.tlmv.util;

import de.teamlapen.vampirism.api.VampirismDataComponents;
import de.teamlapen.vampirism.api.components.IBottleBlood;
import de.teamlapen.vampirism.items.component.BottleBlood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;

public class VampirismHelper {
    private static final String VAMPIRISM_MOD_ID = "vampirism";

    public static boolean isVampirismLoaded() {
        return ModList.get().isLoaded(VAMPIRISM_MOD_ID);
    }

    public static boolean isVampirePlayer(Player player) {
        if (!isVampirismLoaded()) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("de.teamlapen.vampirism.api.VampirismAPI");
            Object vampirePlayer = apiClass.getMethod("getVampirePlayer", Player.class).invoke(null, player);
            return vampirePlayer != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isVampireEntity(Entity entity) {
        if (!isVampirismLoaded()) {
            return false;
        }

        if (entity instanceof Monster) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (key != null && VAMPIRISM_MOD_ID.equals(key.getNamespace())) {
                return true;
            }
        }

        try {
            Class<?> iVampireClass = Class.forName("de.teamlapen.vampirism.api.entity.vampire.IVampire");
            return iVampireClass.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isVampireFang(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "vampirism:vampire_fang".equals(key.toString());
    }

    // ===== Blood Bottle DataComponent Helpers (1.21+) =====

    /**
     * 检查物品是否为 Vampirism 血瓶
     */
    public static boolean isBloodBottle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "vampirism:blood_bottle".equals(key.toString());
    }

    /**
     * 读取血瓶中的血量（0~9），非血瓶返回 0
     */
    public static int getBloodAmount(ItemStack stack) {
        IBottleBlood component = stack.get(VampirismDataComponents.BOTTLE_BLOOD.get());
        return component != null ? component.blood() : 0;
    }

    /**
     * 设置血瓶中的血量（0~9）
     */
    public static void setBloodAmount(ItemStack stack, int amount) {
        stack.set(VampirismDataComponents.BOTTLE_BLOOD.get(), new BottleBlood(Math.max(0, Math.min(9, amount))));
    }

    /**
     * 血瓶是否已满（血量 == 9）
     */
    public static boolean isBloodBottleFull(ItemStack stack) {
        return getBloodAmount(stack) >= 9;
    }

    /**
     * 血瓶是否有血（血量 > 0）
     */
    public static boolean hasBlood(ItemStack stack) {
        return getBloodAmount(stack) > 0;
    }

    /**
     * 创建一个带有指定血量的血瓶 ItemStack
     */
    public static ItemStack createBloodBottle(int bloodAmount) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "blood_bottle")));
        setBloodAmount(stack, bloodAmount);
        return stack;
    }
}
