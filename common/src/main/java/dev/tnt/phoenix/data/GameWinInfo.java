package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameWinInfo {

    public static final Codec<GameWinInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MatchedWinCombination.CODEC.listOf().optionalFieldOf("combinations", Collections.emptyList()).forGetter(t -> t.combinations),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("remaining", 0).forGetter(t -> t.remainingAnimations),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("highlight", 0).forGetter(t -> t.remainingHighlightDuration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("blink", 0).forGetter(t -> t.remainingBlinkDuration)
    ).apply(instance, GameWinInfo::new));

    private final List<MatchedWinCombination> combinations;
    private int remainingAnimations;
    private int remainingHighlightDuration;
    private int remainingBlinkDuration;

    private HighlightCompleteCallback highlightCompleteCallback;
    private WinAnimationCompleteCallback animationCompleteCallback;

    public GameWinInfo(List<MatchedWinCombination> combinations, int remainingAnimations, int remainingHighlightDuration, int remainingBlinkDuration) {
        this.combinations = new ArrayList<>(combinations);
        this.remainingAnimations = remainingAnimations;
        this.remainingHighlightDuration = remainingHighlightDuration;
        this.remainingBlinkDuration = remainingBlinkDuration;
    }

    public void setHighlightCompleteCallback(HighlightCompleteCallback highlightCompleteCallback) {
        this.highlightCompleteCallback = highlightCompleteCallback;
    }

    public void setAnimationCompleteCallback(WinAnimationCompleteCallback animationCompleteCallback) {
        this.animationCompleteCallback = animationCompleteCallback;
    }

    public static GameWinInfo create() {
        return new GameWinInfo(Collections.emptyList(), 0, 0, 0);
    }

    public void update(PhoenixSlotMachineBlockEntity slotMachine) {
        if (this.remainingAnimations > 0) {
            if (this.remainingHighlightDuration > 0) {
                if (--this.remainingHighlightDuration <= 0) {
                    Phoenix.LOGGER.debug("Highlighting finished");
                    this.highlightCompleteCallback.onHighlightComplete(slotMachine, this.remainingAnimations);
                }
            } else if (this.remainingBlinkDuration > 0) {
                --this.remainingBlinkDuration;
            } else {
                --this.remainingAnimations;
                if (this.remainingAnimations <= 0) {
                    Phoenix.LOGGER.debug("All animations finished");
                    this.animationCompleteCallback.onAnimationComplete(slotMachine);
                } else {
                    this.resetAnimation();
                }
                Phoenix.LOGGER.debug("Animation finished");
            }
        }
    }

    public void assignWinCombination(List<MatchedWinCombination> wins) {
        this.combinations.clear();
        this.combinations.addAll(wins);
        this.remainingAnimations = wins.size();
        this.resetAnimation();
    }

    private void resetAnimation() {
        this.remainingHighlightDuration = 40;
        this.remainingBlinkDuration = 30;
    }

    public MatchedWinCombination getCurrentWinCombinationForAnimation(int remainingAnims) {
        return this.combinations.get(this.combinations.size() - remainingAnims);
    }

    public void update(GameWinInfo winInfo) {
        this.combinations.clear();
        this.combinations.addAll(winInfo.combinations);
        this.remainingAnimations = winInfo.remainingAnimations;
        this.remainingHighlightDuration = winInfo.remainingHighlightDuration;
        this.remainingBlinkDuration = winInfo.remainingBlinkDuration;
    }

    @FunctionalInterface
    public interface WinAnimationCompleteCallback {
        void onAnimationComplete(PhoenixSlotMachineBlockEntity slotMachine);
    }

    @FunctionalInterface
    public interface HighlightCompleteCallback {
        void onHighlightComplete(PhoenixSlotMachineBlockEntity slotMachine, int remainingAnimations);
    }
}
