package dev.tnt.phoenix.data;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.tnt.phoenix.Phoenix;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;

import java.util.*;
import java.util.function.Supplier;

public abstract class ItemValueDefinitionManager extends SimplePreparableReloadListener<Map<Identifier, List<ItemValueDefinition>>> {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("item_value_manager");
    private final Codec<List<ItemValueDefinition>> codec;
    private final Supplier<DynamicOps<JsonElement>> ops;
    private final FileToIdConverter lister;
    private final List<ItemValueDefinition> values = new ArrayList<>();
    private final Reference2IntMap<Item> cache = new Reference2IntOpenHashMap<>();

    public ItemValueDefinitionManager() {
        this.codec = ItemValueDefinition.CODEC.listOf();
        this.lister = FileToIdConverter.json("slot_machine_values");
        this.ops = Suppliers.memoize(() -> {
            HolderLookup.Provider provider = this.getRegistryProvider();
            return provider.createSerializationContext(JsonOps.INSTANCE);
        });
    }

    protected abstract HolderLookup.Provider getRegistryProvider();

    public int getItemValue(Item item) {
        return this.cache.computeIfAbsent(item, this::lookupValue);
    }

    public int getItemValue(Holder<Item> holder) {
        return this.getItemValue(holder.value());
    }

    public int getItemValue(ItemInstance instance, boolean useCount) {
        Holder<Item> holder = instance.typeHolder();
        int value = this.getItemValue(holder);
        if (useCount)
            value *= instance.count();
        return value;
    }

    @Override
    protected Map<Identifier, List<ItemValueDefinition>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, List<ItemValueDefinition>> result = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, this.lister, this.ops.get(), this.codec, result);
        return result;
    }

    @Override
    protected void apply(Map<Identifier, List<ItemValueDefinition>> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.values.clear();
        this.cache.clear();
        preparations.values().stream()
                .flatMap(Collection::stream)
                .sorted()
                .forEach(this.values::add);
    }

    private int lookupValue(Item item) {
        for (ItemValueDefinition definition : this.values) {
            if (definition.isForItem(item)) {
                return definition.value();
            }
        }
        return 0;
    }
}
