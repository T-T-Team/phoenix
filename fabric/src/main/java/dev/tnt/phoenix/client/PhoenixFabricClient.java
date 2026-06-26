package dev.tnt.phoenix.client;

import dev.tnt.phoenix.client.platform.PlatformScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class PhoenixFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PhoenixClient.init();
        PlatformScreenManager.getInstance().registerScreenConstructors(this::registerMenuScreen);
    }

    private <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerMenuScreen(MenuType<M> menuType, PlatformScreenManager.ScreenConstructor<M, S> constructor) {
        MenuScreens.register(menuType, constructor::createScreen);
    }
}
