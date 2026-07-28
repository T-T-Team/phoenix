package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record WinCombination(String symbol, int count, int amount, int orderIndex, boolean special) {

    public static final Codec<WinCombination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("symbol").forGetter(WinCombination::symbol),
            ExtraCodecs.POSITIVE_INT.fieldOf("match_count").forGetter(WinCombination::count),
            ExtraCodecs.POSITIVE_INT.fieldOf("win_amount").forGetter(WinCombination::amount),
            Codec.INT.optionalFieldOf("order_index", 0).forGetter(WinCombination::orderIndex),
            Codec.BOOL.optionalFieldOf("special", false).forGetter(WinCombination::special)
    ).apply(instance, WinCombination::new));
    public static final StreamCodec<ByteBuf, WinCombination> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WinCombination::symbol,
            ByteBufCodecs.INT, WinCombination::count,
            ByteBufCodecs.INT, WinCombination::amount,
            ByteBufCodecs.INT, WinCombination::orderIndex,
            ByteBufCodecs.BOOL, WinCombination::special,
            WinCombination::new
    );

    public boolean testInput(String symbol) {
        return this.symbol.equals(symbol);
    }

    public boolean matchesTag(boolean special) {
        return this.special == special;
    }
}
