package dev.tnt.phoenix.menu;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class PhoenixSlotMachineMenu extends AbstractContainerMenu {

    private final PhoenixSlotMachineBlockEntity container;

    public PhoenixSlotMachineMenu(int containerId, Inventory inventory, PhoenixSlotMachineBlockEntity container) {
        super(Phoenix.MENU_PHOENIX_SLOT_MACHINE.get(), containerId);
        this.container = container;
    }

    public PhoenixSlotMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (PhoenixSlotMachineBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    public PhoenixSlotMachineBlockEntity getContainer() {
        return container;
    }
}
