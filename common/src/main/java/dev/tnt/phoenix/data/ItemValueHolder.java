package dev.tnt.phoenix.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.stream.Stream;

public record ItemValueHolder(Item item, int value) implements Comparable<ItemValueHolder> {

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemValueHolder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), ItemValueHolder::item,
            ByteBufCodecs.INT, ItemValueHolder::value,
            ItemValueHolder::new
    );

    public static Stream<ItemValueHolder> unwrap(ItemValueDefinition definition) {
        return definition.holderSet().stream()
                .map(holder -> new ItemValueHolder(holder.value(), definition.value()));
    }

    @Override
    public int compareTo(ItemValueHolder o) {
        return Integer.compare(this.value, o.value);
    }
}
