package dev.tnt.phoenix.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Bet implements StringRepresentable {

    X1("x1", 1, 0),
    X2("x2", 2, 10),
    X5("x5", 5, 20),
    X10("x10", 10, 30),
    X25("x25", 25, 40),
    X50("x50", 50, 50),
    X100("x100", 100, 60);

    public static final Codec<Bet> CODEC = StringRepresentable.fromEnum(Bet::values);
    private final String serializedName;
    private final int multiplier;
    private final int additionalTransferDuration;

    Bet(String serializedName, int multiplier, int additionalTransferDuration) {
        this.serializedName = serializedName;
        this.multiplier = multiplier;
        this.additionalTransferDuration = additionalTransferDuration;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public int getValue(int input) {
        return this.multiplier * input;
    }

    public int getBalanceTransferDuration() {
        return 40 + this.additionalTransferDuration;
    }

    public int multiplier() {
        return this.multiplier;
    }
}
