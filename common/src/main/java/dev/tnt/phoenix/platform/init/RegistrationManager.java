package dev.tnt.phoenix.platform.init;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

public interface RegistrationManager<T> {

    <V extends T> Reference<V> registerElement(String name, Function<ResourceLocation, V> element);

    default <V extends T> Reference<V> registerElement(String name, Supplier<V> element) {
        return this.registerElement(name, id -> element.get());
    }
}
