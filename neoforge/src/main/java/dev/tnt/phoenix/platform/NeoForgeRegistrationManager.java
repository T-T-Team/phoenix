package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public final class NeoForgeRegistrationManager<T> implements RegistrationManager<T> {

    private List<Reference<?>> elements = new ArrayList<>();

    @Override
    public <V extends T> Reference<V> registerElement(String name, Function<Identifier, V> element) {
        Identifier id = Identifier.fromNamespaceAndPath(Phoenix.MOD_ID, name);
        Reference<V> reference = Reference.of(id, () -> element.apply(id));
        this.elements.add(reference);
        return reference;
    }

    public <V extends T> void bind(RegisterEvent.RegisterHelper<T> helper) {
        this.elements.forEach(element -> helper.register(element.getKey(), (V) element.get()));
        this.elements = null;
    }
}
