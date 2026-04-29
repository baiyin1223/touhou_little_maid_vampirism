package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.blockentity.MaidAltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TLMVMain.MOD_ID);

    public static final RegistryObject<BlockEntityType<MaidAltarBlockEntity>> MAID_ALTAR = BLOCK_ENTITIES.register("maid_altar",
        () -> BlockEntityType.Builder.of(MaidAltarBlockEntity::new, ModBlocks.MAID_ALTAR_INFUSION.get()).build(null));
}
