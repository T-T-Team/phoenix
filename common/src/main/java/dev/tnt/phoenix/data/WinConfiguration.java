package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.component.SpinWheel;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record WinConfiguration(List<WinPattern> patterns, List<WinCombination> combinations) {

    public static final Codec<WinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WinPattern.CODEC.listOf().fieldOf("patterns").forGetter(WinConfiguration::patterns),
            WinCombination.CODEC.listOf().fieldOf("combinations").forGetter(WinConfiguration::combinations)
    ).apply(instance, WinConfiguration::new));

    public WinConfiguration(List<WinPattern> patterns, List<WinCombination> combinations) {
        this.patterns = patterns;
        this.combinations = new ArrayList<>(combinations);
    }

    public List<MatchedWinCombination> resolveWins(List<String> wildcardSymbols, List<SpinWheel> spinWheels) {
        List<MatchedWinCombination> wins = new ArrayList<>();
        for (WinPattern pattern : this.patterns) {
            List<MatchedWinCombination> patternWins = new ArrayList<>();
            for (WinCombination combination : this.combinations) {
                MatchType result = pattern.test(combination, spinWheels, wildcardSymbols);
                if (result.isWinningMatch()) {
                    patternWins.add(MatchedWinCombination.of(result, pattern, combination));
                }
            }
            Optional<MatchedWinCombination> patternWin = patternWins.stream()
                    .max(
                            Comparator.comparingInt(MatchedWinCombination::count)
                                    .thenComparingInt(MatchedWinCombination::matchTypeBonus)
                                    .thenComparingInt(MatchedWinCombination::amount)
                    );
            patternWin.ifPresent(wins::add);
        }
        return wins;
    }

    public List<WinCombination> getWinCombinations(boolean special, Comparator<WinCombination> comparator) {
        return this.combinations.stream()
                .filter(c -> c.matchesTag(special))
                .sorted(comparator)
                .toList();
    }

    public static final class WinPattern {

        public static final Codec<WinPattern> CODEC = RecordCodecBuilder.<WinPattern>create(instance -> instance.group(
                Codec.STRING.listOf(3, 3).fieldOf("pattern").forGetter(t -> t.rawPattern),
                Options.CODEC.forGetter(t -> t.options)
        ).apply(instance, WinPattern::new)).validate(WinPattern::checkWinPattern);
        public static final char MISS_CHARACTER = '-';
        public static final char HIT_CHARACTER = 'x';

        private final List<String> rawPattern;
        private final Options options;

        private final List<Index> compiled;

        private WinPattern(List<String> rawPattern, Options options) {
            this.rawPattern = rawPattern;
            this.options = options;
            this.compiled = compilePattern(this.rawPattern);
        }

        public MatchType test(WinCombination combination, List<SpinWheel> spinWheels, List<String> wildcardSymbols) {
            String usedSymbol = null;
            boolean isExactMatch = true;

            for (int i = 0; i < combination.count(); i++) {
                Index index = this.compiled.get(i);
                SpinWheel wheel = spinWheels.get(index.wheelIndex);
                String wheelSymbol = wheel.getSymbolAt(index.positionIndex);

                if (wildcardSymbols.contains(wheelSymbol)) {
                    // check if winning combination is made for wildcards
                    if (!combination.testInput(wheelSymbol)) {
                        // if not, mark as wildcard match only
                        isExactMatch = false;
                    }
                    continue;
                }

                boolean validInputSymbol = combination.testInput(wheelSymbol);
                if (!validInputSymbol) {
                    return MatchType.MISMATCH;
                }
                if (usedSymbol != null && !usedSymbol.equals(wheelSymbol)) {
                    return MatchType.MISMATCH;
                }
                usedSymbol = wheelSymbol;
            }
            return isExactMatch ? MatchType.EXACT : MatchType.WILDCARD;
        }

        public int getWinAmount(int input) {
            return Mth.floor(this.options.winMultiplier * input);
        }

        public boolean indexMatches(Index index) {
            return this.compiled.contains(index);
        }

        public List<Index> getIndexesForWheel(int wheelIndex) {
            return this.compiled.stream()
                    .filter(idx -> idx.wheelIndex == wheelIndex)
                    .toList();
        }

        private static List<Index> compilePattern(List<String> input) {
            List<Index> result = new ArrayList<>();
            int posIndex = 0;
            for (String line : input) {
                for (int i = 0; i < line.length(); i++) {
                    char character = line.charAt(i);
                    if (character == HIT_CHARACTER) {
                        Index index = Index.of(i, posIndex);
                        result.add(index);
                    }
                }
                ++posIndex;
            }
            return result;
        }

        private static DataResult<WinPattern> checkWinPattern(WinPattern input) {
            for (String patternLine : input.rawPattern) {
                for (int i = 0; i < patternLine.length(); i++) {
                    char character = patternLine.charAt(i);
                    if (character != MISS_CHARACTER && character != HIT_CHARACTER) {
                        return DataResult.error(() -> "Pattern line '" + patternLine + "' contains not allowed character: " + character);
                    }
                }
            }
            return DataResult.success(input);
        }

        public record Options(float winMultiplier) {

            public static final Options DEFAULT = new Options(1.0F);
            public static final MapCodec<Options> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0F, 100.0F).optionalFieldOf("win_multiplier", DEFAULT.winMultiplier).forGetter(Options::winMultiplier)
            ).apply(instance, Options::new));
        }
    }

    public enum MatchType {

        MISMATCH,
        WILDCARD,
        EXACT;

        public boolean isWinningMatch() {
            return this != MISMATCH;
        }
    }

    public record Index(int wheelIndex, int positionIndex) {

        public static Index of(int wheel, int pos) {
            return new Index(wheel, pos);
        }
    }
}
