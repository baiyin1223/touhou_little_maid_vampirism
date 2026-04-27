package com.github.catbert.tlmv.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class SunDamageHelper {

    public static boolean isGettingSunDamage(LivingEntity entity) {
        if (ModList.get().isLoaded("vampirism")) {
            try {
                Boolean result = checkVampirismSunDamage(entity);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                // 回退到简单检测
            }
        }
        return simpleSunDamageCheck(entity);
    }

    private static Boolean checkVampirismSunDamage(LivingEntity entity) throws Exception {
        Class<?> vampirismAPIClass = Class.forName("de.teamlapen.vampirism.api.VampirismAPI");
        Object sundamageRegistry = vampirismAPIClass
                .getMethod("sundamageRegistry")
                .invoke(null);

        if (sundamageRegistry == null) {
            return null;
        }

        try {
            return (Boolean) sundamageRegistry.getClass()
                    .getMethod("isGettingSundamage", LivingEntity.class)
                    .invoke(sundamageRegistry, entity);
        } catch (NoSuchMethodException e) {
            // 尝试带 Level 参数的重载
            return (Boolean) sundamageRegistry.getClass()
                    .getMethod("isGettingSundamage", LivingEntity.class, Level.class)
                    .invoke(sundamageRegistry, entity, entity.level());
        }
    }

    public static boolean simpleSunDamageCheck(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (!level.isDay()) {
            return false;
        }

        if (level.isRaining() && level.canSeeSky(pos)) {
            return false;
        }

        return level.canSeeSky(pos.above());
    }

    public static boolean hasSunscreenEffect(LivingEntity entity) {
        MobEffect sunscreenEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("vampirism", "sunscreen")
        );
        if (sunscreenEffect != null && entity.hasEffect(sunscreenEffect)) {
            return true;
        }

        return entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }
}
