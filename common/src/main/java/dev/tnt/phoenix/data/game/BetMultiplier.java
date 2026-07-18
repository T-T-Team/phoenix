package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BetMultiplier implements StringRepresentable {

    X1("x1", 1),
    X2("x2", 2),
    X5("x5", 5),
    X10("x10", 10),
    X25("x25", 25),
    X50("x50", 50),
    X100("x100", 100);

    public static final Codec<BetMultiplier> CODEC = StringRepresentable.fromEnum(BetMultiplier::values);
    private final String serializedName;
    private final int multiplier;

    BetMultiplier(String serializedName, int multiplier) {
        this.serializedName = serializedName;
        this.multiplier = multiplier;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public int getValue(int input) {
        return this.multiplier * input;
    }

    int getMultiplier() {
        return multiplier;
    }
}
