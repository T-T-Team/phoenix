package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.config.PhoenixConfig;
import net.minecraft.util.Mth;

import java.util.ArrayList;
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
    private final List<SpinCompleteCallback> completeCallbacks = new ArrayList<>();

    public SpinWheel(List<String> sequence, float spinAmount, int spinTime) {
        this.sequence = sequence;
        this.spinAmount = spinAmount;
        this.spinTime = spinTime;
        this.lastSpinAmount = spinAmount;
    }

    public void update(PhoenixSlotMachineBlockEntity slotMachine, PhoenixConfig.SpinConfiguration configuration) {
        this.lastSpinAmount = this.spinAmount;
        if (this.spinTime > 0) {
            this.spinAmount -= configuration.spinSpeed;
            if (--this.spinTime <= 0) {
                this.normalizeSpinAmount();
                this.completeCallbacks.forEach(callback -> callback.onSpinComplete(slotMachine, this.spinAmount));
            }
        }
    }

    public String getSymbolAt(int position) {
        int startIndex = Mth.floor(this.spinAmount);
        int index = Math.floorMod(startIndex + position, this.sequence.size());
        return this.sequence.get(index);
    }

    public void addSpinCompleteListener(SpinCompleteCallback callback) {
        this.completeCallbacks.add(callback);
    }

    public void startSpinning(int duration) {
        this.spinTime = duration;
    }

    public float getSpinAmount(float delta) {
        return Mth.lerp(delta, this.lastSpinAmount, this.spinAmount);
    }

    public void setSequence(List<String> sequence) {
        this.sequence = sequence;
    }

    public List<String> getSequence() {
        return this.sequence;
    }

    public void updateFrom(SpinWheel other) {
        this.sequence = other.sequence;
        this.spinAmount = other.spinAmount;
        this.spinTime = other.spinTime;
    }

    private void normalizeSpinAmount() {
        this.spinAmount = Math.floorMod(Mth.floor(this.spinAmount), this.sequence.size());
        this.lastSpinAmount = this.spinAmount;
    }

    @FunctionalInterface
    public interface SpinCompleteCallback {
        void onSpinComplete(PhoenixSlotMachineBlockEntity slotMachine, float spinAmount);
    }
}
