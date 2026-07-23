package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.game.SpinWheel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record WinConfigurationConfig(List<String> wildcards, Map<GameType, WinConfiguration> gameConfiguration) {

    public static final Codec<WinConfigurationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("wildcards", Collections.emptyList()).forGetter(WinConfigurationConfig::wildcards),
            Codec.unboundedMap(GameType.CODEC, WinConfiguration.CODEC).fieldOf("game_configurations").forGetter(WinConfigurationConfig::gameConfiguration)
    ).apply(instance, WinConfigurationConfig::new));

    public WinConfiguration getConfigForGame(GameType gameType) {
        return this.gameConfiguration.get(gameType);
    }

    public List<MatchedWinCombination> resolveWins(GameType gameType, List<SpinWheel> spinWheels) {
        WinConfiguration configuration = this.getConfigForGame(gameType);
        return configuration.resolveWins(this.wildcards, spinWheels);
    }
}
