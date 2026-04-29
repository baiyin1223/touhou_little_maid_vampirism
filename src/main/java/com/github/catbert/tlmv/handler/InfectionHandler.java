package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.config.subconfig.InfectionConfig;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class InfectionHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
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
        VampireMaidCapability cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
        Holder<MobEffect> sanguinare = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare")).orElse(null);
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

        ResourceLocation sanguinareKey = ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare");
        Holder<MobEffect> effect = effectInstance.getEffect();
        ResourceLocation effectKey = effect.unwrapKey().map(k -> k.location()).orElse(null);
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) return;

        VampireMaidCapability expiredCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        if (!expiredCap.isVampire()) {
            expiredCap.setVampire(true);
            expiredCap.setVampireLevel(1);
            expiredCap.setHadSanguinare(false);
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();
        Holder<MobEffect> effect = effectInstance != null ? effectInstance.getEffect() : event.getEffect();

        ResourceLocation sanguinareKey = ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare");
        ResourceLocation effectKey = effect != null ? effect.unwrapKey().map(k -> k.location()).orElse(null) : null;
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) return;

        // In Vampirism 1.21, SanguinareEffect.applyEffectTick returns false at duration==2,
        // causing the effect to be removed via removeEffect() instead of expiring naturally.
        // This means MobEffectEvent.Expired never fires — only MobEffectEvent.Remove does.
        // So we must also handle conversion here.
        //
        // However, we need to distinguish "natural expiry" (applyEffectTick returns false at
        // duration ~1-2) from "external removal" (milk bucket, /effect clear, etc.).
        // When Vampirism internally removes the effect, the remaining duration is very low (~1-2).
        // When externally cleared, the duration is typically much higher.
        // We use a threshold of 5 ticks as a safety margin.
        VampireMaidCapability removedCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        if (removedCap.hasHadSanguinare() && !removedCap.isVampire()) {
            int remainingDuration = effectInstance != null ? effectInstance.getDuration() : 0;
            if (remainingDuration <= 5) {
                // Duration is very low — this is a natural expiry triggered by Vampirism's applyEffectTick
                removedCap.setVampire(true);
                removedCap.setVampireLevel(1);
            } else {
                // Duration is still high — external removal (milk/command), do NOT convert
                TLMVMain.LOGGER.debug("[TLMV] Sanguinare removed externally (duration={}), skipping conversion", remainingDuration);
            }
        }
        removedCap.setHadSanguinare(false);
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

        ResourceLocation sanguinareKey = ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare");
        Holder<MobEffect> effect = effectInstance.getEffect();
        ResourceLocation effectKey = effect.unwrapKey().map(k -> k.location()).orElse(null);
        if (effectKey == null || !effectKey.equals(sanguinareKey)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isMaidEntity(entity)) {
            return;
        }

        VampireMaidCapability addedCap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        addedCap.setHadSanguinare(true);
    }

    private static boolean isMaidEntity(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && "touhou_little_maid".equals(key.getNamespace());
    }
}
