package dev.tnt.phoenix.api;

import dev.tnt.phoenix.data.GameType;

public interface SpinGame {

    Lock LOCK = Lock.create(LockReason.SPIN);

    Bet bet();

    GameType gameType();

    boolean isRolling();

    int getSpinCost(Bet bet, GameType gameType);

    default int getSpinCost(GameType gameType) {
        return this.getSpinCost(this.bet(), gameType);
    }

    default int getSpinCost() {
        return this.getSpinCost(this.gameType());
    }
}
