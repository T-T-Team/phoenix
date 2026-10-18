package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public final class FabricSlotMachinePayoutManager extends SlotMachinePayoutManager implements IdentifiableResourceReloadListener {

    private HolderLookup.Provider provider;

    public FabricSlotMachinePayoutManager withHolderLookupProvider(HolderLookup.Provider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }

    @Override
    public ResourceLocation getFabricId() {
        return DATA_MANAGER_IDENTIFIER;
    }
}
