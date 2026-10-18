package dev.tnt.phoenix.data.payout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record SlotMachinePayout(ResourceLocation payoutId, ItemStack template, int price) implements Comparable<SlotMachinePayout> {

    public static final Codec<SlotMachinePayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("payout_id").forGetter(SlotMachinePayout::payoutId),
            ItemStack.CODEC.fieldOf("item_template").forGetter(SlotMachinePayout::template),
            ExtraCodecs.POSITIVE_INT.fieldOf("payout_price").forGetter(SlotMachinePayout::price)
    ).apply(instance, SlotMachinePayout::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotMachinePayout> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SlotMachinePayout::payoutId,
            ItemStack.STREAM_CODEC, SlotMachinePayout::template,
            ByteBufCodecs.INT, SlotMachinePayout::price,
            SlotMachinePayout::new
    );

    public ItemStack assemble() {
        return this.template.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SlotMachinePayout that)) return false;
        return Objects.equals(payoutId, that.payoutId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(payoutId);
    }

    @Override
    public int compareTo(SlotMachinePayout o) {
        return Integer.compare(this.price, o.price);
    }
}
