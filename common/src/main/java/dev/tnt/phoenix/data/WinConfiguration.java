package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.Comparator;
import java.util.List;

public record WinConfiguration(List<WinPattern> patterns, List<WinCombination> combinations) {

    public static final Codec<WinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WinPattern.CODEC.listOf().fieldOf("patterns").forGetter(WinConfiguration::patterns),
            WinCombination.CODEC.listOf().fieldOf("combinations").forGetter(WinConfiguration::combinations)
    ).apply(instance, WinConfiguration::new));
    public static final StreamCodec<ByteBuf, WinConfiguration> STREAM_CODEC = StreamCodec.composite(
            WinPattern.STREAM_CODEC.apply(ByteBufCodecs.list()), WinConfiguration::patterns,
            WinCombination.STREAM_CODEC.apply(ByteBufCodecs.list()), WinConfiguration::combinations,
            WinConfiguration::new
    );

    public List<WinCombination> getDisplayableCombinations(boolean special) {
        return this.getDisplayableCombinations(special, Comparator.comparingInt(WinCombination::amount));
    }

    public List<WinCombination> getDisplayableCombinations(boolean special, Comparator<WinCombination> comparator) {
        return this.combinations.stream()
                .filter(c -> c.shouldRender(special))
                .sorted(comparator)
                .flatMap(WinCombination::spread)
                .toList();
    }

    public record WinPattern(List<Integer> indexes) {

        public static final Codec<WinPattern> CODEC = ExtraCodecs.NON_NEGATIVE_INT.listOf()
                .xmap(WinPattern::new, WinPattern::indexes);
        public static final StreamCodec<ByteBuf, WinPattern> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.apply(ByteBufCodecs.list()), WinPattern::indexes,
                WinPattern::new
        );

        public float getLeftHeight() {
            int left = this.indexes.getFirst();
            return left / 2.0F;
        }

        public float getRightHeight() {
            int right = this.indexes.getLast();
            return right / 2.0F;
        }
    }
}
