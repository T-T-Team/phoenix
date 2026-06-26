package dev.tnt.phoenix.platform.init;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface PlatformMenuProvider<V> {

    Component title();

    V getMenuData(ServerPlayer player);

    AbstractContainerMenu createMenu(int menuId, Inventory inventory, Player player);
}
