package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FabricPlatform implements Platform {

    private static final List<FabricRegistrationManager<?>> REGISTRIES = new ArrayList<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        FabricRegistrationManager<T> manager = new FabricRegistrationManager<>(registry);
        REGISTRIES.add(manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return FabricCreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }

    public static void bindRegistries() {
        REGISTRIES.forEach(FabricRegistrationManager::bind);
    }
}
