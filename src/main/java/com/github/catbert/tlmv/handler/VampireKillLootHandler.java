package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class VampireKillLootHandler {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof EntityMaid maid)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || !cap.isHunter()) return;

        if (!isVampire(victim)) return;

        // 30% chance for blood bottle
        if (RANDOM.nextFloat() < 0.3f) {
            var bloodBottle = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vampirism", "vampire_blood_bottle"));
            if (bloodBottle != null) {
                spawnItemAt(victim, new ItemStack(bloodBottle));
            }
        }

        // 10% chance for soul orb
        if (RANDOM.nextFloat() < 0.1f) {
            var soulOrb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vampirism", "soul_orb_vampire"));
            if (soulOrb != null) {
                spawnItemAt(victim, new ItemStack(soulOrb));
            }
        }
    }

    private static void spawnItemAt(LivingEntity entity, ItemStack stack) {
        if (entity.level().isClientSide()) return;
        ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        entity.level().addFreshEntity(item);
    }

    private static boolean isVampire(LivingEntity e) {
        try {
            Class<?> helperClass = Class.forName("de.teamlapen.vampirism.util.Helper");
            return (boolean) helperClass.getMethod("isVampire", net.minecraft.world.entity.Entity.class).invoke(null, e);
        } catch (Exception ignored) { return false; }
    }
}
