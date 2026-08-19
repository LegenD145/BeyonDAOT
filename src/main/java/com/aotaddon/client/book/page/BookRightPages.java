package com.aotaddon.client.book.page;

import com.aotaddon.client.ClientCardStats;
import com.aotaddon.client.ClientCurrencyState;
import com.aotaddon.client.ClientHonorData;
import com.aotaddon.client.ConsentClientSetup;
import com.aotaddon.client.GrabModeClientSetup;
import com.aotaddon.client.ODMDiagnosticKeybind;
import com.aotaddon.client.ShiftlockClientState;
import com.aotaddon.client.book.BookTab;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * Tab-content layouts drawn on the left page. Stats honor/currency/identity
 * come from client caches synced from the server.
 */
public final class BookRightPages {

    private static final int INK = 0x4A3F2A;
    private static final int INK_MUTED = 0x7A6B55;
    private static final int BAR_BG = 0xFFC4B49A;
    private static final int BAR_FILL = 0xFF4A3F2A;
    private static final int PAD = 22;
    private static final int CONTENT_SHIFT_X = 18;
    private static final int VALUE_GAP = 36;
    private static final int ROW_H = 16;
    private static final int SETTINGS_PAD = 22;
    private static final int SETTINGS_ROW_H = 15;
    /** Tight gap so label + value stay on the left page. */
    private static final int SETTINGS_VALUE_GAP = 6;
    private static final int BAR_H = 8;

    private BookRightPages() {}

    public static void render(GuiGraphics gfx, Font font, BookTab tab,
                              int regionX, int regionY, int regionW, int regionH) {
        int shiftX = tab == BookTab.SETTINGS ? 12 : CONTENT_SHIFT_X;
        int pad = tab == BookTab.SETTINGS ? SETTINGS_PAD : PAD;
        int x = regionX + pad + shiftX;
        int y = regionY + PAD;
        int innerW = regionW - pad * 2 - shiftX;

        switch (tab) {
            case STATS -> renderStats(gfx, font, x, y, innerW);
            case TALENTS -> renderTalents(gfx, font, x, y, innerW);
            case SETTINGS -> renderSettings(gfx, font, x, y, innerW);
            case REPUTATION -> renderReputation(gfx, font, x, y, innerW);
            case UNASSIGNED -> renderUnassigned(gfx, font, regionX, regionY, regionW, regionH);
        }
    }

