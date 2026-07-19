package dev.tnt.phoenix;

import dev.tnt.phoenix.data.ItemValueDefinitionManager;
import dev.tnt.phoenix.data.NeoForgeItemValueDefinitionManager;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.network.*;
import dev.tnt.phoenix.platform.NeoForgePlatform;
import dev.tnt.phoenix.platform.NeoForgeRegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Phoenix.MOD_ID)
public final class PhoenixNeoForge {

    public PhoenixNeoForge(IEventBus modEventBus) {
        modEventBus.addListener((RegisterEvent event) -> this.register(event));
        modEventBus.addListener(this::initNetworking);
        modEventBus.addListener(this::registerConfigurationTasks);
        NeoForge.EVENT_BUS.addListener(this::registerDataPacks);

        Phoenix.init();
    }

    private void registerDataPacks(AddServerReloadListenersEvent event) {
        Phoenix.ITEM_VALUES = new NeoForgeItemValueDefinitionManager();
        event.addListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);
        event.addListener(ItemValueDefinitionManager.DATA_MANAGER_IDENTIFIER, Phoenix.ITEM_VALUES);
    }

    private void initNetworking(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Phoenix.MOD_ID)
                .executesOn(HandlerThread.MAIN);

        registrar.configurationToClient(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToClient(S2C_OpenPhoenixMachineScreen.TYPE, S2C_OpenPhoenixMachineScreen.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToClient(S2C_OpenPayoutScreen.TYPE, S2C_OpenPayoutScreen.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToServer(C2S_SlotMachineRequest.TYPE, C2S_SlotMachineRequest.STREAM_CODEC, (payload, ctx) -> payload.handle(ctx.player()));
        registrar.playToServer(C2S_SlotMachinePayoutRequest.TYPE, C2S_SlotMachinePayoutRequest.STREAM_CODEC, (payload, ctx) -> payload.handle(ctx.player()));
    }

    private void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncSlotMachineConfigsTask(event.getListener()));
    }

    @SuppressWarnings("unchecked")
    private <T> void register(RegisterEvent event) {
        for (var entry : NeoForgePlatform.REGISTRIES.entrySet()) {
            ResourceKey<Registry<T>> key = (ResourceKey<Registry<T>>) entry.getKey();
            event.register(key, helper -> {
                NeoForgeRegistrationManager<T> manager = (NeoForgeRegistrationManager<T>) entry.getValue();
                manager.bind(helper);
            });
        }
    }
}
