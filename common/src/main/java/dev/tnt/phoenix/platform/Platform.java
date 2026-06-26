package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public interface Platform {

    <T> RegistrationManager<T> createRegistryManager(Registry<T> registry);

    CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator);

    <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<? super FriendlyByteBuf, D> dataCodec);

    <T> void openMenu(ServerPlayer player, StreamCodec<? super FriendlyByteBuf, T> codec, PlatformMenuProvider<T> provider);

    @FunctionalInterface
    interface TabPopulator {
        void apply(Reference<? extends Item> ref);
    }

    @FunctionalInterface
    interface MenuFactory<M extends AbstractContainerMenu, D> {
        M createMenu(int menuId, Inventory inventory, D d);
    }

    interface PlatformMenuProvider<D> {
        D getMenuData(ServerPlayer player);
        Component getTitle();
        AbstractContainerMenu createMenu(int menuId, Inventory inventory, Player player);
    }
}
