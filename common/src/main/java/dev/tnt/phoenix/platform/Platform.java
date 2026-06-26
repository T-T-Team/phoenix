package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public interface Platform {

    <T> RegistrationManager<T> createRegistryManager(Registry<T> registry);

    CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator);

    @FunctionalInterface
    interface TabPopulator {
        void apply(Reference<? extends Item> ref);
    }
}
