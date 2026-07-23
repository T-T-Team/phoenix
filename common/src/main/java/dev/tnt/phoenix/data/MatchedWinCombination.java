package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MatchedWinCombination(int matchTypeBonus, WinConfiguration.WinPattern pattern, WinCombination combination) {

    public static final Codec<MatchedWinCombination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("match_bonus").forGetter(MatchedWinCombination::matchTypeBonus),
            WinConfiguration.WinPattern.CODEC.fieldOf("pattern").forGetter(MatchedWinCombination::pattern),
            WinCombination.CODEC.fieldOf("combination").forGetter(MatchedWinCombination::combination)
    ).apply(instance, MatchedWinCombination::new));

    static MatchedWinCombination of(WinConfiguration.MatchType type, WinConfiguration.WinPattern pattern, WinCombination combination) {
        return new MatchedWinCombination(type.ordinal() - 1, pattern, combination);
    }

    public int count() {
        return this.combination.count();
    }

    public int amount() {
        return this.combination.amount();
    }
}
