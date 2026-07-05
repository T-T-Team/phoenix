package dev.tnt.phoenix.platform;

import dev.tnt.phoenix.platform.init.PlatformMenuProvider;
import dev.tnt.phoenix.platform.init.Reference;
import dev.tnt.phoenix.platform.init.RegistrationManager;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class NeoForgePlatform implements Platform {

    public static final Map<ResourceKey<? extends Registry<?>>, NeoForgeRegistrationManager<?>> REGISTRIES = new HashMap<>();

    @Override
    public <T> RegistrationManager<T> createRegistryManager(Registry<T> registry) {
        NeoForgeRegistrationManager<T> manager = new NeoForgeRegistrationManager<>();
        REGISTRIES.put(registry.key(), manager);
        return manager;
    }

    @Override
    public CreativeModeTab buildCreativeTab(Identifier identifier, Reference<? extends Item> icon, Consumer<TabPopulator> populator) {
        return CreativeModeTab.builder()
                .title(Component.translatable(identifier.toLanguageKey("itemGroup")))
                .icon(() -> icon.get().getDefaultInstance())
                .displayItems((_, out) -> populator.accept(ref -> out.accept(ref.get())))
                .build();
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<? super FriendlyByteBuf, D> dataCodec) {
        return IMenuTypeExtension.create((id, inv, buf) -> {
            D data = dataCodec.decode(buf);
            return factory.createMenu(id, inv, data);
        });
    }

    @Override
    public <T> void openMenu(ServerPlayer player, StreamCodec<? super FriendlyByteBuf, T> codec, PlatformMenuProvider<T> provider) {
        player.openMenu(new SimpleMenuProvider(
                provider::createMenu,
                provider.title()
        ), buf -> {
            T data = provider.getMenuData(player);
            codec.encode(buf, data);
        });
    }

    @Override
    public void sendPacket(ServerPlayer target, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(target, payload);
    }
}
