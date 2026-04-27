package com.github.catbert.tlmv.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VampireMaidCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final VampireMaidCapability capability = new VampireMaidCapability();
    private final LazyOptional<VampireMaidCapability> optional = LazyOptional.of(() -> capability);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.VAMPIRE_MAID.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return capability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        capability.deserializeNBT(tag);
    }
}
