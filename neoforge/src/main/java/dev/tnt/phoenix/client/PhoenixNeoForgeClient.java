package dev.tnt.phoenix.client;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.platform.PlatformScreenManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = Phoenix.MOD_ID, dist = Dist.CLIENT)
public final class PhoenixNeoForgeClient {

    public PhoenixNeoForgeClient(IEventBus modEventBus) {
        PhoenixClient.init();

        modEventBus.addListener(this::registerMenuScreens);
    }

    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        PlatformScreenManager.getInstance()
                .registerScreenConstructors((type, constructor) -> registerMenuScreen(event, type, constructor));
    }

    private <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerMenuScreen(RegisterMenuScreensEvent event, MenuType<M> menuType, PlatformScreenManager.ScreenConstructor<M, S> constructor) {
        event.register(menuType, constructor::createScreen);
    }
}