    private static void renderStats(GuiGraphics gfx, Font font, int x, int y, int innerW) {
        y = drawTitle(gfx, font, x, y, innerW, Component.translatable("titanreqiuem.book.tab.stats"));
        y += 8;
        y = drawRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.stats.honor"),
                Component.literal(formatStatNumber(ClientHonorData.getBalance())));
        y = drawRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.stats.currency"),
                Component.literal(String.valueOf(ClientCurrencyState.getBalance())));
        y = drawRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.stats.family"),
                labeledOrDash(ClientCardStats.getFamily()));
        y = drawRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.stats.bloodline"),
                labeledOrDash(ClientCardStats.getBloodline()));
        drawRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.stats.combat_xp"),
                Component.literal(formatStatNumber(ClientCardStats.getCombatXp())));
    }

    private static Component labeledOrDash(String value) {
        if (value == null || value.isBlank()) {
            return Component.translatable("titanreqiuem.book.value.dash");
        }
        String pretty = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
        return Component.literal(pretty);
    }

    private static String formatStatNumber(double value) {
        return (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static void renderTalents(GuiGraphics gfx, Font font, int x, int y, int innerW) {
        y = drawTitle(gfx, font, x, y, innerW, Component.translatable("titanreqiuem.book.tab.talents"));
        y += 10;
        Component text = Component.translatable("titanreqiuem.book.coming_soon");
        int tx = x + (innerW / 2) - (font.width(text) / 2);
        gfx.drawString(font, text, tx, y, INK_MUTED, false);
    }

    private static void renderSettings(GuiGraphics gfx, Font font, int x, int y, int innerW) {
        y = drawTitle(gfx, font, x, y, innerW, Component.translatable("titanreqiuem.book.tab.settings"));
        y += 6;
        y = drawSettingsRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.settings.shiftlock"),
                toggleValue(ShiftlockClientState.isActive(), ODMDiagnosticKeybind.KEY_SHIFTLOCK));
        y = drawSettingsRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.settings.grab_mode"),
                toggleValue(ClientCardStats.isGrabMode(), GrabModeClientSetup.TOGGLE_GRAB_MODE));
        y = drawSettingsRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.settings.consent"),
                toggleValue(ClientCardStats.isConsentOpen(), ConsentClientSetup.TOGGLE_CONSENT));
        drawSettingsRow(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.settings.horse_whistle"),
                settingValue(ODMDiagnosticKeybind.KEY_HORSE_WHISTLE));
    }

    private static void renderReputation(GuiGraphics gfx, Font font, int x, int y, int innerW) {
        y = drawTitle(gfx, font, x, y, innerW, Component.translatable("titanreqiuem.book.tab.reputation"));
        y += 10;
        y = drawLabeledBar(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.rep.paradis"), ClientCardStats.getRepParadis(), 100);
        y += 8;
        drawLabeledBar(gfx, font, x, y, innerW,
                Component.translatable("titanreqiuem.book.rep.marley"), ClientCardStats.getRepMarley(), 100);
    }

    private static void renderUnassigned(GuiGraphics gfx, Font font, int regionX, int regionY, int regionW, int regionH) {
        Component text = Component.translatable("titanreqiuem.book.coming_soon");
        int tx = regionX + (regionW / 2) - (font.width(text) / 2);
        int ty = regionY + (regionH / 2) - (font.lineHeight / 2);
        gfx.drawString(font, text, tx, ty, INK_MUTED, false);
    }

    private static Component settingValue(KeyMapping mapping) {
        Component keyName = mapping.isUnbound()
                ? Component.translatable("titanreqiuem.book.value.dash")
                : mapping.getTranslatedKeyMessage();
        return keyName;
    }

    private static Component toggleValue(boolean active, KeyMapping mapping) {
        String state = active ? "ON" : "OFF";
        Component keyName = mapping.isUnbound()
                ? Component.translatable("titanreqiuem.book.value.dash")
                : mapping.getTranslatedKeyMessage();
        return Component.literal(state + " / ").append(keyName);
    }

    private static int drawTitle(GuiGraphics gfx, Font font, int x, int y, int innerW, Component title) {
        int tx = x + (innerW / 2) - (font.width(title) / 2);
        gfx.drawString(font, title, tx, y, INK, false);
        return y + font.lineHeight + 4;
    }

    private static int drawSettingsRow(GuiGraphics gfx, Font font, int x, int y, int innerW,
                                       Component label, Component value) {
        gfx.drawString(font, label, x, y, INK, false);
        int valueX = x + font.width(label) + SETTINGS_VALUE_GAP;
        int maxValueX = x + innerW - font.width(value);
        if (valueX > maxValueX) {
            valueX = maxValueX;
        }
        gfx.drawString(font, value, valueX, y, INK, false);
        return y + SETTINGS_ROW_H;
    }

    private static int drawRow(GuiGraphics gfx, Font font, int x, int y, int innerW, Component label, Component value) {
        return drawRow(gfx, font, x, y, innerW, ROW_H, label, value);
    }

    private static int drawRow(GuiGraphics gfx, Font font, int x, int y, int innerW, int rowH,
                               Component label, Component value) {
        gfx.drawString(font, label, x, y, INK, false);
        int minValueX = x + font.width(label) + VALUE_GAP;
        int valueX = Math.max(minValueX, x + innerW - font.width(value));
        gfx.drawString(font, value, valueX, y, INK, false);
        return y + rowH;
    }

    private static int drawLabeledBar(GuiGraphics gfx, Font font, int x, int y, int innerW,
                                      Component label, int current, int max) {
        Component count = Component.literal(current + "/" + max);
        gfx.drawString(font, label, x, y, INK, false);
        int barRight = x + innerW;
        gfx.drawString(font, count, barRight - font.width(count), y, INK, false);
        y += font.lineHeight + 3;
        gfx.fill(x, y, barRight, y + BAR_H, BAR_BG);
        if (max > 0 && current > 0) {
            int fillW = Math.min(innerW, innerW * current / max);
            gfx.fill(x, y, x + fillW, y + BAR_H, BAR_FILL);
        }
        return y + BAR_H + 4;
    }
}
