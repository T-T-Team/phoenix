package dev.tnt.phoenix.api;

public interface AccountBalance {

    int getBalance(AccountType accountType);

    void addBalance(AccountType accountType, int amount);

    void subtractBalance(AccountType accountType, int amount);

    void clearBalance(AccountType accountType);

    default boolean hasBalanceInAccount(AccountType accountType, int requiredBalance) {
        return this.getBalance(accountType) >= requiredBalance;
    }

    default boolean hasBalanceInAccount(AccountType accountType) {
        return this.hasBalanceInAccount(accountType, 1);
    }
}
