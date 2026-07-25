package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum LockReason implements StringRepresentable {

    NONE("none"),
    SPIN("spin"),
    RISK("risk"),
    TRANSFER("transfer");

    public static final Codec<LockReason> CODEC = StringRepresentable.fromEnum(LockReason::values);
    private final String serializedName;

    LockReason(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isActiveGame() {
        return this == SPIN || this == RISK || this == TRANSFER;
    }
}
