package dev.crackmc.mod;

import dev.crackmc.mod.tunnel.TunnelManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class CrackMCClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TunnelManager.stopTunnel());
    }
}
