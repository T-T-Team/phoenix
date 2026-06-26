package dev.tnt.phoenix;

import dev.tnt.phoenix.platform.NeoForgePlatform;
import dev.tnt.phoenix.platform.NeoForgeRegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Phoenix.MOD_ID)
public final class PhoenixNeoForge {

    public PhoenixNeoForge(IEventBus modEventBus) {
        modEventBus.addListener((RegisterEvent event) -> this.register(event));

        Phoenix.init();
    }

    @SuppressWarnings("unchecked")
    private <T> void register(RegisterEvent event) {
        for (var entry : NeoForgePlatform.REGISTRIES.entrySet()) {
            ResourceKey<Registry<T>> key = (ResourceKey<Registry<T>>) entry.getKey();
            event.register(key, helper -> {
                NeoForgeRegistrationManager<T> manager = (NeoForgeRegistrationManager<T>) entry.getValue();
                manager.bind(helper);
            });
        }
    }
}
