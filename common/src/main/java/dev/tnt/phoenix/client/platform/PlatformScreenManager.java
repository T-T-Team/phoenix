package dev.tnt.phoenix.client.platform;

import dev.tnt.phoenix.platform.init.Reference;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class PlatformScreenManager {

    private static final PlatformScreenManager INSTANCE = new PlatformScreenManager();
    private List<ScreenEntry<?, ?>> registeredScreenConstructors = new ArrayList<>();

    private PlatformScreenManager() {}

    public static PlatformScreenManager getInstance() {
        return INSTANCE;
    }

    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreenFactory(Reference<MenuType<M>> reference, ScreenConstructor<M, S> constructor) {
        this.registeredScreenConstructors.add(new ScreenEntry<>(reference, constructor));
    }

    @SuppressWarnings("unchecked")
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreenConstructors(BiConsumer<MenuType<M>, ScreenConstructor<M, S>> consumer) {
        for (ScreenEntry<?, ?> entry : this.registeredScreenConstructors) {
            var reference = entry.reference();
            MenuType<M> menuType = (MenuType<M>) reference.get();
            ScreenConstructor<M, S> constructor = (ScreenConstructor<M, S>) entry.constructor();
            consumer.accept(menuType, constructor);
        }
        this.registeredScreenConstructors = null;
    }

    @FunctionalInterface
    public interface ScreenConstructor<T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> {
        S createScreen(T menu, Inventory inventory, Component title);
    }

    private record ScreenEntry<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>(Reference<MenuType<M>> reference, ScreenConstructor<M, S> constructor) {}
}
