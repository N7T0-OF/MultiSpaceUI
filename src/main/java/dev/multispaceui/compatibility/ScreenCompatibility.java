package dev.multispaceui.compatibility;

import dev.multispaceui.config.MultiSpaceConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

/**
 * Decides whether a {@link Screen} can be turned into a spatial window.
 *
 * <p>A screen that cannot be rendered or used correctly is left in vanilla 2D instead of
 * being forced into a panel that would break it.</p>
 *
 * <p>The final answer is the conjunction of two filters: this mod's own white / black
 * lists, and the Spatial GUI base's ({@code SpatialGUIClient.shouldHookScreen}). The base
 * decides what it is willing to render as a panel - it would keep everything else flat in
 * the HUD - so a screen accepted here but rejected there would end up drawn twice, once in
 * the HUD and once as a window.</p>
 */
public final class ScreenCompatibility {

    /** Screens that are structurally incompatible with a floating panel. */
    private static final String[] ALWAYS_FLAT = {
            "TitleScreen",
            "ChatScreen",
            "PauseScreen",
            "ReceivingLevelScreen",
            "LevelLoadingScreen",
            "ConnectScreen",
            "DownloadingTerrainScreen",
            "GenericMessageScreen",
            "ConfirmScreen",
            "OpenToLanScreen",
    };

    private ScreenCompatibility() {
    }

    public static boolean isWindowable(Screen screen) {
        if (screen == null) {
            return false;
        }
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        if (!cfg.enabled) {
            return false;
        }
        String id = screen.getClass().getName();
        if (cfg.excludedScreens.contains(id)) {
            return false;
        }
        String simple = screen.getClass().getSimpleName();
        for (String banned : ALWAYS_FLAT) {
            if (simple.contains(banned) || id.endsWith("." + banned)) {
                // An explicit whitelist entry wins over the built-in exclusion.
                return cfg.includedScreens.contains(id);
            }
        }
        if (cfg.includedScreens.contains(id)) {
            return true;
        }
        boolean allowed;
        if (screen instanceof CreativeModeInventoryScreen || screen instanceof InventoryScreen) {
            allowed = cfg.spatialInventory;
        } else if (screen instanceof AbstractContainerScreen<?>) {
            allowed = cfg.spatialContainers;
        } else if (!screen.isInGameUi()) {
            allowed = false;
        } else {
            allowed = cfg.spatialOtherScreens;
        }
        return allowed && SpatialGUIClient.shouldHookScreen(screen);
    }

    /** Records an unknown screen so the settings screen can list every screen seen in game. */
    public static void remember(Screen screen) {
        // The base already records every screen it is asked about (its `seenScreens`
        // list, filled by shouldHookScreen), which is what the settings screen reads.
    }
}
