package dev.tnt.phoenix.platform;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

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
        var result = new HashMap<Identifier, T>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, this.lister, this.ops.get(), this.codec, result);
        return result;
    }
}
