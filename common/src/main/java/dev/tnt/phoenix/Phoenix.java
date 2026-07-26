package dev.tnt.phoenix;

import com.mojang.logging.LogUtils;
import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.config.PhoenixConfig;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import dev.tnt.phoenix.data.input.SlotMachineInput;
import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.platform.Platform;
import dev.tnt.phoenix.platform.Services;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import dev.toma.configuration.Configuration;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class Phoenix {

    public static final String MOD_ID = "phoenix";
    public static final Logger LOGGER = LogManager.getLogger("Phoenix");
    public static final org.slf4j.Logger LOGGER_SLF4J = LogUtils.getLogger();
    public static final Marker NETWORK_MARKER = MarkerManager.getMarker("Network");
    public static final Marker DATA_MARKER = MarkerManager.getMarker("Data");
    public static final Platform PLATFORM = Services.load(Platform.class);
    public static final PhoenixConfig CONFIG = Configuration.registerSimpleJsonConfig(PhoenixConfig.class);

    // Data managers
    public static final SlotMachineDataManager SLOT_MACHINES = new SlotMachineDataManager();

    // Slot machine configs
    public static final Identifier SLOT_MACHINE_CONFIG_PHOENIX = identifier("phoenix");

    // Registries
    public static final RegistrationManager<Block> BLOCKS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.BLOCK);
    public static final RegistrationManager<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final RegistrationManager<Item> ITEMS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.ITEM);
    public static final RegistrationManager<SoundEvent> SOUND_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.SOUND_EVENT);
    public static final RegistrationManager<CreativeModeTab> CREATIVE_TABS_REGISTRY = PLATFORM.createRegistryManager(BuiltInRegistries.CREATIVE_MODE_TAB);

    // Blocks
    public static final Reference<PhoenixSlotMachineBlock> BLOCK_PHOENIX_SLOT_MACHINE = registerBlock("phoenix", PhoenixSlotMachineBlock::new);
    // Block entities
    public static final Reference<BlockEntityType<PhoenixSlotMachineBlockEntity>> BLOCK_ENTITY_PHOENIX_SLOT_MACHINE = BLOCK_ENTITY_REGISTRY.registerElement("phoenix", () -> new BlockEntityType<>(PhoenixSlotMachineBlockEntity::new, Set.of(BLOCK_PHOENIX_SLOT_MACHINE.get())));
    // Items
    public static final Reference<BlockItem> ITEM_PHOENIX_SLOT_MACHINE = registerBlockItem(BLOCK_PHOENIX_SLOT_MACHINE);
    // Sounds
    public static final Reference<SoundEvent> BET = registerVariableRangeSound("bet");
    public static final Reference<SoundEvent> COUNT = registerVariableRangeSound("count");
    public static final Reference<SoundEvent> GAMBLE = registerVariableRangeSound("gamble");
    public static final Reference<SoundEvent> GAMBLE_LOSE = registerVariableRangeSound("gamble_lose");
    public static final Reference<SoundEvent> GAMBLE_WIN = registerVariableRangeSound("gamble_win");
    public static final Reference<SoundEvent> HIT = registerVariableRangeSound("hit");
    public static final Reference<SoundEvent> HOLD = registerVariableRangeSound("hold");
    public static final Reference<SoundEvent> SLOT = registerVariableRangeSound("slot");
    public static final Reference<SoundEvent> START = registerVariableRangeSound("start");
    public static final Reference<SoundEvent> WIN = registerVariableRangeSound("win");
    // Creative tabs
    public static final Reference<CreativeModeTab> PHOENIX_TAB = CREATIVE_TABS_REGISTRY.registerElement("phoenix", id -> PLATFORM.buildCreativeTab(id, ITEM_PHOENIX_SLOT_MACHINE, populator -> {
        populator.apply(ITEM_PHOENIX_SLOT_MACHINE);
    }));

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String getTraceId(Vec3i pos, UUID uid) {
        return String.format(Locale.ROOT, "%s@[%d %d %d]", uid, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void modifyTooltip(ItemStack itemStack, Consumer<Component> tooltipAdder) {
        SlotMachineInputApi api = PLATFORM.getSlotMachineInputs();
        int stackValue = api.getItemValue(itemStack, true);
        int value = api.getItemValue(itemStack, false);
        if (value > 0) {
            Component component = value != stackValue
                    ? SlotMachineInput.getStackDisplayLabel(value, stackValue)
                    : SlotMachineInput.getDisplayLabel(value);
            tooltipAdder.accept(component);
        }
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

    private static Reference<SoundEvent> registerVariableRangeSound(String name) {
        return SOUND_REGISTRY.registerElement(name, SoundEvent::createVariableRangeEvent);
    }

    private static Reference<SoundEvent> registerFixedRangeSound(String name, float range) {
        return SOUND_REGISTRY.registerElement(name, id -> SoundEvent.createFixedRangeEvent(id, range));
    }

    private static Reference<BlockItem> registerBlockItem(Reference<? extends Block> blockReference) {
        String path = blockReference.getKey().getPath();
        return registerItem(path, props -> new BlockItem(blockReference.get(), props));
    }
}
