package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import net.minecraft.core.HolderLookup;

public class FabricSlotMachineInputManager extends SlotMachineInputManager {

    private final HolderLookup.Provider provider;

    public FabricSlotMachineInputManager(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }
}
