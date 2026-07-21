package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import net.minecraft.core.HolderLookup;

public class NeoForgeSlotMachineInputManager extends SlotMachineInputManager {

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.getRegistryLookup();
    }
}
