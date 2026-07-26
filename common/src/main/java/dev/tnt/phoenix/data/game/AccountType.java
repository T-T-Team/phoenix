package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum AccountType implements StringRepresentable {

    INPUT("input"),
    WIN("win"),
    MULTIWIN("multiwin"),;

    public static final Codec<AccountType> CODEC = StringRepresentable.fromEnum(AccountType::values);
    private final String serializedName;

    AccountType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
