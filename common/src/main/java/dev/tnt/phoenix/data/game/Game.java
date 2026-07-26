package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.util.EnumHelper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Game {

    public static final Codec<Game> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GameType.CODEC.optionalFieldOf("selected_game", GameType.LOW).forGetter(t -> t.selectedGameType),
            Codec.BOOL.optionalFieldOf("risk_hearts", false).forGetter(t -> t.riskHearts),
            Codec.INT.optionalFieldOf("risk_stop_delay", 0).forGetter(t -> t.riskStopDelay),
            Codec.INT.optionalFieldOf("current_risk", 0).forGetter(t -> t.currentRiskValue),
            Codec.BOOL.optionalFieldOf("played", false).forGetter(t -> t.played),
            Codec.INT.listOf().optionalFieldOf("held_slots", Collections.emptyList()).forGetter(t -> new ArrayList<>(t.hold)),
            Codec.BOOL.optionalFieldOf("risk_active", false).forGetter(t -> t.riskActive),
            Codec.INT.optionalFieldOf("risk_freeze_duration", 0).forGetter(t -> t.riskFreezeDuration)
    ).apply(instance, Game::new));

    private GameType selectedGameType;
    private boolean riskHearts;
    private int riskStopDelay;
    private int currentRiskValue;
    private boolean played;
    private final IntSet hold;
    private boolean riskActive;
    private int riskFreezeDuration;

    private RiskCompleteCallback riskCompleteCallback;
    private RiskUnfreezeCallback riskUnfreezeCallback;

    public static Game create() {
        return new Game(GameType.LOW, false, 0, 0, false, Collections.emptyList(), false, 0);
    }

    private Game(GameType selectedGameType, boolean riskHearts, int riskStopDelay, int currentRiskValue, boolean played, List<Integer> hold, boolean riskActive, int riskFreezeDuration) {
        this.selectedGameType = selectedGameType;
        this.riskHearts = riskHearts;
        this.riskStopDelay = riskStopDelay;
        this.currentRiskValue = currentRiskValue;
        this.played = played;
        this.hold = new IntOpenHashSet(hold);
        this.riskActive = riskActive;
        this.riskFreezeDuration = riskFreezeDuration;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public boolean hasPlayed() {
        return this.played;
    }

    public void hold(int slot) {
        this.hold.add(slot);
    }

    public boolean isHeld(int slot) {
        return this.hold.contains(slot);
    }

    public int getHeldCount() {
        return this.hold.size();
    }

    public void clearHold() {
        this.hold.clear();
    }

    public void setRiskCompleteCallback(RiskCompleteCallback riskCompleteCallback) {
        this.riskCompleteCallback = riskCompleteCallback;
    }

    public void setRiskUnfreezeCallback(RiskUnfreezeCallback riskUnfreezeCallback) {
        this.riskUnfreezeCallback = riskUnfreezeCallback;
    }

    public void changeGameType() {
        this.selectedGameType = EnumHelper.next(this.selectedGameType);
    }

    public void startRiskBet(int riskStopDelay, boolean riskHearts) {
        this.riskStopDelay = riskStopDelay;
        this.riskHearts = riskHearts;
    }

    public boolean isRiskActive() {
        return riskActive;
    }

    public void enableRisk() {
        this.riskActive = true;
    }

    public void cancelRisk() {
        this.riskActive = false;
    }

    public boolean isFrozen() {
        return this.riskFreezeDuration > 0;
    }

    public void update(PhoenixSlotMachineBlockEntity slotMachine) {
        if (this.riskFreezeDuration > 0) {
            if (--this.riskFreezeDuration <= 0) {
                this.riskUnfreezeCallback.onRiskUnfreeze(slotMachine);
            }
            return;
        }
        if (this.riskActive) {
            ++this.currentRiskValue;
            if (this.riskStopDelay > 0 && --this.riskStopDelay <= 0) {
                this.riskFreezeDuration = 40;
                boolean won = this.riskHearts == this.isHearts();
                this.riskCompleteCallback.onRiskComplete(slotMachine, won);
            }
        }
    }

    public boolean isHearts() {
        int cycle = Phoenix.CONFIG.riskCycleDuration;
        return this.currentRiskValue % (2 * cycle) < cycle;
    }

    public GameType getSelectedGameType() {
        return selectedGameType;
    }

    public void updateFrom(Game other) {
        this.selectedGameType = other.selectedGameType;
        this.riskHearts = other.riskHearts;
        this.riskStopDelay = other.riskStopDelay;
        this.currentRiskValue = other.currentRiskValue;
        this.played = other.played;
        this.hold.clear();
        this.hold.addAll(other.hold);
        this.riskActive = other.riskActive;
        this.riskFreezeDuration = other.riskFreezeDuration;
    }

    @FunctionalInterface
    public interface RiskCompleteCallback {
        void onRiskComplete(PhoenixSlotMachineBlockEntity slotMachine, boolean won);
    }

    @FunctionalInterface
    public interface RiskUnfreezeCallback {
        void onRiskUnfreeze(PhoenixSlotMachineBlockEntity slotMachine);
    }
}
