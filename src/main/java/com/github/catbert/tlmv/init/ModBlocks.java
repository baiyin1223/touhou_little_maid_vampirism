package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.block.MaidAltarBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TLMVMain.MOD_ID);

    public static final DeferredHolder<Block, Block> MAID_ALTAR_INFUSION = BLOCKS.register("maid_altar_infusion",
        () -> new MaidAltarBlock(BlockBehaviour.Properties.of()
            .strength(3.0f, 9.0f)
            .sound(SoundType.STONE)
            .noOcclusion()));
}
