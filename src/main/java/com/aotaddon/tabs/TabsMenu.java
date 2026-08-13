package com.aotaddon.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Registry mapping screen classes to the set of tabs that should be drawn on top of them.
 * Clicking a tab does NOT embed another screen's content into a shared panel — it fully
 * replaces the current screen via {@link TabBase#onClick}, the same way the original
 * Legendary Tabs mod's TabsMenu worked. The "seamless" feel comes from every participating
 * screen also having the tab bar registered on it.
 */
public final class TabsMenu {

    private static final Map<Class<? extends Screen>, ScreenInfo> TABS_SCREENS = new HashMap<>();

    private static int leftScreenPos;
    private static int topScreenPos;
    private static int startTabIndex;
    private static int visibleTabCount;
    private static List<TabBase> enabledTabsCache = new ArrayList<>();

    private TabsMenu() {
    }

    /**
     * Register a tab onto a screen class.
     *
     * @param widthFn  supplies the "panel width" that screen is expected to render at, in pixels
     *                 (e.g. 176 for vanilla inventory-sized screens). Used only for the initial
     *                 centered-position guess before the real per-frame position kicks in.
     * @param heightFn same idea, panel height.
     * @param priority lower priority = appears further left in the tab bar. Use gaps of 10
     *                 (10, 20, 30...) so it's easy to slot new tabs in between later.
     */
    public static void addTabToScreen(TabBase tab, Class<? extends Screen> screenClass,
                                       Function<Player, Integer> widthFn, Function<Player, Integer> heightFn,
                                       int priority) {
        TABS_SCREENS.computeIfAbsent(screenClass, s -> new ScreenInfo(widthFn, heightFn))
                .addTab(priority, tab);
    }

    /**
     * Call from a Render.Pre-style hook every frame, with the screen's real panel origin
     * (e.g. an AbstractContainerScreen's getGuiLeft()/getGuiTop()). Only repositions buttons
     * if the origin actually changed since last frame.
     */
    public static void updateButtonsPosition(Screen screen, int left, int top) {
        if (leftScreenPos == left && topScreenPos == top) {
            return;
        }
        leftScreenPos = left;
        topScreenPos = top;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof TabButton button) {
                button.updatePosition(left, top);
            } else if (listener instanceof NextTabsButton button) {
                button.updatePosition(left, top);
            }
        }
    }

    /**
     * Call from a Init.Post-style hook. Builds the tab buttons for this screen, if it's
     * registered. No-ops silently for unregistered screens.
     */
    public static void initScreenButtons(Screen screen, ScreenInitCallback callback) {
        ScreenInfo info = TABS_SCREENS.get(screen.getClass());
        if (info == null) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int panelWidth = info.widthFn.apply(player);
        int panelHeight = info.heightFn.apply(player);
        leftScreenPos = (screen.width - panelWidth) / 2;
        topScreenPos = (screen.height - panelHeight) / 2;

        // Not enough room above the panel for a tab row — skip rather than draw off-screen.
        if (topScreenPos - 22 < 0) {
            return;
        }

        enabledTabsCache = new ArrayList<>();
        for (List<TabBase> tabs : info.tabsByPriority.values()) {
            for (TabBase tab : tabs) {
                if (tab.isEnabled(player)) {
                    enabledTabsCache.add(tab);
                }
            }
        }

        startTabIndex = 0;
        visibleTabCount = 0;
        int remainingWidth = panelWidth;
        for (TabBase tab : enabledTabsCache) {
            if (remainingWidth <= 26) {
                break;
            }
            callback.addListener(new TabButton(tab, visibleTabCount, leftScreenPos, topScreenPos,
                    screen.getClass()));
            remainingWidth -= 27;
            visibleTabCount++;
        }

        if (enabledTabsCache.size() > visibleTabCount) {
            callback.addListener(new NextTabsButton(visibleTabCount, leftScreenPos, topScreenPos,
                    b -> cycleTabs(screen)));
        }
    }

    private static void cycleTabs(Screen screen) {
        List<TabButton> buttons = screen.children().stream()
                .filter(TabButton.class::isInstance)
                .map(TabButton.class::cast)
                .toList();

        startTabIndex = (startTabIndex + visibleTabCount >= enabledTabsCache.size())
                ? 0
                : startTabIndex + visibleTabCount;

        int shown = 0;
        for (int i = startTabIndex; i < enabledTabsCache.size() && shown < buttons.size(); i++, shown++) {
            buttons.get(shown).setTab(enabledTabsCache.get(i));
        }
    }

    public interface ScreenInitCallback {
        void addListener(GuiEventListener listener);
    }

    private static final class ScreenInfo {
        final Function<Player, Integer> widthFn;
        final Function<Player, Integer> heightFn;
        final TreeMap<Integer, List<TabBase>> tabsByPriority = new TreeMap<>();

        ScreenInfo(Function<Player, Integer> widthFn, Function<Player, Integer> heightFn) {
            this.widthFn = widthFn;
            this.heightFn = heightFn;
        }

        void addTab(int priority, TabBase tab) {
            tabsByPriority.computeIfAbsent(priority, p -> new ArrayList<>()).add(tab);
        }
    }
}
