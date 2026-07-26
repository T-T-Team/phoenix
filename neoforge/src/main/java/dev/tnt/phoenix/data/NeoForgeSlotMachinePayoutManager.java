package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import net.minecraft.core.HolderLookup;

public final class NeoForgeSlotMachinePayoutManager extends SlotMachinePayoutManager {

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.getRegistryLookup();
    }
}
