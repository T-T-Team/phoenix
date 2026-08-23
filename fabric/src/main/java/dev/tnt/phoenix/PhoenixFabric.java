package dev.tnt.phoenix;

import dev.tnt.phoenix.data.FabricSlotMachineInputManager;
import dev.tnt.phoenix.data.FabricSlotMachinePayoutManager;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import dev.tnt.phoenix.network.*;
import dev.tnt.phoenix.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.packs.PackType;

public final class PhoenixFabric implements ModInitializer {

    public static final FabricSlotMachineInputManager INPUT_MANAGER = new FabricSlotMachineInputManager();
    public static final FabricSlotMachinePayoutManager PAYOUT_MANAGER = new FabricSlotMachinePayoutManager();

    @Override
    public void onInitialize() {
        Phoenix.LOGGER.debug("Phoenix bootstrap - Fabric platform");
        FabricPlatform.bindRegistries();

        // data managers
        DataResourceLoader resourceLoader = (DataResourceLoader) ResourceLoader.get(PackType.SERVER_DATA);
        resourceLoader.registerReloadListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);
        resourceLoader.registerReloadListener(SlotMachineInputManager.DATA_MANAGER_IDENTIFIER, INPUT_MANAGER::withHolderLookupProvider);
        resourceLoader.registerReloadListener(SlotMachinePayoutManager.DATA_MANAGER_IDENTIFIER, PAYOUT_MANAGER::withHolderLookupProvider);

        // packets
        PayloadTypeRegistry<RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.clientboundPlay();
        s2c.register(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC);
        s2c.register(S2C_OpenPhoenixMachineScreen.TYPE, S2C_OpenPhoenixMachineScreen.STREAM_CODEC);
        s2c.register(S2C_OpenPayoutScreen.TYPE, S2C_OpenPayoutScreen.STREAM_CODEC);
        s2c.register(S2C_SyncSlotMachineInputs.TYPE, S2C_SyncSlotMachineInputs.STREAM_CODEC);

        PayloadTypeRegistry<RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
        c2s.register(C2S_SlotMachineRequest.TYPE, C2S_SlotMachineRequest.STREAM_CODEC);
        c2s.register(C2S_SlotMachinePayoutRequest.TYPE, C2S_SlotMachinePayoutRequest.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(C2S_SlotMachineRequest.TYPE, (payload, ctx) -> payload.handle(ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_SlotMachinePayoutRequest.TYPE, (payload, ctx) -> payload.handle(ctx.player()));

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, _) -> {
            S2C_SyncSlotMachineConfigs slotMachineConfigPayload = Phoenix.SLOT_MACHINES.getPayload();
            ServerPlayNetworking.send(player, slotMachineConfigPayload);

            S2C_SyncSlotMachineInputs slotMachineInputPayload = INPUT_MANAGER.getPayload();
            ServerPlayNetworking.send(player, slotMachineInputPayload);
        });

        ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> Phoenix.modifyTooltip(stack, lines::add));
    }
}
