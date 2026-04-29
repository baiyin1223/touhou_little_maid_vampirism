package com.github.catbert.tlmv.compat.jade;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.registries.BuiltInRegistries;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VampireMaidComponentProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "vampire_status");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean("isVampireMaid")) {
            return;
        }

        int level = data.getInt("vampireLevel");
        tooltip.add(Component.translatable("tooltip.touhou_little_maid_vampirism.vampire_maid")
                .withStyle(ChatFormatting.DARK_PURPLE));

        if (data.getBoolean("isInfected")) {
            int seconds = data.getInt("infectionSeconds");
            tooltip.add(Component.translatable("tooltip.touhou_little_maid_vampirism.infected_seconds", seconds));
        }

        if (data.contains("bloodLevel") && data.contains("maxBlood")) {
            int blood = data.getInt("bloodLevel");
            int maxBlood = data.getInt("maxBlood");
            tooltip.add(Component.translatable("tooltip.touhou_little_maid_vampirism.blood_level", blood, maxBlood));
            tooltip.add(getVampireRankComponent(level));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        LivingEntity entity = (LivingEntity) accessor.getEntity();
        VampireMaidCapability cap = entity.getData(ModAttachments.VAMPIRE_MAID.get());
        Holder<MobEffect> sanguinare = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("vampirism", "sanguinare")).orElse(null);
        boolean hasSanguinare = sanguinare != null && entity.hasEffect(sanguinare);
        boolean isVampire = cap.isVampire();

        if (isVampire || hasSanguinare) {
            data.putBoolean("isVampireMaid", true);
            data.putBoolean("isInfected", hasSanguinare);

            if (hasSanguinare && sanguinare != null) {
                MobEffectInstance effect = entity.getEffect(sanguinare);
                int remainingTicks = effect != null ? effect.getDuration() : 0;
                data.putInt("infectionSeconds", remainingTicks / 20);
            }

            if (isVampire) {
                data.putInt("vampireLevel", cap.getVampireLevel());
                if (entity instanceof PathfinderMob mob) {
                    VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(ext -> {
                        int blood = ext.getBlood();
                        int maxBlood = ext.getMaxBlood();
                        // Treat -1 (unregistered entity) as 0
                        data.putInt("bloodLevel", Math.max(0, blood));
                        data.putInt("maxBlood", Math.max(0, maxBlood));
                    });
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    private static Component getVampireRankComponent(int level) {
        return Component.translatable("tooltip.touhou_little_maid_vampirism.vampire_rank." + level);
    }
}
