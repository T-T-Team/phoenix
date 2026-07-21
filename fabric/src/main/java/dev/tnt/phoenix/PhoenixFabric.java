package dev.tnt.phoenix;

import dev.tnt.phoenix.data.FabricSlotMachineInputManager;
import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import dev.tnt.phoenix.network.*;
import dev.tnt.phoenix.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.packs.PackType;

public final class PhoenixFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Phoenix.init();
        FabricPlatform.bindRegistries();

        // data managers
        DataResourceLoader resourceLoader = (DataResourceLoader) ResourceLoader.get(PackType.SERVER_DATA);
        resourceLoader.registerReloadListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);
        resourceLoader.registerReloadListener(SlotMachineInputManager.DATA_MANAGER_IDENTIFIER, provider -> {
            Phoenix.ITEM_VALUES = new FabricSlotMachineInputManager(provider);
            return Phoenix.ITEM_VALUES;
        });
        resourceLoader.registerReloadListener(SlotMachinePayoutManager.DATA_MANAGER_IDENTIFIER, Phoenix.PAYOUT_MANAGER);

        // packets
        PayloadTypeRegistry<RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.clientboundPlay();
        s2c.register(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC);
        s2c.register(S2C_OpenPhoenixMachineScreen.TYPE, S2C_OpenPhoenixMachineScreen.STREAM_CODEC);
        s2c.register(S2C_OpenPayoutScreen.TYPE, S2C_OpenPayoutScreen.STREAM_CODEC);

        PayloadTypeRegistry<RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
        c2s.register(C2S_SlotMachineRequest.TYPE, C2S_SlotMachineRequest.STREAM_CODEC);
        c2s.register(C2S_SlotMachinePayoutRequest.TYPE, C2S_SlotMachinePayoutRequest.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(C2S_SlotMachineRequest.TYPE, (payload, ctx) -> payload.handle(ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_SlotMachinePayoutRequest.TYPE, (payload, ctx) -> payload.handle(ctx.player()));

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, _) -> {
            S2C_SyncSlotMachineConfigs payload = Phoenix.SLOT_MACHINES.getPayload();
            ServerPlayNetworking.send(player, payload);
        });
    }
}
