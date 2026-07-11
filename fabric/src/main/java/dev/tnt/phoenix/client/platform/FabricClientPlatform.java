package dev.tnt.phoenix.client.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class FabricClientPlatform implements ClientPlatform {

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
