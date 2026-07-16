package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public final class SpinWheel {

    public static final Codec<SpinWheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("sequence").forGetter(t -> t.sequence),
            Codec.FLOAT.fieldOf("spin_amount").forGetter(t -> t.spinAmount)
    ).apply(instance, SpinWheel::new));

    private List<String> sequence;
    private float spinAmount;

    public SpinWheel(List<String> sequence, float spinAmount) {
        this.sequence = sequence;
        this.spinAmount = spinAmount;
    }

    public List<String> getSequence() {
        return this.sequence;
    }

    public void updateFrom(SpinWheel other) {
        this.sequence = other.sequence;
        this.spinAmount = other.spinAmount;
    }
}
