package com.github.catbert.tlmv;

import com.github.catbert.tlmv.command.VampirismMaidCommand;
import com.github.catbert.tlmv.compat.tlm.TLMCompat;
import com.github.catbert.tlmv.config.TLMVConfig;
import com.github.catbert.tlmv.network.TLMVNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TLMVMain.MOD_ID)
public class TLMVMain {
    public static final String MOD_ID = "touhou_little_maid_vampirism";
    public static final Logger LOGGER = LogManager.getLogger();

    public TLMVMain() {
        LOGGER.info("TLMVMain constructor executing...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        TLMVConfig.register();

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("TLMVMain constructor completed.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TouhouLittleMaid Vampirism initializing...");
        TLMVNetwork.register();
        TLMCompat.init();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VampirismMaidCommand.register(event.getDispatcher());
    }
}
