package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

public record ItemValueDefinition(HolderSet<Item> holderSet, int value, int order) implements Comparable<ItemValueDefinition> {

    public static final Codec<ItemValueDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HolderSetCodec.create(Registries.ITEM, Item.CODEC, false).fieldOf("item").forGetter(ItemValueDefinition::holderSet),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("value").forGetter(ItemValueDefinition::value),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ItemValueDefinition::order)
    ).apply(instance, ItemValueDefinition::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemValueDefinition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.ITEM), ItemValueDefinition::holderSet,
            ByteBufCodecs.INT, ItemValueDefinition::value,
            ByteBufCodecs.INT, ItemValueDefinition::order,
            ItemValueDefinition::new
    );

    public boolean isForItem(Item item) {
        return this.holderSet.contains(item.builtInRegistryHolder());
    }

    @Override
    public int compareTo(@NonNull ItemValueDefinition o) {
        return Integer.compare(this.order, o.order);
    }
}
