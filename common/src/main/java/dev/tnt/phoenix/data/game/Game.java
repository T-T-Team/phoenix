package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.util.EnumHelper;

import java.util.ArrayList;
import java.util.List;

public final class Game {

    public static final Codec<Game> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GameType.CODEC.optionalFieldOf("selected_game", GameType.LOW).forGetter(t -> t.selectedGameType),
            Codec.BOOL.optionalFieldOf("risk_hearts", false).forGetter(t -> t.riskHearts),
            Codec.INT.optionalFieldOf("risk_duration", 0).forGetter(t -> t.riskPlayDuration),
            Codec.INT.optionalFieldOf("current_risk", 0).forGetter(t -> t.currentRiskValue)
    ).apply(instance, Game::new));

    private GameType selectedGameType;
    private boolean riskHearts;
    private int riskPlayDuration;
    private int currentRiskValue;

    private final List<RiskCompleteCallback> completeCallbacks = new ArrayList<>();

    public static Game create() {
        return new Game(GameType.LOW, false, 0, 0);
    }

    private Game(GameType selectedGameType, boolean riskHearts, int riskPlayDuration, int currentRiskValue) {
        this.selectedGameType = selectedGameType;
        this.riskHearts = riskHearts;
        this.riskPlayDuration = riskPlayDuration;
        this.currentRiskValue = currentRiskValue;
    }

    public void addRiskCompleteListener(RiskCompleteCallback callback) {
        this.completeCallbacks.add(callback);
    }

    public void changeGameType() {
        this.selectedGameType = EnumHelper.next(this.selectedGameType);
    }

    public void startRisk(int duration, boolean riskHearts) {
        this.riskHearts = riskHearts;
        this.riskPlayDuration = duration;
    }

    public void update(PhoenixSlotMachineBlockEntity slotMachine) {
        if (this.riskPlayDuration > 0) {
            ++this.currentRiskValue;
            if (--this.riskPlayDuration <= 0) {
                boolean won = this.riskHearts == this.isHearts();
                Phoenix.LOGGER.debug("Risk game finished with result: {}", won);
                this.completeCallbacks.forEach(callback -> callback.onRiskComplete(slotMachine, won));
            }
        }
    }

    public boolean isHearts() {
        return this.currentRiskValue % 10 < 5;
    }

    public GameType getSelectedGameType() {
        return selectedGameType;
    }

    public void updateFrom(Game other) {
        this.selectedGameType = other.selectedGameType;
        this.riskHearts = other.riskHearts;
        this.riskPlayDuration = other.riskPlayDuration;
        this.currentRiskValue = other.currentRiskValue;
    }

    public interface RiskCompleteCallback {
        void onRiskComplete(PhoenixSlotMachineBlockEntity slotMachine, boolean won);
    }
}
