package dev.tnt.phoenix.client.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientPlatform {

    void sendPacket(CustomPacketPayload payload);
}
