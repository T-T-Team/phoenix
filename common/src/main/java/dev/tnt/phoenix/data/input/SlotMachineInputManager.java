package dev.tnt.phoenix.data.input;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.platform.MultiPlatformJsonReloadListener;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class SlotMachineInputManager extends MultiPlatformJsonReloadListener<List<SlotMachineInput>> implements SlotMachineInputApi {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("item_value_manager");
    private final List<SlotMachineInput> values = new ArrayList<>();
    private final Reference2IntMap<Item> cache = new Reference2IntOpenHashMap<>();

    public SlotMachineInputManager() {
        super(SlotMachineInput.CODEC.listOf(), FileToIdConverter.json("slot_machine/input"));
    }

    @Override
    public int getItemValue(Item item) {
        return this.cache.computeIfAbsent(item, this::lookupValue);
    }

    @Override
    public int getItemValue(ItemInstance instance, boolean useCount) {
        Holder<Item> holder = instance.typeHolder();
        int value = this.getItemValue(holder);
        if (useCount)
            value *= instance.count();
        return value;
    }

    @Override
    protected void apply(Map<Identifier, List<SlotMachineInput>> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.values.clear();
        this.cache.clear();
        preparations.values().stream()
                .flatMap(Collection::stream)
                .sorted()
                .forEach(this.values::add);
    }

    private int lookupValue(Item item) {
        for (SlotMachineInput definition : this.values) {
            if (definition.isForItem(item)) {
                return definition.value();
            }
        }
        return 0;
    }
}
