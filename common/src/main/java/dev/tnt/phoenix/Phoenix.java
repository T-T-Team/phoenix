package dev.tnt.phoenix;

import dev.tnt.phoenix.platform.Services;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Phoenix {

    public static final String MOD_ID = "phoenix";

    // Registries
    public static final RegistrationManager<Block> BLOCKS_REGISTRY = Services.PLATFORM.createRegistryManager(BuiltInRegistries.BLOCK);
    public static final RegistrationManager<Item> ITEMS_REGISTRY = Services.PLATFORM.createRegistryManager(BuiltInRegistries.ITEM);
    public static final RegistrationManager<CreativeModeTab> CREATIVE_TABS_REGISTRY = Services.PLATFORM.createRegistryManager(BuiltInRegistries.CREATIVE_MODE_TAB);

    // Blocks
    public static final Reference<Block> BLOCK_PHOENIX_SLOT_MACHINE = BLOCKS_REGISTRY.registerElement("phoenix_slot_machine", id -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))));
    // Items
    public static final Reference<BlockItem> ITEM_PHOENIX_SLOT_MACHINE = ITEMS_REGISTRY.registerElement("phoenix_slot_machine", id -> new BlockItem(BLOCK_PHOENIX_SLOT_MACHINE.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    // Creative tabs
    public static final Reference<CreativeModeTab> PHOENIX_TAB = CREATIVE_TABS_REGISTRY.registerElement("phoenix", id -> Services.PLATFORM.buildCreativeTab(id, ITEM_PHOENIX_SLOT_MACHINE, populator -> {
        populator.apply(ITEM_PHOENIX_SLOT_MACHINE);
    }));

    public static void init() {

    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
