package dev.tnt.phoenix.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record PayoutRequestEntry(Identifier payoutId, int quantity) implements Comparable<PayoutRequestEntry> {

    public static final StreamCodec<RegistryFriendlyByteBuf, PayoutRequestEntry> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PayoutRequestEntry::payoutId,
            ByteBufCodecs.INT, PayoutRequestEntry::quantity,
            PayoutRequestEntry::new
    );

    @Override
    public int compareTo(PayoutRequestEntry o) {
        return Integer.compare(this.quantity, o.quantity);
    }
}
