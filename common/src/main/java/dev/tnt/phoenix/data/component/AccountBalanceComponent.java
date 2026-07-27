package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.AccountBalance;
import dev.tnt.phoenix.api.AccountType;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AccountBalanceComponent extends PhoenixComponent implements AccountBalance {

    public static final Marker MARKER = MarkerManager.getMarker("Account");

    public static final Codec<AccountBalanceComponent> CODEC = Codec.unboundedMap(AccountType.CODEC, Codec.INT)
            .xmap(AccountBalanceComponent::new, holder -> holder.balances);

    private final Map<AccountType, Integer> balances;

    public AccountBalanceComponent(Map<AccountType, Integer> balances) {
        this.balances = new HashMap<>(balances);
    }

    public static AccountBalanceComponent createDefault() {
        return new AccountBalanceComponent(Collections.emptyMap());
    }

    public void updateFrom(AccountBalanceComponent holder) {
        this.balances.putAll(holder.balances);
    }

    @Override
    public int getBalance(AccountType type) {
        return this.balances.getOrDefault(type, 0);
    }

    @Override
    public void addBalance(AccountType type, int amount) {
        this.setBalance(type, this.getBalance(type) + amount);
    }

    @Override
    public void subtractBalance(AccountType type, int amount) {
        this.setBalance(type, this.getBalance(type) - amount);
    }

    @Override
    public void clearBalance(AccountType type) {
        this.setBalance(type, 0);
    }

    public void setBalance(AccountType type, int amount) {
        int balance = Math.max(0, amount);
        int original = this.balances.getOrDefault(type, 0);
        if (original != balance) {
            this.balances.put(type, balance);

            int diff = amount - original;
            String diffLabel = (diff > 0 ? "+" : "") + diff;
            this.instanceAccess.setChanged();
            Phoenix.LOGGER.debug(MARKER, "[{}] Balance changed in account {}: {} -> {} [{}]", this.instanceAccess.traceId(), type, original, amount, diffLabel);
        }
    }

    public void transferBalance(AccountType from, AccountType to, int limit) {
        int available = Math.min(limit, this.getBalance(from));
        this.subtractBalance(from, available);
        this.addBalance(to, available);
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

    public @Nullable Integer getWinBalanceForDisplay() {
        int balance = this.getBalance(AccountType.WIN);
        return balance != 0 ? balance : null;
    }
}
