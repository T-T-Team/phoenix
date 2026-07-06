package dev.tnt.phoenix.data;

import dev.tnt.phoenix.Phoenix;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ItemValueDefinitionManager extends SimpleJsonResourceReloadListener<List<ItemValueDefinition>> {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("item_value_manager");
    private final List<ItemValueDefinition> values = new ArrayList<>();
    private final Reference2IntMap<Item> cache = new Reference2IntOpenHashMap<>();

    public ItemValueDefinitionManager() {
        super(ItemValueDefinition.CODEC.listOf(), FileToIdConverter.json("slot_machine_values"));
    }

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
