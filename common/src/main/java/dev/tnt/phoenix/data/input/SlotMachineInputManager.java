package dev.tnt.phoenix.data.input;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.network.S2C_SyncSlotMachineInputs;
import dev.tnt.phoenix.platform.MultiPlatformJsonReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;

import java.util.*;

import static dev.tnt.phoenix.Phoenix.DATA_MARKER;

public abstract class SlotMachineInputManager extends MultiPlatformJsonReloadListener<List<SlotMachineInput>> implements SlotMachineInputApi {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("item_value_manager");
    private final List<SlotMachineInput> values = new ArrayList<>();
    private final Map<Item, Integer> cache = new IdentityHashMap<>();

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

    public S2C_SyncSlotMachineInputs getPayload() {
        return new S2C_SyncSlotMachineInputs(Phoenix.CONFIG.showItemInputValue ? this.values : Collections.emptyList());
    }

    public void receivePayload(S2C_SyncSlotMachineInputs payload) {
        this.values.clear();
        this.cache.clear();
        this.values.addAll(payload.inputs());
        Phoenix.LOGGER.info(DATA_MARKER, "Received {} slot machine input values from server", this.values.size());
    }

    @Override
    protected void apply(Map<Identifier, List<SlotMachineInput>> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.values.clear();
        this.cache.clear();
        preparations.values().stream()
                .flatMap(Collection::stream)
                .sorted()
                .forEach(this.values::add);
        Phoenix.LOGGER.info(DATA_MARKER, "Loaded {} slot machine input values from {} data files", this.values.size(), preparations.size());
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
