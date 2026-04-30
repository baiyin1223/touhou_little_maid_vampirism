package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TLMVNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TLMVMain.MOD_ID).versioned("1");

        // server-bound
        registrar.playToServer(
                StartRitualPacket.TYPE,
                StartRitualPacket.STREAM_CODEC,
                StartRitualPacket::handle
        );
        registrar.playToServer(
                ClearMaidVampirePacket.TYPE,
                ClearMaidVampirePacket.STREAM_CODEC,
                ClearMaidVampirePacket::handle
        );

        // client-bound
        registrar.playToClient(
                SyncVampireMaidPacket.TYPE,
                SyncVampireMaidPacket.STREAM_CODEC,
                SyncVampireMaidPacket::handle
        );
    }
}
