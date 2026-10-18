package dev.tnt.phoenix.platform.init;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class Reference<T> implements Supplier<T> {

    private final ResourceLocation identifier;
    private final Supplier<T> provider;

    private Reference(ResourceLocation identifier, Supplier<T> provider) {
        this.identifier = identifier;
        this.provider = provider;
    }

    public static <T> Reference<T> of(ResourceLocation id, Supplier<T> provider) {
        return new Reference<>(
                id,
                Suppliers.memoize(provider)
        );
    }

    @Override
    public T get() {
        return this.provider.get();
    }

    public ResourceLocation getKey() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reference<?> reference)) return false;
        return Objects.equals(identifier, reference.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identifier);
    }

    @Override
    public String toString() {
        return this.identifier.toString();
    }
}
