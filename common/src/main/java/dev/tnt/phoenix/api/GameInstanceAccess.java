package dev.tnt.phoenix.api;

import org.jspecify.annotations.Nullable;

public interface GameInstanceAccess {

    String traceId();

    void setChanged();

    void lock(Lock lock);

    void unlock(@Nullable Lock lock);

    boolean isLocked();

    boolean isLockedWithReason(LockReason reason, LockReason... other);

    AccountBalance account();

    SpinGame spin();

    RiskGame risk();

    AccountBalanceTransaction transactions();
}
