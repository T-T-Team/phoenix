package dev.tnt.phoenix.client.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeClientPlatform implements ClientPlatform {

    @Override
    public void sendPacket(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
