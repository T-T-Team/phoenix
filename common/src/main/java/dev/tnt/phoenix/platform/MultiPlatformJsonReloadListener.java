package dev.tnt.phoenix.platform;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.tnt.phoenix.Phoenix;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static dev.tnt.phoenix.Phoenix.DATA_MARKER;

public abstract class MultiPlatformJsonReloadListener<T> extends SimplePreparableReloadListener<Map<ResourceLocation, T>> {

    private final Codec<T> codec;
    private final String path;
    private final Supplier<DynamicOps<JsonElement>> ops;
    private final Gson gson = new Gson();

    public MultiPlatformJsonReloadListener(Codec<T> codec, String path) {
        this.codec = codec;
        this.path = path;
        this.ops = Suppliers.memoize(() -> {
            HolderLookup.Provider provider = this.getRegistryProvider();
            return provider.createSerializationContext(JsonOps.INSTANCE);
        });
    }

    protected abstract HolderLookup.Provider getRegistryProvider();

    @Override
    protected final Map<ResourceLocation, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var files = new HashMap<ResourceLocation, JsonElement>();
        var result = new HashMap<ResourceLocation, T>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, this.path, this.gson, files);
        for (var entry : files.entrySet()) {
            ResourceLocation key = entry.getKey();
            JsonElement element = entry.getValue();
            DataResult<T> parseResult = this.codec.parse(this.ops.get(), element);
            parseResult.ifError(error -> Phoenix.LOGGER.error(DATA_MARKER, "Failed to parse file '{}' due to error: {}", key, error))
                    .ifSuccess(value -> result.put(key, value));
        }
        return result;
    }
}
