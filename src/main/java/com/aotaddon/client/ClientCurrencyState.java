package com.aotaddon.client;

/**
 * Client-side cached copy of the local player's currency balance (whichever
 * one applies to their faction — Medals or Banknotes). Kept in sync via
 * CurrencySyncPayload, pushed periodically by CurrencySyncTicker so it stays
 * correct even when the balance changes from something other than a titan
 * kill (shop, admin command, etc).
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