package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.config.PhoenixConfig;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.SlotMachineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SpinWheel {

    public static final Codec<SpinWheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("sequence").forGetter(t -> t.sequence),
            Codec.FLOAT.fieldOf("spin_amount").forGetter(t -> t.spinAmount),
            Codec.INT.fieldOf("spin_time").forGetter(t -> t.spinTime),
            Codec.FLOAT.fieldOf("speed").forGetter(t -> t.speed)
    ).apply(instance, SpinWheel::new));

    private List<String> sequence;
    private float spinAmount;
    private float lastSpinAmount;
    private int spinTime;
    private float speed;

    private SpinGameComponent.SpinFinishCallback finishCallback;

    public SpinWheel(List<String> sequence, float spinAmount, int spinTime, float speed) {
        this.sequence = sequence;
        this.spinAmount = spinAmount;
        this.spinTime = spinTime;
        this.lastSpinAmount = spinAmount;
        this.speed = speed;
    }

    public void update(Level level, BlockPos pos) {
        this.lastSpinAmount = this.spinAmount;
        if (this.spinTime > 0) {
            this.spinAmount -= this.speed;
            if (--this.spinTime <= 0) {
                this.normalizeSpinAmount();
                this.finishCallback.onSpinFinished(level, pos);
            }
        }
    }

    public void reloadSequence(RandomSource random, SlotMachineConfig config, GameType gameType, int index) {
        this.sequence = config.generateSequence(random, gameType, index);
    }

    public String getSymbolAt(int position) {
        int startIndex = Mth.floor(this.spinAmount);
        int index = Math.floorMod(startIndex + position, this.sequence.size());
        return this.sequence.get(index);
    }

    public void setFinishCallback(SpinGameComponent.SpinFinishCallback finishCallback) {
        this.finishCallback = finishCallback;
    }

    public void startSpinning(int duration, float speed) {
        this.spinTime = duration;
        this.speed = speed;
    }

    public float getSpinAmount(float delta) {
        return Mth.lerp(delta, this.lastSpinAmount, this.spinAmount);
    }

    public List<String> getSequence() {
        return this.sequence;
    }

    public void updateFrom(SpinWheel other) {
        this.sequence = other.sequence;
        this.spinAmount = other.spinAmount;
        this.spinTime = other.spinTime;
        this.speed = other.speed;
    }

    private void normalizeSpinAmount() {
        this.spinAmount = Math.floorMod(Mth.floor(this.spinAmount), this.sequence.size());
        this.lastSpinAmount = this.spinAmount;
    }
}
