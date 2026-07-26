package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public interface Platform {

    <T> RegistrationManager<T> createRegistryManager(Registry<T> registry);

    CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator);

    void sendPacket(ServerPlayer target, CustomPacketPayload payload);

    SlotMachineInputApi getSlotMachineInputs();

    SlotMachinePayoutApi getSlotMachinePayouts();

    @FunctionalInterface
    interface TabPopulator {
        void apply(Reference<? extends Item> ref);
    }
}
