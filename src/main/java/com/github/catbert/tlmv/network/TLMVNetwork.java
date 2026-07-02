package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class TLMVNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TLMVMain.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++, SyncVampireMaidPacket.class,
                SyncVampireMaidPacket::encode,
                SyncVampireMaidPacket::decode,
                SyncVampireMaidPacket::handle);
        INSTANCE.registerMessage(packetId++, StartRitualPacket.class,
                StartRitualPacket::encode,
                StartRitualPacket::new,
                StartRitualPacket::handle);
        INSTANCE.registerMessage(packetId++, ClearMaidVampirePacket.class,
                ClearMaidVampirePacket::encode,
                ClearMaidVampirePacket::new,
                ClearMaidVampirePacket::handle);
        INSTANCE.registerMessage(packetId++, OpenMaidTrainingPacket.class,
                OpenMaidTrainingPacket::encode,
                OpenMaidTrainingPacket::decode,
                OpenMaidTrainingPacket::handle);
        INSTANCE.registerMessage(packetId++, PromoteMaidPacket.class,
                PromoteMaidPacket::encode,
                PromoteMaidPacket::decode,
                PromoteMaidPacket::handle);
        INSTANCE.registerMessage(packetId++, RevertHunterMaidPacket.class,
                RevertHunterMaidPacket::encode,
                RevertHunterMaidPacket::decode,
                RevertHunterMaidPacket::handle);
    }
}
