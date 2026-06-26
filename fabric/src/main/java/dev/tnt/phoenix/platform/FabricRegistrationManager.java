package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public final class FabricRegistrationManager<T> implements RegistrationManager<T> {

    private final Registry<T> registry;
    private List<Reference<?>> elements = new ArrayList<>();

    public FabricRegistrationManager(Registry<T> registry) {
        this.registry = registry;
    }

    @Override
    public <V extends T> Reference<V> registerElement(String name, Function<Identifier, V> element) {
        Identifier id = Phoenix.identifier(name);
        Reference<V> reference = Reference.of(id, () -> element.apply(id));
        this.elements.add(reference);
        return reference;
    }

    public <V extends T> void bind() {
        this.elements.forEach(ref -> {
            Identifier id = ref.getKey();
            Registry.register(this.registry, id, (V) ref.get());
        });
        this.elements = null;
    }
}
