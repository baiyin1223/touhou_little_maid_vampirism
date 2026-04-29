package com.github.catbert.tlmv.init;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.inventory.MaidAltarMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TLMVMain.MOD_ID);

    public static final RegistryObject<MenuType<MaidAltarMenu>> MAID_ALTAR = MENUS.register("maid_altar",
        () -> IForgeMenuType.create((windowId, inv, data) -> new MaidAltarMenu(windowId, inv, data.readBlockPos())));
}
