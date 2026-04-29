package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TLMVMain.MOD_ID);

    public static final RegistryObject<BlockItem> MAID_ALTAR_INFUSION = ITEMS.register("maid_altar_infusion",
        () -> new BlockItem(ModBlocks.MAID_ALTAR_INFUSION.get(), new Item.Properties()));
}
