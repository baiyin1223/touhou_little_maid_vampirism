package com.github.catbert.tlmv.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

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
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "vampirism:vampire_fang".equals(key.toString());
    }
}
