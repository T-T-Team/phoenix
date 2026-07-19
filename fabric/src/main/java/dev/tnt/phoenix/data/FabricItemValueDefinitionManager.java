package dev.tnt.phoenix.data;

import net.minecraft.core.HolderLookup;

public class FabricItemValueDefinitionManager extends ItemValueDefinitionManager {

    private final HolderLookup.Provider provider;

    public FabricItemValueDefinitionManager(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.provider;
    }
}
