package dev.multispaceui.client;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Client bootstrap: config, keybinds, per-tick housekeeping and disconnect cleanup. */
final class MultiSpaceUIHooks {
    private MultiSpaceUIHooks() {
    }

    static void bootstrap() {
        MultiSpaceConfig.load();

        new MultiSpaceKeybinds().register();

        // Window lifecycle: finished close animations are dropped every tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MultiSpaceUI.isEnabled()) {
                return;
            }
            try {
                WindowManager.get().tick();
            } catch (Throwable t) {
                MultiSpaceUI.LOGGER.error("Window tick failed", t);
            }
        });

        // Leaving a world must not carry panels, captures or windows over.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            try {
                WindowManager.get().dropAllWindows();
                SpatialWindowRenderer.instance.onDisconnect();
            } catch (Throwable t) {
                MultiSpaceUI.LOGGER.debug("Disconnect cleanup failed", t);
            }
        });

        MultiSpaceUI.LOGGER.info("MultiSpaceUI client initialized");
    }
}
