package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.game.SpinWheel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.*;

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

    public List<WinCombination> resolveWins(List<String> wildcardSymbols, List<SpinWheel> spinWheels) {
        List<WinCombination> wins = new ArrayList<>();
        for (WinPattern pattern : this.patterns) {
            List<WinCombination> patternWins = new ArrayList<>();
            for (WinCombination combination : this.combinations) {
                if (pattern.matches(combination, spinWheels, wildcardSymbols)) {
                    patternWins.add(combination);
                }
            }
            Optional<WinCombination> patternWin = patternWins.stream()
                    .max(Comparator.comparingInt(WinCombination::count).thenComparingInt(WinCombination::amount));
            patternWin.ifPresent(wins::add);
        }
        return wins;
    }

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

        public boolean matches(WinCombination combination, List<SpinWheel> spinWheels, List<String> wildcardSymbols) {
            String usedSymbol = null;
            for (int i = 0; i < combination.count(); i++) {
                int patternIndex = this.indexes.get(i);
                SpinWheel wheel = spinWheels.get(i);
                String wheelSymbol = wheel.getSymbolAt(patternIndex);
                if (wildcardSymbols.contains(wheelSymbol))
                    continue;
                boolean validInput = combination.testInput(wheelSymbol);
                if (!validInput) {
                    return false;
                }
                if (usedSymbol != null && !usedSymbol.equals(wheelSymbol)) {
                    return false;
                }
                usedSymbol = wheelSymbol;
            }
            return true;
        }
    }
}
