package com.aotaddon.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads the player's nickname from Simple Nicknames via reflection.
 * Falls back to getDisplayName() if the mod isn't loaded or has no nick set.
 */
public final class NicknameHelper {

    private static boolean attempted = false;
    private static Object managerInstance = null;
    private static Method getRawNicknameMethod = null;

    private NicknameHelper() {}

    public static Component getNickname(Player player) {
        String nick = getSimpleNickname(player);
        if (nick != null && !nick.isEmpty()) {
            return Component.literal(nick);
        }
        return player.getDisplayName();
    }

    private static String getSimpleNickname(Player player) {
        if (!attempted) {
            attempted = true;
            try {
                Class<?> mainClass = Class.forName("com.donutello.simplenicknames.SimpleNicknamesMain");
                Field managerField = mainClass.getField("NICKNAME_MANAGER");
                managerInstance = managerField.get(null);
                getRawNicknameMethod = managerInstance.getClass().getMethod("getRawNickname", Player.class);
            } catch (Exception ignored) {
                managerInstance = null;
                getRawNicknameMethod = null;
            }
        }

        if (managerInstance == null || getRawNicknameMethod == null) return null;

        try {
            return (String) getRawNicknameMethod.invoke(managerInstance, player);
        } catch (Exception ignored) {
            return null;
        }
    }
}
