package com.github.catbert.tlmv.task;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import com.github.catbert.tlmv.task.behavior.ExtractBloodBehavior;
import com.github.catbert.tlmv.task.behavior.FindBloodTargetBehavior;
import com.github.catbert.tlmv.task.behavior.MoveToBloodTargetBehavior;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Predicate;

public class CollectBloodTask implements IMaidTask {
    private static final ResourceLocation UID = new ResourceLocation("touhou_little_maid_vampirism", "collect_blood");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        Item bloodBottle = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vampirism", "blood_bottle"));
        return bloodBottle != null ? new ItemStack(bloodBottle) : new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        try {
            return Lists.newArrayList(
                    Pair.of(3, new FindBloodTargetBehavior()),
                    Pair.of(4, new MoveToBloodTargetBehavior()),
                    Pair.of(5, new ExtractBloodBehavior())
            );
        } catch (Exception e) {
            TLMVMain.LOGGER.error("Failed to create blood collection brain tasks", e);
            return Lists.newArrayList();
        }
    }

    @Override
    public boolean isEnable(EntityMaid maid) {
        return TaskConfig.COLLECT_BLOOD_ENABLED.get();
    }

    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return true;
    }

    @Override
    public boolean enablePanic(EntityMaid maid) {
        return true;
    }

    @Override
    public boolean enableEating(EntityMaid maid) {
        return true;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(
            Pair.of("is_vampire", this::isVampireMaid),
            Pair.of("has_container", this::hasAvailableContainer)
        );
    }

    private boolean isVampireMaid(EntityMaid maid) {
        return ModCapabilities.getVampireMaid(maid)
                .map(cap -> cap.isVampire())
                .orElse(false);
    }

    private boolean hasAvailableContainer(EntityMaid maid) {
        var inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (FindBloodTargetBehavior.isAvailableContainer(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }
}
