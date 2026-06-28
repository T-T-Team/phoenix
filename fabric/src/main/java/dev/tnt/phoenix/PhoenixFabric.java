package dev.tnt.phoenix;

import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.network.S2C_SyncSlotMachineConfigs;
import dev.tnt.phoenix.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

public final class PhoenixFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Phoenix.init();
        FabricPlatform.bindRegistries();

        ResourceLoader resourceLoader = ResourceLoader.get(PackType.SERVER_DATA);
        resourceLoader.registerReloadListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);

        PayloadTypeRegistry.clientboundPlay()
                .register(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, _) -> {
            S2C_SyncSlotMachineConfigs payload = Phoenix.SLOT_MACHINES.getPayload();
            ServerPlayNetworking.send(player, payload);
        });
    }
}
