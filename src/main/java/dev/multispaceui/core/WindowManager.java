package dev.multispaceui.core;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.compatibility.ScreenCompatibility;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owns every open {@link SpatialWindow} and decides which one talks to the game.
 *
 * <p>Only {@link #active()} is installed as the Minecraft screen, so vanilla input routing,
 * ticking and closing semantics are preserved. Background windows stay alive as plain Java
 * objects: their {@link Screen} keeps its widgets, and their menu keeps receiving packets.</p>
 */
public final class WindowManager {

    private static final WindowManager INSTANCE = new WindowManager();

    private final List<SpatialWindow> windows = new ArrayList<>();
    private SpatialWindow active;
    private boolean installedAsScreen;

    private WindowManager() {
    }

    public static WindowManager get() {
        return INSTANCE;
    }

    public List<SpatialWindow> windows() {
        return windows;
    }

    public SpatialWindow active() {
        return active;
    }

    public boolean hasWindows() {
        return !windows.isEmpty();
    }

    public SpatialWindow findByScreen(Screen screen) {
        if (screen == null) {
            return null;
        }
        for (SpatialWindow w : windows) {
            if (w.screen() == screen) {
                return w;
            }
        }
        return null;
    }

    public SpatialWindow findByMenuId(int menuId) {
        if (menuId < 0) {
            return null;
        }
        for (SpatialWindow w : windows) {
            if (w.menuId() == menuId) {
                return w;
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------------------
    // Screen lifecycle
    // ----------------------------------------------------------------------------------

    /** Registers {@code screen} as a window when the game opens it. */
    public void onScreenOpened(Screen screen) {
        if (!MultiSpaceConfig.get().enabled || !ScreenCompatibility.isWindowable(screen)) {
            return;
        }
        if (findByScreen(screen) != null) {
            return;
        }
        int menuId = menuIdOf(screen);
        SpatialWindow window = new SpatialWindow(screen, menuId);
        windows.add(window);
        active = window;
        reindex();
        MultiSpaceUI.LOGGER.debug("Opened spatial window {} (menu {})", window.screenClass(), menuId);
    }

    /**
     * Called by the {@code Gui.setScreen} mixin at TAIL to reconcile the window list with
     * whatever the game decided to display.
     */
    public void onScreenReplaced(Screen newScreen) {
        if (newScreen == null) {
            if (!installedAsScreen) {
                dropAllWindows();
            }
            installedAsScreen = false;
            return;
        }
        SpatialWindow window = findByScreen(newScreen);
        if (window != null) {
            active = window;
            installedAsScreen = true;
            return;
        }
        installedAsScreen = false;
        if (!ScreenCompatibility.isWindowable(newScreen)) {
            // The player left the window workspace (chat, pause, death...): fold everything.
            dropAllWindows();
        }
    }

    /**
     * Decides whether {@code removed()} must be skipped for {@code old} when {@code next}
     * replaces it. Skipped {@code removed()} keeps the screen object (and its menu) alive
     * as a background window instead of tearing it down.
     */
    public boolean shouldKeepOnReplace(Screen old, Screen next) {
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        if (!cfg.enabled || !cfg.multiWindowEnabled || old == next || next == null) {
            return false;
        }
        SpatialWindow window = findByScreen(old);
        if (window == null || window.isClosing()) {
            return false;
        }
        return ScreenCompatibility.isWindowable(next);
    }

    // ----------------------------------------------------------------------------------
    // Activation and closing
    // ----------------------------------------------------------------------------------

    /** Makes {@code window} the screen the game talks to. Safe to call with the active one. */
    public void activate(SpatialWindow window) {
        if (window == null || window.isClosing()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        active = window;
        if (mc.gui.screen() == window.screen()) {
            installedAsScreen = true;
            return;
        }
        mc.gui.setScreen(window.screen());
    }

    public void activateNext() {
        for (SpatialWindow w : windows) {
            if (!w.isClosing() && !w.isMinimized()) {
                activate(w);
                return;
            }
        }
    }

    /**
     * Closes one window: its menu is closed server side, the screen is detached and the
     * remaining windows take over. Other windows stay open.
     */
    public void close(SpatialWindow window) {
        if (window == null || window.isClosing()) {
            return;
        }
        boolean wasActive = active == window;
        window.beginClose();
        closeMenuOf(window);

        if (wasActive) {
            Minecraft mc = Minecraft.getInstance();
            SpatialWindow next = nextAlive(window);
            if (next != null) {
                activate(next);
            } else {
                mc.gui.setScreen(null);
            }
        }
    }

    public void closeActive() {
        close(active);
    }

    public void closeAll() {
        for (SpatialWindow w : new ArrayList<>(windows)) {
            if (!w.isClosing()) {
                closeMenuOf(w);
                w.beginClose();
            }
        }
        Minecraft.getInstance().gui.setScreen(null);
    }

    /** Immediate teardown, no animation, no packets already sent are repeated. */
    public void dropAllWindows() {
        for (SpatialWindow w : windows) {
            if (!w.isClosing()) {
                closeMenuOf(w);
            }
        }
        windows.clear();
        active = null;
    }

    /** Removes windows whose close animation finished. */
    public void tick() {
        long animMs = MultiSpaceConfig.get().animationsEnabled ? 180L : 0L;
        Iterator<SpatialWindow> it = windows.iterator();
        boolean listChanged = false;
        while (it.hasNext()) {
            SpatialWindow w = it.next();
            if (w.isCloseFinished(animMs)) {
                it.remove();
                listChanged = true;
                if (active == w) {
                    active = null;
                }
            }
        }
        if (listChanged) {
            reindex();
            if (active == null && !windows.isEmpty()) {
                SpatialWindow first = nextAlive(null);
                if (first != null && Minecraft.getInstance().gui.screen() == null) {
                    activate(first);
                }
            }
        }
    }

    /**
     * Closes the menu behind {@code window} on the server. When the menu is not the one the
     * game currently believes in, the packet is addressed by id so the server (which tracks
     * every open menu when MultiSpaceUI is installed on both sides) closes the right one.
     */
    private void closeMenuOf(SpatialWindow window) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !window.hasMenu()) {
            return;
        }
        AbstractContainerMenu menu = window.menuOrNull();
        if (menu == null) {
            return;
        }
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundContainerClosePacket(menu.containerId));
        }
        if (player.containerMenu == menu) {
            player.containerMenu = player.inventoryMenu;
        }
    }

    private SpatialWindow nextAlive(SpatialWindow except) {
        for (SpatialWindow w : windows) {
            if (w != except && !w.isClosing() && !w.isMinimized()) {
                return w;
            }
        }
        return null;
    }

    private void reindex() {
        int index = 0;
        for (SpatialWindow w : windows) {
            if (!w.isClosing()) {
                w.setLayoutIndex(index++);
            }
        }
    }

    private static int menuIdOf(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            AbstractContainerMenu menu = containerScreen.getMenu();
            return menu == null ? -1 : menu.containerId;
        }
        return -1;
    }
}
