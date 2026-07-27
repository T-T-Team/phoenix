package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameWinInfo {

    public static final Codec<GameWinInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MatchedWinCombination.CODEC.listOf().optionalFieldOf("combinations", Collections.emptyList()).forGetter(t -> t.combinations),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("animation_index", 0).forGetter(t -> t.animationIndex),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("highlight", 0).forGetter(t -> t.remainingHighlightDuration),
            Codec.BOOL.optionalFieldOf("blink_mode", false).forGetter(t -> t.blinkMode),
            Codec.BOOL.optionalFieldOf("animate_all", false).forGetter(t -> t.animateAll)
    ).apply(instance, GameWinInfo::new));

    private final List<MatchedWinCombination> combinations;
    private int animationIndex;
    private int remainingHighlightDuration;
    private boolean blinkMode;
    private boolean animateAll;

    private HighlightCompleteCallback highlightCompleteCallback;

    public GameWinInfo(List<MatchedWinCombination> combinations, int animationIndex, int remainingHighlightDuration, boolean blinkMode, boolean animateAll) {
        this.combinations = new ArrayList<>(combinations);
        this.animationIndex = animationIndex;
        this.remainingHighlightDuration = remainingHighlightDuration;
        this.blinkMode = blinkMode;
        this.animateAll = animateAll;
    }

    public void setHighlightCompleteCallback(HighlightCompleteCallback highlightCompleteCallback) {
        this.highlightCompleteCallback = highlightCompleteCallback;
    }

    public static GameWinInfo create() {
        return new GameWinInfo(Collections.emptyList(), 0, 0, false, false);
    }

    public void tick(Level level, BlockPos pos) {
        if (this.combinations.isEmpty())
            return;
        if (this.remainingHighlightDuration > 0) {
            if (--this.remainingHighlightDuration <= 0) {
                this.highlightCompleteCallback.onHighlightComplete();
                this.blinkMode = true;
            }
        } else if (!this.blinkMode) {
            if (this.animationIndex < this.combinations.size() - 1) {
                ++this.animationIndex;
                this.resetAnimation();
                this.playHitSound(level, pos);
            } else if (!this.animateAll) {
                this.animateAll = true;
            }
        }
    }

    public void reset() {
        this.blinkMode = false;
        this.animateAll = false;
        this.combinations.clear();
        this.animationIndex = 0;
        this.resetAnimation();
    }

    public boolean isFinalAnimation() {
        return this.animationIndex == this.combinations.size() - 1;
    }

    public void forceLockAnimation() {
        this.blinkMode = false;
        this.animateAll = true;
    }

    public void assignWinCombination(List<MatchedWinCombination> wins) {
        this.combinations.clear();
        this.combinations.addAll(wins);
        this.animationIndex = 0;
        this.resetAnimation();
    }

    public void playHitSound(Level level, BlockPos pos) {
        float pitch = 1.0F + this.animationIndex * 0.05F;
        level.playSound(null, pos, Phoenix.SOUND_HIT.get(), SoundSource.BLOCKS, 1.0F, pitch);
    }

    public void transitionToBlinkMode() {
        this.blinkMode = true;
    }

    public int getAnimationIndex() {
        return animationIndex;
    }

    public void cancelBlinkMode() {
        this.blinkMode = false;
    }

    public boolean isBlinkMode() {
        return this.remainingHighlightDuration <= 0 && this.blinkMode;
    }

    public boolean isAnimateAll() {
        return this.animateAll;
    }

    public boolean isAnimatingWin() {
        return !this.combinations.isEmpty();
    }

    public boolean isBeingAnimated(WinCombination combination) {
        if (this.combinations.isEmpty())
            return false;
        if (this.animateAll) {
            return this.combinations.stream()
                    .anyMatch(c -> c.is(combination));
        } else {
            MatchedWinCombination winCombination = this.combinations.get(this.animationIndex);
            return winCombination.is(combination);
        }
    }

    private void resetAnimation() {
        this.remainingHighlightDuration = 20;
        this.blinkMode = false;
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

    public void updateFrom(GameWinInfo winInfo) {
        this.combinations.clear();
        this.combinations.addAll(winInfo.combinations);
        this.animationIndex = winInfo.animationIndex;
        this.remainingHighlightDuration = winInfo.remainingHighlightDuration;
        this.blinkMode = winInfo.blinkMode;
        this.animateAll = winInfo.animateAll;
    }

    @FunctionalInterface
    public interface HighlightCompleteCallback {
        void onHighlightComplete();
    }
}
