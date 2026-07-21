package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AccountBalance {

    public static final Codec<AccountBalance> CODEC = Codec.unboundedMap(BalanceType.CODEC, Codec.INT)
            .xmap(AccountBalance::new, holder -> holder.balances);

    private final Map<BalanceType, Integer> balances;

    public AccountBalance(Map<BalanceType, Integer> balances) {
        this.balances = new HashMap<>(balances);
    }

    public static AccountBalance createDefault() {
        return new AccountBalance(Collections.emptyMap());
    }

    public void updateFrom(AccountBalance holder) {
        this.balances.putAll(holder.balances);
    }

    public int getBalance(BalanceType type) {
        return this.balances.getOrDefault(type, 0);
    }

    public void setBalance(BalanceType type, int amount) {
        this.balances.put(type, Math.max(0, amount));
    }

    public void addBalance(BalanceType type, int amount) {
        this.setBalance(type, this.getBalance(type) + amount);
    }

    public void multiplyBalance(BalanceType type, int amount) {
        this.setBalance(type, this.getBalance(type) * amount);
    }

    public void subtractBalance(BalanceType type, int amount) {
        this.setBalance(type, this.getBalance(type) - amount);
    }

    public void transferBalance(BalanceType from, BalanceType to) {
        this.transferBalance(from, to, Integer.MAX_VALUE);
    }

    public void transferBalance(BalanceType from, BalanceType to, int limit) {
        int available = Math.min(limit, this.getBalance(from));
        this.subtractBalance(from, available);
        this.addBalance(to, available);
    }

    public void clearBalance(BalanceType type) {
        this.setBalance(type, 0);
    }

    public void clearAllBalances() {
        for (BalanceType type : BalanceType.values()) {
            this.setBalance(type, 0);
        }
    }

    public int getInputBalance() {
        return this.getBalance(BalanceType.INPUT);
    }

    public int getWinBalance() {
        return this.getBalance(BalanceType.WIN);
    }

    public boolean hasSufficientBalance(BalanceType type, int requestAmount) {
        return this.getBalance(type) >= requestAmount;
    }

    public @Nullable Integer getWinBalanceForDisplay() {
        int balance = this.getWinBalance();
        return balance != 0 ? balance : null;
    }

    public int getMultiWinBalance() {
        return this.getBalance(BalanceType.MULTIWIN);
    }
}
