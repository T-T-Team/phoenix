package dev.tnt.phoenix.api;

public interface GameInstanceAccess {

    String traceId();

    void setChanged();

    void lock(Lock lock);

    void unlock(Lock lock);

    boolean isLocked();

    boolean isLockedWithReason(LockReason reason, LockReason... other);

    AccountBalance account();

    SpinGame spin();

    RiskGame risk();

    AccountBalanceTransaction transactions();
}
