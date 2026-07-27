package dev.tnt.phoenix.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TransactionSource implements StringRepresentable {

    PENDING("pending", Lock.EMPTY),
    SPIN("spin", SpinGame.LOCK),
    RISK_TRANSFER("risk_transfer", AccountBalanceTransaction.LOCK);

    public static final Codec<TransactionSource> CODEC = StringRepresentable.fromEnum(TransactionSource::values);
    private final String serializedName;
    private final Lock lock;

    TransactionSource(String serializedName, Lock lock) {
        this.serializedName = serializedName;
        this.lock = lock;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Lock getLock() {
        return this.lock;
    }
}
