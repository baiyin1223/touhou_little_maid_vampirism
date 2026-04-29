package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.block.MaidAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TLMVMain.MOD_ID);

    public static final RegistryObject<Block> MAID_ALTAR_INFUSION = BLOCKS.register("maid_altar_infusion",
        () -> new MaidAltarBlock(BlockBehaviour.Properties.of()
            .strength(3.0f, 9.0f)
            .sound(SoundType.STONE)
            .noOcclusion()));
}
