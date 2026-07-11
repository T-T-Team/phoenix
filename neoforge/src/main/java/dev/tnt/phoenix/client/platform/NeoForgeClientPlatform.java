package dev.tnt.phoenix.client.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class NeoForgeClientPlatform implements ClientPlatform {

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
