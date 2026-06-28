package dev.tnt.phoenix;

import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.menu.PhoenixSlotMachineMenu;
import dev.tnt.phoenix.platform.Platform;
import dev.tnt.phoenix.platform.Services;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;
import java.util.function.Function;

public class Phoenix {

    public static final String MOD_ID = "phoenix";
    public static final Platform PLATFORM = Services.load(Platform.class);

    // Data managers
    public static final SlotMachineDataManager SLOT_MACHINES = new SlotMachineDataManager();

    // Registries
    public static final RegistrationManager<Block> BLOCKS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.BLOCK);
    public static final RegistrationManager<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final RegistrationManager<MenuType<?>> MENU_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.MENU);
    public static final RegistrationManager<Item> ITEMS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.ITEM);
    public static final RegistrationManager<CreativeModeTab> CREATIVE_TABS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.CREATIVE_MODE_TAB);

    // Blocks
    public static final Reference<PhoenixSlotMachineBlock> BLOCK_PHOENIX_SLOT_MACHINE = registerBlock("phoenix_slot_machine", PhoenixSlotMachineBlock::new);
    // Block entities
    public static final Reference<BlockEntityType<PhoenixSlotMachineBlockEntity>> BLOCK_ENTITY_PHOENIX_SLOT_MACHINE = BLOCK_ENTITY_REGISTRY.registerElement("phoenix_slot_machine", () -> new BlockEntityType<>(PhoenixSlotMachineBlockEntity::new, Set.of(BLOCK_PHOENIX_SLOT_MACHINE.get())));
    // Menu
    public static final Reference<MenuType<PhoenixSlotMachineMenu>> MENU_PHOENIX_SLOT_MACHINE = registerMenu("phoenix_slot_machine", PhoenixSlotMachineMenu::new, BlockPos.STREAM_CODEC);
    // Items
    public static final Reference<BlockItem> ITEM_PHOENIX_SLOT_MACHINE = registerBlockItem(BLOCK_PHOENIX_SLOT_MACHINE);
    // Creative tabs
    public static final Reference<CreativeModeTab> PHOENIX_TAB = CREATIVE_TABS_REGISTRY.registerElement("phoenix", id -> PLATFORM.buildCreativeTab(id, ITEM_PHOENIX_SLOT_MACHINE, populator -> {
        populator.apply(ITEM_PHOENIX_SLOT_MACHINE);
    }));

    public static void init() {

    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static <T extends Block> Reference<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> block) {
        return BLOCKS_REGISTRY.registerElement(name, id -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id));
            return block.apply(properties);
        });
    }

    private static <T extends Item> Reference<T> registerItem(String name, Function<Item.Properties, T> item) {
        return ITEMS_REGISTRY.registerElement(name, id -> {
            Item.Properties properties = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id));
            return item.apply(properties);
        });
    }

    private static Reference<BlockItem> registerBlockItem(Reference<? extends Block> blockReference) {
        String path = blockReference.getKey().getPath();
        return registerItem(path, props -> new BlockItem(blockReference.get(), props));
    }

    private static <T extends AbstractContainerMenu, DATA> Reference<MenuType<T>> registerMenu(String name, Platform.MenuFactory<T, DATA> factory, StreamCodec<? super FriendlyByteBuf, DATA> codec) {
        return MENU_REGISTRY.registerElement(name, () -> PLATFORM.createMenuType(factory, codec));
    }
}
