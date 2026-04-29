package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.blockentity.MaidAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TLMVMain.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaidAltarBlockEntity>> MAID_ALTAR = BLOCK_ENTITIES.register("maid_altar",
        () -> BlockEntityType.Builder.of(MaidAltarBlockEntity::new, ModBlocks.MAID_ALTAR_INFUSION.get()).build(null));
}
