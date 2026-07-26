package dev.tnt.phoenix.data.input;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;

public interface SlotMachineInputApi {

    int getItemValue(Item item);

    int getItemValue(ItemInstance instance, boolean withCount);

    default int getItemValue(Holder<Item> holder) {
        return this.getItemValue(holder.value());
    }
}
