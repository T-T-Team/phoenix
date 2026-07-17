package dev.tnt.phoenix;

import dev.tnt.phoenix.data.ItemValueDefinitionManager;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.network.C2S_SlotMachineRequest;
import dev.tnt.phoenix.network.S2C_OpenPhoenixMachineScreen;
import dev.tnt.phoenix.network.S2C_SyncSlotMachineConfigs;
import dev.tnt.phoenix.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.packs.PackType;

public final class PhoenixFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Phoenix.init();
        FabricPlatform.bindRegistries();

        // data managers
        ResourceLoader resourceLoader = ResourceLoader.get(PackType.SERVER_DATA);
        resourceLoader.registerReloadListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);
        resourceLoader.registerReloadListener(ItemValueDefinitionManager.DATA_MANAGER_IDENTIFIER, Phoenix.ITEM_VALUES);

        // packets
        PayloadTypeRegistry<RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.clientboundPlay();
        s2c.register(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC);
        s2c.register(S2C_OpenPhoenixMachineScreen.TYPE, S2C_OpenPhoenixMachineScreen.STREAM_CODEC);
        s2c.register(S2C_RefreshSlotMachine.TYPE, S2C_RefreshSlotMachine.STREAM_CODEC);

        PayloadTypeRegistry<RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
        c2s.register(C2S_SlotMachineRequest.TYPE, C2S_SlotMachineRequest.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(C2S_SlotMachineRequest.TYPE, (payload, ctx) -> payload.handle(ctx.player()));

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, _) -> {
            S2C_SyncSlotMachineConfigs payload = Phoenix.SLOT_MACHINES.getPayload();
            ServerPlayNetworking.send(player, payload);
        });
    }
}
