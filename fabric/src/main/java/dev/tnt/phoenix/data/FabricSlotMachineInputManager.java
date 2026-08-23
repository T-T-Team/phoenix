package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import net.minecraft.core.HolderLookup;

public final class FabricSlotMachineInputManager extends SlotMachineInputManager {

    private HolderLookup.Provider provider;

    public FabricSlotMachineInputManager withHolderLookupProvider(HolderLookup.Provider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }
}
