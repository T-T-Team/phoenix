package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public interface Platform {

    <T> RegistrationManager<T> createRegistryManager(Registry<T> registry);

    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityFactory<T> factory, Reference<? extends Block> block);

    CreativeModeTab buildCreativeTab(ResourceLocation identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator);

    void sendPacket(ServerPlayer target, CustomPacketPayload payload);

    SlotMachineInputApi getSlotMachineInputs();

    SlotMachinePayoutApi getSlotMachinePayouts();

    @FunctionalInterface
    interface TabPopulator {
        void apply(Reference<? extends Item> ref);
    }

    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T createNewBlockEntity(BlockPos pos, BlockState state);
    }
}
