package dev.tnt.phoenix;

import dev.tnt.phoenix.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;

public final class PhoenixFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Phoenix.init();
        FabricPlatform.bindRegistries();
    }
}
