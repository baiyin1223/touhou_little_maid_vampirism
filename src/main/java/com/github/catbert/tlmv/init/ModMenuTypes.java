package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.inventory.MaidAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TLMVMain.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MaidAltarMenu>> MAID_ALTAR = MENUS.register("maid_altar",
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new MaidAltarMenu(windowId, inv, data.readBlockPos())));
}
