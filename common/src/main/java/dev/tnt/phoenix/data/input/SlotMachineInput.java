package dev.tnt.phoenix.data.input;

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

public record SlotMachineInput(HolderSet<Item> holderSet, int value, int order) implements Comparable<SlotMachineInput> {

    public static final Codec<SlotMachineInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HolderSetCodec.create(Registries.ITEM, Item.CODEC, false).fieldOf("item").forGetter(SlotMachineInput::holderSet),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("value").forGetter(SlotMachineInput::value),
            Codec.INT.optionalFieldOf("order", 0).forGetter(SlotMachineInput::order)
    ).apply(instance, SlotMachineInput::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotMachineInput> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.ITEM), SlotMachineInput::holderSet,
            ByteBufCodecs.INT, SlotMachineInput::value,
            ByteBufCodecs.INT, SlotMachineInput::order,
            SlotMachineInput::new
    );

    public boolean isForItem(Item item) {
        return this.holderSet.contains(item.builtInRegistryHolder());
    }

    @Override
    public int compareTo(@NonNull SlotMachineInput o) {
        return Integer.compare(this.order, o.order);
    }
}
