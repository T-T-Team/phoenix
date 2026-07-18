package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BalanceType implements StringRepresentable {

    INPUT("input"),
    WIN("win"),
    MULTIWIN("multiwin"),;

    public static final Codec<BalanceType> CODEC = StringRepresentable.fromEnum(BalanceType::values);
    private final String serializedName;

    BalanceType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
