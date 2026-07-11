package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class DataInstanceHolder {

    public static final Codec<DataInstanceHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("value", 0).forGetter(DataInstanceHolder::getValue)
    ).apply(instance, DataInstanceHolder::new));

    private int value;

    private DataInstanceHolder(int value) {
        this.value = value;
    }

    public static DataInstanceHolder createDefault() {
        return new DataInstanceHolder(
                0
        );
    }

    public void addValue(int value) {
        this.value += value;
    }

    public int getValue() {
        return value;
    }

    public boolean canPlay() {
        return true; // TODO implement
    }

    public void play() {

    }

    public DataInstanceHolder update(DataInstanceHolder holder) {
        this.value = holder.value;
        return this;
    }
}
