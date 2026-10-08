package dev.multispaceui.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * The shortcuts of the mod. Every binding is registered in the vanilla controls
 * screen, so players can rebind them like any other key.
 *
 * <ul>
 *   <li><b>Toggle MultiSpaceUI</b> - turns spatial windows on/off,</li>
 *   <li><b>Focus next window</b> - cycles the active window,</li>
 *   <li><b>Close window</b> - closes the active window (its menu is closed
 *       server side, the other windows stay).</li>
 * </ul>
 */
final class MultiSpaceKeybinds {

    private final KeyMapping toggle;
    private final KeyMapping nextWindow;
    private final KeyMapping closeWindow;

    MultiSpaceKeybinds() {
        this.toggle = new KeyMapping(
                "key.multispaceui.toggle",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC
        );
        this.nextWindow = new KeyMapping(
                "key.multispaceui.next_window",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC
        );
        this.closeWindow = new KeyMapping(
                "key.multispaceui.close_window",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC
        );
    }

    void register() {
        KeyMappingHelper.registerKeyMapping(toggle);
        KeyMappingHelper.registerKeyMapping(nextWindow);
        KeyMappingHelper.registerKeyMapping(closeWindow);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        MultiSpaceUI.LOGGER.info(
                "MultiSpaceUI keybinds registered (rebind them in Controls > MultiSpaceUI)");
    }

    private void tick() {
        while (toggle.consumeClick()) {
            MultiSpaceConfig cfg = MultiSpaceConfig.get();
            cfg.enabled = !cfg.enabled;
            MultiSpaceConfig.save();
            if (!cfg.enabled) {
                WindowManager.get().dropAllWindows();
            }
            MultiSpaceUI.LOGGER.info("MultiSpaceUI {}", cfg.enabled ? "enabled" : "disabled");
        }
        while (nextWindow.consumeClick()) {
            WindowManager.get().activateNext();
        }
        while (closeWindow.consumeClick()) {
            WindowManager.get().closeActive();
        }
    }
}
