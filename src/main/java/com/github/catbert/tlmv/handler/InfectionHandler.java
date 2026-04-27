package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.config.subconfig.InfectionConfig;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class InfectionHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!isMaidEntity(target)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }

        // 方式1: 吸血鬼怪物实体攻击感染
        if (InfectionConfig.ENABLE_MONSTER_INFECTION.get() && VampirismHelper.isVampireEntity(attacker)) {
            tryInfectMaid(target);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!InfectionConfig.ENABLE_FANG_ITEM_INFECTION.get()) {
            return;
        }

        Entity target = event.getTarget();
        if (!isMaidEntity(target)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        if (!VampirismHelper.isVampireFang(stack)) {
            return;
        }

        // 取消默认交互，防止打开女仆GUI等
        event.setCanceled(true);

        if (target instanceof LivingEntity maid) {
            boolean success = tryInfectMaid(maid);

            if (success) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.maid_infected"));
            } else {
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.already_infected"));
            }
        }
    }

    private static boolean tryInfectMaid(LivingEntity maid) {
        return ModCapabilities.getVampireMaid(maid).map(cap -> {
            MobEffect sanguinare = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("vampirism", "sanguinare"));
            if (cap.isVampire() || (sanguinare != null && maid.hasEffect(sanguinare))) {
                return false;
            }
            if (sanguinare != null) {
                maid.addEffect(new MobEffectInstance(sanguinare, InfectionConfig.INFECTION_DURATION.get(), 0, false, true));
                cap.setHadSanguinare(true);
                return true;
            }
            TLMVMain.LOGGER.warn("[TLMV] tryInfectMaid: sanguinare effect is null, cannot infect");
            return false;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) {
            TLMVMain.LOGGER.warn("[TLMV] onEffectExpired: effectInstance is null, returning");
            return;
        }

        ResourceLocation sanguinareKey = new ResourceLocation("vampirism", "sanguinare");
        MobEffect effect = effectInstance.getEffect();
        ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) return;

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            if (!cap.isVampire()) {
                cap.setVampire(true);
                cap.setVampireLevel(1);
                cap.setHadSanguinare(false);
            }
        });
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();
        MobEffect effect = effectInstance != null ? effectInstance.getEffect() : event.getEffect();

        ResourceLocation sanguinareKey = new ResourceLocation("vampirism", "sanguinare");
        ResourceLocation effectKey = effect != null ? ForgeRegistries.MOB_EFFECTS.getKey(effect) : null;
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) return;

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            cap.setHadSanguinare(false);
        });
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) {
            return;
        }

        ResourceLocation sanguinareKey = new ResourceLocation("vampirism", "sanguinare");
        MobEffect effect = effectInstance.getEffect();
        ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) {
            return;
        }

        ModCapabilities.getVampireMaid(entity).ifPresent(cap -> {
            cap.setHadSanguinare(true);
        });
    }

    private static boolean isMaidEntity(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "touhou_little_maid".equals(key.getNamespace());
    }
}
