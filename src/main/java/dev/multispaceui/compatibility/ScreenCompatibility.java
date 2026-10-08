package dev.multispaceui.compatibility;

import dev.multispaceui.config.MultiSpaceConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Decides whether a {@link Screen} can be turned into a spatial window.
 *
 * <p>A screen that cannot be rendered or used correctly is left in vanilla 2D instead of
 * being forced into a panel that would break it.</p>
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
        if (screen instanceof CreativeModeInventoryScreen || screen instanceof InventoryScreen) {
            return cfg.spatialInventory;
        }
        if (screen instanceof AbstractContainerScreen<?>) {
            return cfg.spatialContainers;
        }
        if (!screen.isInGameUi()) {
            return false;
        }
        return cfg.spatialOtherScreens;
    }

    /** Records an unknown screen so the settings screen can list every screen seen in game. */
    public static void remember(Screen screen) {
        // Reserved for the settings UI: kept deliberately side-effect free for now.
    }
}
