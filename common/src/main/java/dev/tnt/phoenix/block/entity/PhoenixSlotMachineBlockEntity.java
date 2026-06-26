package dev.tnt.phoenix.block.entity;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.menu.PhoenixSlotMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PhoenixSlotMachineBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    public PhoenixSlotMachineBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get(), worldPosition, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return PhoenixSlotMachineBlock.NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PhoenixSlotMachineMenu(containerId, inventory, this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.inventory = items;
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }
}
