package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.game.SpinWheel;
import org.jspecify.annotations.Nullable;

import java.util.*;

public record WinConfigurationConfig(List<String> wildcards, Map<GameType, WinConfiguration> gameConfiguration) {

    public static final Codec<WinConfigurationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("wildcards", Collections.emptyList()).forGetter(WinConfigurationConfig::wildcards),
            Codec.unboundedMap(GameType.CODEC, WinConfiguration.CODEC).fieldOf("game_configurations").forGetter(WinConfigurationConfig::gameConfiguration)
    ).apply(instance, WinConfigurationConfig::new));

    public WinConfiguration getConfigForGame(GameType gameType) {
        return this.gameConfiguration.get(gameType);
    }

    public Optional<WinCombination> resolveWin(GameType gameType, List<SpinWheel> spinWheels) {
        WinConfiguration configuration = this.getConfigForGame(gameType);
        List<WinCombination> wins = configuration.resolveWins(this.wildcards, spinWheels);
        return wins.stream()
                .max(
                        Comparator.comparingInt(WinCombination::count)
                                .thenComparingInt(WinCombination::amount)
                );
    }
}
