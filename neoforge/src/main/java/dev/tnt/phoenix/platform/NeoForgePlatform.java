package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.PhoenixNeoForge;
import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class NeoForgePlatform implements Platform {

    public static final Map<ResourceKey<? extends Registry<?>>, NeoForgeRegistrationManager<?>> REGISTRIES = new HashMap<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        NeoForgeRegistrationManager<T> manager = new NeoForgeRegistrationManager<>();
        REGISTRIES.put(registry.key(), manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return CreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }

    @Override
    public void sendPacket(ServerPlayer target, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(target, payload);
    }

    @Override
    public SlotMachineInputApi getSlotMachineInputs() {
        return PhoenixNeoForge.INPUT_MANAGER;
    }

    @Override
    public SlotMachinePayoutApi getSlotMachinePayouts() {
        return PhoenixNeoForge.PAYOUT_MANAGER;
    }
}
