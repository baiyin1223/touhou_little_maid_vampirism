package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 猎人女仆击杀吸血鬼时掉落额外战利品。
 * 弩猎杀和钉桩猎杀任务共享此处理器。
 */
@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class VampireKillLootHandler {

    private static final ResourceLocation SOUL_ORB = ResourceLocation.fromNamespaceAndPath("vampirism", "soul_orb_vampire");
    private static final ResourceLocation BLOOD_BOTTLE = ResourceLocation.fromNamespaceAndPath("vampirism", "vampire_blood_bottle");

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        if (!VampirismHelper.isVampirismLoaded()) return;

        // Check if killer is a hunter maid
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        var cap = killer.getData(ModAttachments.VAMPIRE_MAID.get());
        if (!cap.isHunter()) return;

        // Check if target is a vampire faction entity
        if (!isVampireFactionEntity(target)) return;

        // 50% chance for soul orb, 50% for blood bottle
        if (target.getRandom().nextFloat() < 0.5f) {
            var soulOrbItem = BuiltInRegistries.ITEM.get(SOUL_ORB);
            if (soulOrbItem != null) {
                target.spawnAtLocation(new ItemStack(soulOrbItem));
            }
        }
        if (target.getRandom().nextFloat() < 0.5f) {
            var bloodBottleItem = BuiltInRegistries.ITEM.get(BLOOD_BOTTLE);
            if (bloodBottleItem != null) {
                target.spawnAtLocation(new ItemStack(bloodBottleItem));
            }
        }
    }

    private static boolean isVampireFactionEntity(LivingEntity target) {
        try {
            Class<?> apiClass = Class.forName("de.teamlapen.vampirism.api.VampirismAPI");
            Object registry = apiClass.getMethod("factionRegistry").invoke(null);
            Object faction = registry.getClass().getMethod("getFaction", net.minecraft.world.entity.Entity.class).invoke(registry, target);
            if (faction != null) {
                ResourceLocation id = (ResourceLocation) faction.getClass().getMethod("getID").invoke(faction);
                return "vampirism:vampire".equals(id.toString());
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
