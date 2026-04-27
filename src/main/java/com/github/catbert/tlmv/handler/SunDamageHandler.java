package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.config.subconfig.SunDamageConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class SunDamageHandler {

    private static final Map<Integer, Integer> sunDamageTimers = new HashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (!SunDamageConfig.ENABLE_SUN_DAMAGE.get()) {
            return;
        }

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            int entityId = entity.getId();

            if (!cap.isVampire()) {
                sunDamageTimers.remove(entityId);
                return;
            }

            if (cap.getVampireLevel() >= 5) {
                sunDamageTimers.remove(entityId);
                return;
            }

            boolean inSunlight = isInSunlight(entity);
            boolean sunscreen = hasSunscreenProtection(entity);

            if (inSunlight && !sunscreen) {
                int timer = sunDamageTimers.getOrDefault(entityId, 0) + 1;

                if (timer >= SunDamageConfig.SUN_DAMAGE_FREQUENCY.get()) {
                    timer = 0;
                    entity.hurt(entity.damageSources().onFire(), SunDamageConfig.SUN_DAMAGE_AMOUNT.get().floatValue());
                    entity.setSecondsOnFire(2);
                }

                sunDamageTimers.put(entityId, timer);
            } else {
                sunDamageTimers.remove(entityId);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        sunDamageTimers.remove(event.getEntity().getId());
    }

    private static boolean isInSunlight(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (!level.isDay()) {
            return false;
        }

        if (level.isRaining() && level.canSeeSky(pos)) {
            return false;
        }

        if (!level.canSeeSky(pos.above())) {
            return false;
        }

        return true;
    }

    private static boolean hasSunscreenProtection(LivingEntity entity) {
        if (!SunDamageConfig.RESPECT_SUNSCREEN.get()) {
            return false;
        }

        MobEffect sunscreenEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("vampirism", "sunscreen")
        );
        if (sunscreenEffect != null && entity.hasEffect(sunscreenEffect)) {
            return true;
        }

        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return true;
        }

        return false;
    }
}
