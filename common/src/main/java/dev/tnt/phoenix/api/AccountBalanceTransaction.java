package dev.tnt.phoenix.api;

import java.util.Optional;

public interface AccountBalanceTransaction {

    Lock LOCK = Lock.create(LockReason.TRANSFER);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void initiate(TransactionSource source, Optional<AccountType> sourceAccount, AccountType destAccount, int transactionVolume);

    default void initiate(TransactionSource source, AccountType sourceAccount, AccountType destAccount, int transactionVolume) {
        this.initiate(source, Optional.of(sourceAccount), destAccount, transactionVolume);
    }

    default void initiate(TransactionSource source, AccountType destAccount, int transactionVolume) {
        this.initiate(source, Optional.empty(), destAccount, transactionVolume);
    }

}
