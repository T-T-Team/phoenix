package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AccountBalance {

    public static final Codec<AccountBalance> CODEC = Codec.unboundedMap(AccountType.CODEC, Codec.INT)
            .xmap(AccountBalance::new, holder -> holder.balances);

    private final Map<AccountType, Integer> balances;
    private BalanceChangeListener changeListener;

    public AccountBalance(Map<AccountType, Integer> balances) {
        this.balances = new HashMap<>(balances);
    }

    public static AccountBalance createDefault() {
        return new AccountBalance(Collections.emptyMap());
    }

    public void setChangeListener(BalanceChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void updateFrom(AccountBalance holder) {
        this.balances.putAll(holder.balances);
    }

    public int getBalance(AccountType type) {
        return this.balances.getOrDefault(type, 0);
    }

    public void setBalance(AccountType type, int amount) {
        int balance = Math.max(0, amount);
        int original = this.balances.getOrDefault(type, 0);
        if (original != balance) {
            this.balances.put(type, balance);
            this.changeListener.onBalanceChanged(type, original, balance);
        }
    }

    public void addBalance(AccountType type, int amount) {
        this.setBalance(type, this.getBalance(type) + amount);
    }

    public void multiplyBalance(AccountType type, int amount) {
        this.setBalance(type, this.getBalance(type) * amount);
    }

    public void subtractBalance(AccountType type, int amount) {
        this.setBalance(type, this.getBalance(type) - amount);
    }

    public void transferBalance(AccountType from, AccountType to) {
        this.transferBalance(from, to, Integer.MAX_VALUE);
    }

    public void transferBalance(AccountType from, AccountType to, int limit) {
        int available = Math.min(limit, this.getBalance(from));
        this.subtractBalance(from, available);
        this.addBalance(to, available);
    }

    public void clearBalance(AccountType type) {
        this.setBalance(type, 0);
    }

    public void clearAllBalances() {
        for (AccountType type : AccountType.values()) {
            this.setBalance(type, 0);
        }
    }

    public int getInputBalance() {
        return this.getBalance(AccountType.INPUT);
    }

    public int getWinBalance() {
        return this.getBalance(AccountType.WIN);
    }

    public boolean hasBalanceInAccount(AccountType type) {
        return this.hasBalanceInAccount(type, 1);
    }

    public boolean hasBalanceInAccount(AccountType type, int requestAmount) {
        return this.getBalance(type) >= requestAmount;
    }

    public boolean hasBalanceInEitherAccount(int minBalance, AccountType account, AccountType... otherAccounts) {
        if (this.hasBalanceInAccount(account, minBalance)) {
            return true;
        }
        for (AccountType otherAccount : otherAccounts) {
            if (this.hasBalanceInAccount(otherAccount, minBalance)) {
                return true;
            }
        }
        return false;
    }

    public boolean isZeroBalance() {
        for (AccountType type : AccountType.values()) {
            if (this.getBalance(type) > 0) {
                return false;
            }
        }
        return true;
    }

    public @Nullable Integer getWinBalanceForDisplay() {
        int balance = this.getWinBalance();
        return balance != 0 ? balance : null;
    }

    public int getMultiWinBalance() {
        return this.getBalance(AccountType.MULTIWIN);
    }

    @FunctionalInterface
    public interface BalanceChangeListener {
        void onBalanceChanged(AccountType type, int originalAmount, int newAmount);
    }
}
