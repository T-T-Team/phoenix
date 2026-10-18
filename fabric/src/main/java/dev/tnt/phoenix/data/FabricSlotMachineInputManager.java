package dev.tnt.phoenix.data;

import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public final class FabricSlotMachineInputManager extends SlotMachineInputManager implements IdentifiableResourceReloadListener {

    private HolderLookup.Provider provider;

    public FabricSlotMachineInputManager withHolderLookupProvider(HolderLookup.Provider provider) {
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
