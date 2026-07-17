package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class AccountBalance {

    public static final Codec<AccountBalance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("balance").forGetter(AccountBalance::getBalance),
            Codec.INT.fieldOf("win_balance").forGetter(AccountBalance::getWinBalance),
            Codec.INT.fieldOf("multiwin_balance").forGetter(AccountBalance::getMultiWinBalance)
    ).apply(instance, AccountBalance::new));

    private int balance;
    private int winBalance;
    private int multiWinBalance;

    public AccountBalance(int balance, int winBalance, int multiWinBalance) {
        this.balance = balance;
        this.winBalance = winBalance;
        this.multiWinBalance = multiWinBalance;
    }

    public static AccountBalance createDefault() {
        return new AccountBalance(0, 0, 0);
    }

    public void updateFrom(AccountBalance holder) {
        this.balance = holder.balance;
        this.winBalance = holder.winBalance;
        this.multiWinBalance = holder.multiWinBalance;
    }

    public int getBalance() {
        return this.balance;
    }

    public void subtractBalance(int amount) {
        this.balance -= amount;
    }

    public void addBalance(int amount) {
        this.balance += amount;
    }

    public int getWinBalance() {
        return winBalance;
    }

    public void subtractWinBalance(int amount) {
        this.winBalance -= amount;
    }

    public int getMultiWinBalance() {
        return multiWinBalance;
    }

    public void subtractMultiWinBalance(int amount) {
        this.multiWinBalance -= amount;
    }

    public boolean isZero() {
        return this.balance == 0 && this.winBalance == 0 && this.multiWinBalance == 0;
    }
}
