package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> BLOOD_STARVATION = 
        ResourceKey.create(Registries.DAMAGE_TYPE, 
            ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "blood_starvation"));

    public static final ResourceKey<DamageType> GARLIC_DAMAGE = 
        ResourceKey.create(Registries.DAMAGE_TYPE, 
            ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "garlic_damage"));
}
