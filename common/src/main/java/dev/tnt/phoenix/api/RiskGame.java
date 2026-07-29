package dev.tnt.phoenix.api;

public interface RiskGame {

    Lock LOCK = Lock.create(LockReason.RISK);
    Lock LOCK_PENDING = Lock.create(LockReason.RISK_PENDING);

    boolean isActive();

    boolean isStopped();

    void enable();

    void stop();
}
