package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public record WinCombination(List<String> symbols, int count, int amount, int orderIndex, boolean render) {

    public static final Codec<WinCombination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf(1, Integer.MAX_VALUE).fieldOf("symbols").forGetter(WinCombination::symbols),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(WinCombination::count),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(WinCombination::amount),
            Codec.INT.optionalFieldOf("order_index", 0).forGetter(WinCombination::orderIndex),
            Codec.BOOL.optionalFieldOf("render", true).forGetter(WinCombination::render)
    ).apply(instance, WinCombination::new));
    public static final StreamCodec<ByteBuf, WinCombination> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), WinCombination::symbols,
            ByteBufCodecs.INT, WinCombination::count,
            ByteBufCodecs.INT, WinCombination::amount,
            ByteBufCodecs.INT, WinCombination::orderIndex,
            ByteBufCodecs.BOOL, WinCombination::render,
            WinCombination::new
    );

    public Stream<WinCombination> spread() {
        return this.symbols.stream()
                .map(symbol -> new WinCombination(Collections.singletonList(symbol), this.count, this.amount, this.orderIndex, this.render));
    }

    public String getCombinationSymbol() {
        return this.symbols.getFirst();
    }

    public boolean testInput(String symbol) {
        return this.symbols.contains(symbol);
    }

    public boolean shouldRender(boolean mode) {
        return this.render == mode;
    }
}
