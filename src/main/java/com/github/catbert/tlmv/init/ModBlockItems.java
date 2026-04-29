package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TLMVMain.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> MAID_ALTAR_INFUSION = ITEMS.register("maid_altar_infusion",
        () -> new BlockItem(ModBlocks.MAID_ALTAR_INFUSION.get(), new Item.Properties()));
}
