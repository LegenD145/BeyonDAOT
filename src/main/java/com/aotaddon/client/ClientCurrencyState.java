package com.aotaddon.client;

/**
 * Client-side cached copy of the local player's currency balance (medals or
 * banknotes, matching their DAOT bloodline). Kept in sync via CurrencySyncPayload.
 */
public final class ClientCurrencyState {

    private static int balance = 0;

    private ClientCurrencyState() {}

    public static int getBalance() {
        return balance;
    }

    public static void setBalance(int newBalance) {
        balance = newBalance;
    }
}