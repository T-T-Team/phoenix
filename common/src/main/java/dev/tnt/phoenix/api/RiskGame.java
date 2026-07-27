package dev.tnt.phoenix.api;

public interface RiskGame {

    Lock LOCK = Lock.create(LockReason.RISK);

    boolean isActive();

    boolean isStopped();

    void enable();

    void stop();
}
