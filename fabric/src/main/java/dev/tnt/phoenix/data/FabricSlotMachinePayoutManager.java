package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import net.minecraft.core.HolderLookup;

public final class FabricSlotMachinePayoutManager extends SlotMachinePayoutManager {

    private final HolderLookup.Provider provider;

    public FabricSlotMachinePayoutManager(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }
}
