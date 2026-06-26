package dev.tnt.phoenix.client;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.platform.ClientPlatform;
import dev.tnt.phoenix.client.platform.PlatformScreenManager;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import dev.tnt.phoenix.platform.Services;

public final class PhoenixClient {

    public static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);

    public static void init() {
        PlatformScreenManager screenManager = PlatformScreenManager.getInstance();
        screenManager.registerScreenFactory(Phoenix.MENU_PHOENIX_SLOT_MACHINE, PhoenixSlotMachineScreen::new);
    }
}
