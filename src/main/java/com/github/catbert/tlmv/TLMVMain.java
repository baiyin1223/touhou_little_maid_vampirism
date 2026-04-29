package com.github.catbert.tlmv;

import com.github.catbert.tlmv.command.VampirismMaidCommand;
import com.github.catbert.tlmv.compat.tlm.TLMCompat;
import com.github.catbert.tlmv.config.TLMVConfig;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.init.ModBlockEntities;
import com.github.catbert.tlmv.init.ModBlockItems;
import com.github.catbert.tlmv.init.ModBlocks;
import com.github.catbert.tlmv.init.ModMenuTypes;
import com.github.catbert.tlmv.network.TLMVNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TLMVMain.MOD_ID)
public class TLMVMain {
    public static final String MOD_ID = "touhou_little_maid_vampirism";
    public static final Logger LOGGER = LogManager.getLogger();

    public TLMVMain(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("TLMVMain constructor executing...");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(TLMVNetwork::register);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(TLMVMain::onBuildCreativeTab);

        TLMVConfig.register(modContainer);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("TLMVMain constructor completed.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TouhouLittleMaid Vampirism initializing...");
        TLMCompat.init();
    }

    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "main"))) {
            event.accept(ModBlockItems.MAID_ALTAR_INFUSION.get());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VampirismMaidCommand.register(event.getDispatcher());
    }
}
