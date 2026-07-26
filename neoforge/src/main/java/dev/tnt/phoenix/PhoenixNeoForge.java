package dev.tnt.phoenix;

import dev.tnt.phoenix.data.NeoForgeSlotMachineInputManager;
import dev.tnt.phoenix.data.NeoForgeSlotMachinePayoutManager;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.data.input.SlotMachineInput;
import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import dev.tnt.phoenix.network.*;
import dev.tnt.phoenix.platform.NeoForgePlatform;
import dev.tnt.phoenix.platform.NeoForgeRegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Phoenix.MOD_ID)
public final class PhoenixNeoForge {

    public static final NeoForgeSlotMachineInputManager INPUT_MANAGER = new NeoForgeSlotMachineInputManager();
    public static final NeoForgeSlotMachinePayoutManager PAYOUT_MANAGER = new NeoForgeSlotMachinePayoutManager();

    public PhoenixNeoForge(IEventBus modEventBus) {
        Phoenix.LOGGER.debug("Phoenix bootstrap - NeoForge platform");
        modEventBus.addListener((RegisterEvent event) -> this.register(event));
        modEventBus.addListener(this::initNetworking);
        NeoForge.EVENT_BUS.addListener(this::registerDataPacks);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(this::onTooltip);
    }

    private void registerDataPacks(AddServerReloadListenersEvent event) {
        event.addListener(SlotMachineDataManager.DATA_MANAGER_IDENTIFIER, Phoenix.SLOT_MACHINES);
        event.addListener(SlotMachineInputManager.DATA_MANAGER_IDENTIFIER, INPUT_MANAGER);
        event.addListener(SlotMachinePayoutManager.DATA_MANAGER_IDENTIFIER, PAYOUT_MANAGER);
    }

    private void initNetworking(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Phoenix.MOD_ID)
                .executesOn(HandlerThread.MAIN);

        registrar.playToClient(S2C_SyncSlotMachineConfigs.TYPE, S2C_SyncSlotMachineConfigs.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToClient(S2C_OpenPhoenixMachineScreen.TYPE, S2C_OpenPhoenixMachineScreen.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToClient(S2C_OpenPayoutScreen.TYPE, S2C_OpenPayoutScreen.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToClient(S2C_SyncSlotMachineInputs.TYPE, S2C_SyncSlotMachineInputs.STREAM_CODEC, (payload, _) -> payload.handle());
        registrar.playToServer(C2S_SlotMachineRequest.TYPE, C2S_SlotMachineRequest.STREAM_CODEC, (payload, ctx) -> payload.handle(ctx.player()));
        registrar.playToServer(C2S_SlotMachinePayoutRequest.TYPE, C2S_SlotMachinePayoutRequest.STREAM_CODEC, (payload, ctx) -> payload.handle(ctx.player()));
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        S2C_SyncSlotMachineConfigs configPayload = Phoenix.SLOT_MACHINES.getPayload();
        S2C_SyncSlotMachineInputs inputPayload = INPUT_MANAGER.getPayload();
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, configPayload);
            PacketDistributor.sendToPlayer(player, inputPayload);
        });
    }

    private void onTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        Phoenix.modifyTooltip(itemStack, event.getToolTip()::add);
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
