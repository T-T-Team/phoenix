package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FabricPlatform implements Platform {

    private static final List<FabricRegistrationManager<?>> REGISTRIES = new ArrayList<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        FabricRegistrationManager<T> manager = new FabricRegistrationManager<>(registry);
        REGISTRIES.add(manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return FabricCreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<? super FriendlyByteBuf, D> dataCodec) {
        return new ExtendedMenuType<>(factory::createMenu, dataCodec);
    }

    @Override
    public <T> void openMenu(ServerPlayer player, StreamCodec<? super FriendlyByteBuf, T> codec, PlatformMenuProvider<T> provider) {
        player.openMenu(new ExtendedMenuProvider<>() {
            @Override
            public Object getScreenOpeningData(ServerPlayer player) {
                return provider.getMenuData(player);
            }

            @Override
            public Component getDisplayName() {
                return provider.getTitle();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return provider.createMenu(containerId, inventory, player);
            }
        });
    }

    public static void bindRegistries() {
        REGISTRIES.forEach(FabricRegistrationManager::bind);
    }
}
