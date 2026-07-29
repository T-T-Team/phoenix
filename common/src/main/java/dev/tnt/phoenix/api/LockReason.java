package dev.tnt.phoenix.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum LockReason implements StringRepresentable {

    NONE("none"),
    SPIN("spin"),
    RISK("risk"),
    RISK_PENDING("risk_pending"),
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
        return this.is(SPIN, RISK, TRANSFER);
    }

    public boolean is(LockReason reason, LockReason... otherReasons) {
        if (this == reason) {
            return true;
        }
        for (LockReason otherReason : otherReasons) {
            if (this == otherReason) {
                return true;
            }
        }
        return false;
    }
}
