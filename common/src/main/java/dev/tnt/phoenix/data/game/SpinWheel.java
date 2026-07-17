package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.List;

public final class SpinWheel {

    public static final Codec<SpinWheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("sequence").forGetter(t -> t.sequence),
            Codec.FLOAT.fieldOf("spin_amount").forGetter(t -> t.spinAmount),
            Codec.INT.fieldOf("spin_time").forGetter(t -> t.spinTime)
    ).apply(instance, SpinWheel::new));

    private List<String> sequence;
    private float spinAmount;
    private float lastSpinAmount;
    private int spinTime;

    public SpinWheel(List<String> sequence, float spinAmount, int spinTime) {
        this.sequence = sequence;
        this.spinAmount = spinAmount;
        this.spinTime = spinTime;
    }

    public void update() {
        this.lastSpinAmount = this.spinAmount;
        if (this.spinTime > 0) {
            this.spinAmount += 0.8F;
            --this.spinTime;
        } else {
            this.spinAmount = Mth.floor(this.spinAmount);
        }
        if (this.spinAmount > this.sequence.size()) {
            this.spinAmount = 0.0F;
        }
    }

    public void startSpinning(int duration) {
        this.spinTime = duration;
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
    }
}
