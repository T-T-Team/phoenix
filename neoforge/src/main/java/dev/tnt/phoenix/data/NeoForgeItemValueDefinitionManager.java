package dev.tnt.phoenix.data;

import net.minecraft.core.HolderLookup;

public class NeoForgeItemValueDefinitionManager extends ItemValueDefinitionManager {

    @Override
    protected HolderLookup.Provider getRegistryProvider() {
        return this.getRegistryLookup();
    }
}
