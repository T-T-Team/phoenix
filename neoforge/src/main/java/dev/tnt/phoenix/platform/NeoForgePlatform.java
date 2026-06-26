package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class NeoForgePlatform implements Platform {

    public static final Map<ResourceKey<? extends Registry<?>>, NeoForgeRegistrationManager<?>> REGISTRIES = new HashMap<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        NeoForgeRegistrationManager<T> manager = new NeoForgeRegistrationManager<>();
        REGISTRIES.put(registry.key(), manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return CreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }
}
