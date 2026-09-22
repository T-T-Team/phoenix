package dev.tnt.phoenix.platform;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.tnt.phoenix.Phoenix;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class MultiPlatformJsonReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {

    private final Codec<T> codec;
    private final FileToIdConverter lister;
    private final Supplier<DynamicOps<JsonElement>> ops;

    public MultiPlatformJsonReloadListener(Codec<T> codec, FileToIdConverter lister) {
        this.codec = codec;
        this.lister = lister;
        this.ops = Suppliers.memoize(() -> {
            HolderLookup.Provider provider = this.getRegistryProvider();
            return provider.createSerializationContext(JsonOps.INSTANCE);
        });
    }

    protected abstract HolderLookup.Provider getRegistryProvider();

    @Override
    protected final Map<Identifier, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, T> resources = new HashMap<>();
        for (var entry : this.lister.listMatchingResources(manager).entrySet()) {
            Identifier identifier = entry.getKey();
            Identifier resourceId = this.lister.fileToId(identifier);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = StrictJsonParser.parse(reader);
                this.codec.parse(this.ops.get(), element)
                        .ifSuccess(result -> {
                            if (resources.putIfAbsent(resourceId, result) != null) {
                                throw new IllegalStateException("Duplicate data file: " + resourceId);
                            }
                        })
                        .ifError(error -> Phoenix.LOGGER.error("Couldn't parse data file '{}' from '{}': {}", resourceId, identifier, error));
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                Phoenix.LOGGER.error("Couldn't parse data file '{}' from '{}'", resourceId, identifier, e);
            }
        }
        return resources;
    }
}
