package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.PhoenixFabric;
import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FabricPlatform implements Platform {

    private static final List<FabricRegistrationManager<?>> REGISTRIES = new ArrayList<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        FabricRegistrationManager<T> manager = new FabricRegistrationManager<>(registry);
        REGISTRIES.add(manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return FabricCreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }

    @Override
    public void sendPacket(ServerPlayer target, CustomPacketPayload payload) {
        ServerPlayNetworking.send(target, payload);
    }

    @Override
    public SlotMachineInputApi getSlotMachineInputs() {
        return PhoenixFabric.INPUT_MANAGER;
    }

    @Override
    public SlotMachinePayoutApi getSlotMachinePayouts() {
        return PhoenixFabric.PAYOUT_MANAGER;
    }

    public static void bindRegistries() {
        REGISTRIES.forEach(FabricRegistrationManager::bind);
    }
}
