package com.aotaddon.client;

/**
 * Client-side cached copy of the local player's Honor Point balance, kept in
 * sync via HonorSyncPayload. Never read persistentData directly client-side
 * for this value — it won't reflect server writes.
 */
public final class ClientHonorData {

    private static double balance = 0.0;

    private ClientHonorData() {}

    public static double getBalance() {
        return balance;
    }

    public static void setBalance(double newBalance) {
        balance = newBalance;
    }
}