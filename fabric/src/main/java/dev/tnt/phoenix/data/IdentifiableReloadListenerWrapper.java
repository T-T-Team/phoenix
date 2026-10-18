package dev.tnt.phoenix.data;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public record IdentifiableReloadListenerWrapper(ResourceLocation identifier, PreparableReloadListener wrapped) implements IdentifiableResourceReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
        return this.wrapped.reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2);
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.identifier;
    }
}
