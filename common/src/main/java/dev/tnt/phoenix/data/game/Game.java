package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.GameType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Game {

    public static final Codec<Game> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(GameType.CODEC, Codec.STRING.listOf().listOf()).optionalFieldOf("sequences", Collections.emptyMap()).forGetter(t -> t.sequences),
            GameType.CODEC.optionalFieldOf("selected_game", GameType.LOW).forGetter(t -> t.selectedGameType)
    ).apply(instance, Game::new));

    private final Map<GameType, List<List<String>>> sequences;
    private GameType selectedGameType;
    private int spinCount = 0;

    public static Game create() {
        return new Game(Collections.emptyMap(), GameType.LOW);
    }

    private Game(Map<GameType, List<List<String>>> sequences, GameType selectedGameType) {
        this.sequences = new HashMap<>(sequences);
        this.selectedGameType = selectedGameType;
    }

    public Map<GameType, List<List<String>>> getSequences() {
        return sequences;
    }

    public GameType getSelectedGameType() {
        return selectedGameType;
    }

    public boolean isPlaying() {
        return spinCount > 0;
    }

    public void updateFrom(Game other) {
        this.sequences.putAll(other.sequences);
        this.selectedGameType = other.selectedGameType;
    }
}
