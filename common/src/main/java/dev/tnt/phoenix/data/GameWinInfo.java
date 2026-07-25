package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameWinInfo {

    public static final Codec<GameWinInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MatchedWinCombination.CODEC.listOf().optionalFieldOf("combinations", Collections.emptyList()).forGetter(t -> t.combinations),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("animation_index", 0).forGetter(t -> t.animationIndex),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("highlight", 0).forGetter(t -> t.remainingHighlightDuration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("blink", 0).forGetter(t -> t.remainingBlinkDuration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("blink_animation_length", 30).forGetter(t -> t.totalBlinkDuration),
            Codec.BOOL.optionalFieldOf("animate_all", false).forGetter(t -> t.animateAll)
    ).apply(instance, GameWinInfo::new));

    private final List<MatchedWinCombination> combinations;
    private int animationIndex;
    private int remainingHighlightDuration;
    private int remainingBlinkDuration;
    private int totalBlinkDuration;
    private boolean animateAll;

    private HighlightCompleteCallback highlightCompleteCallback;
    private WinAnimationCompleteCallback animationCompleteCallback;

    public GameWinInfo(List<MatchedWinCombination> combinations, int animationIndex, int remainingHighlightDuration, int remainingBlinkDuration, int totalBlinkDuration, boolean animateAll) {
        this.combinations = new ArrayList<>(combinations);
        this.animationIndex = animationIndex;
        this.remainingHighlightDuration = remainingHighlightDuration;
        this.remainingBlinkDuration = remainingBlinkDuration;
        this.totalBlinkDuration = totalBlinkDuration;
        this.animateAll = animateAll;
    }

    public void setHighlightCompleteCallback(HighlightCompleteCallback highlightCompleteCallback) {
        this.highlightCompleteCallback = highlightCompleteCallback;
    }

    public void setAnimationCompleteCallback(WinAnimationCompleteCallback animationCompleteCallback) {
        this.animationCompleteCallback = animationCompleteCallback;
    }

    public static GameWinInfo create() {
        return new GameWinInfo(Collections.emptyList(), 0, 0, 0, 30, false);
    }

    public void update(PhoenixSlotMachineBlockEntity slotMachine) {
        if (this.combinations.isEmpty())
            return;
        if (this.remainingHighlightDuration > 0) {
            if (--this.remainingHighlightDuration <= 0) {
                this.totalBlinkDuration = this.highlightCompleteCallback.onHighlightComplete(slotMachine);
            }
        } else if (this.remainingBlinkDuration > 0) {
            --this.remainingBlinkDuration;
        } else {
            if (this.animationIndex < this.combinations.size() - 1) {
                ++this.animationIndex;
                this.resetAnimation();
            } else if (!this.animateAll) {
                this.animationCompleteCallback.onAnimationComplete(slotMachine);
                this.animateAll = true;
            }
        }
    }

    public void reset() {
        this.animateAll = false;
        this.combinations.clear();
        this.animationIndex = 0;
        this.resetAnimation();
    }

    public void assignWinCombination(List<MatchedWinCombination> wins) {
        this.combinations.clear();
        this.combinations.addAll(wins);
        this.animationIndex = 0;
        this.resetAnimation();
    }

    public boolean isBlinkMode() {
        return this.remainingHighlightDuration <= 0 && this.remainingBlinkDuration > 0;
    }

    public boolean isAnimateAll() {
        return this.animateAll;
    }

    public boolean isAnimatingWin() {
        return !this.combinations.isEmpty();
    }

    private void resetAnimation() {
        this.remainingHighlightDuration = 40;
        this.remainingBlinkDuration = this.totalBlinkDuration;
    }

    public MatchedWinCombination getAnimatedWinCombination() {
        return this.combinations.get(this.animationIndex);
    }

    public boolean isWinningIndex(int wheelIndex, int position) {
        for (MatchedWinCombination combination : this.combinations) {
            WinConfiguration.WinPattern pattern = combination.pattern();
            int index = pattern.indexes().get(wheelIndex);
            if (wheelIndex < combination.count() && index == position) {
                return true;
            }
        }
        return false;
    }

    public void update(GameWinInfo winInfo) {
        this.combinations.clear();
        this.combinations.addAll(winInfo.combinations);
        this.animationIndex = winInfo.animationIndex;
        this.remainingHighlightDuration = winInfo.remainingHighlightDuration;
        this.remainingBlinkDuration = winInfo.remainingBlinkDuration;
        this.totalBlinkDuration = winInfo.totalBlinkDuration;
        this.animateAll = winInfo.animateAll;
    }

    @FunctionalInterface
    public interface WinAnimationCompleteCallback {
        void onAnimationComplete(PhoenixSlotMachineBlockEntity slotMachine);
    }

    @FunctionalInterface
    public interface HighlightCompleteCallback {
        int onHighlightComplete(PhoenixSlotMachineBlockEntity slotMachine);
    }
}
