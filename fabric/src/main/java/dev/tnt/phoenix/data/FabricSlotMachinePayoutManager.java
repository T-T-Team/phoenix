package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import net.minecraft.core.HolderLookup;

public final class FabricSlotMachinePayoutManager extends SlotMachinePayoutManager {

    private HolderLookup.Provider provider;

    public FabricSlotMachinePayoutManager withHolderLookupProvider(HolderLookup.Provider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }
}
