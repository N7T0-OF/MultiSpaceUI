package dev.multispaceui.interaction;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector3f;

/**
 * Translates vanilla mouse input into panel space.
 *
 * <p>Vanilla asks {@code MouseHandler} for the cursor position in GUI units and
 * hands it to the active screen. This class answers that question with the
 * cursor position <b>inside the panel of the active window</b>, so a screen
 * laid out for a small window receives coordinates that match its widgets:</p>
 *
 * <ul>
 *   <li>cursor over the active panel → its GUI coordinates,</li>
 *   <li>cursor off the panel → a value far outside every widget, and the click
 *       itself is dropped (see {@link #shouldDropClick}), so a container can
 *       never interpret it as "clicked outside" and close itself,</li>
 *   <li>cursor over <i>another</i> window → that window is focused first, then
 *       the same mapping applies to it, so one click focuses and interacts,</li>
 *   <li>no placement data yet (first frames, mod half disabled) → vanilla
 *       coordinates, so input degrades to vanilla instead of breaking.</li>
 * </ul>
 */
public final class SpatialInput {

    /** Coordinate reported when the cursor is not over the panel. */
    private static final double OFF_PANEL = -10000.0;

    private SpatialInput() {
    }

    // ----------------------------------------------------------------------------------
    // Coordinate mapping
    // ----------------------------------------------------------------------------------

    public static double mapX(double vanilla) {
        SpatialWindow active = activeWindow();
        if (active == null) {
            return vanilla;
        }
        Vector3f hit = hitOf(active);
        return hit == null ? OFF_PANEL : hit.x() * active.innerWidth();
    }

    public static double mapY(double vanilla) {
        SpatialWindow active = activeWindow();
        if (active == null) {
            return vanilla;
        }
        Vector3f hit = hitOf(active);
        return hit == null ? OFF_PANEL : (1.0f - hit.y()) * active.innerHeight();
    }

    /** Normalized hit of the cursor on {@code window}, or null when it misses. */
    private static Vector3f hitOf(SpatialWindow window) {
        WindowTargeting targeting = SpatialWindowRenderer.instance.targeting();
        if (!targeting.hasCursor() || !targeting.hasPlacement()) {
            return null;
        }
        Vector3f hit = targeting.hitOf(window);
        if (hit == null) {
            return null;
        }
        // Behind the panel or outside it: the cursor is not on this window.
        return hit.z() >= 0.0f && hit.x() >= -0.02f && hit.x() <= 1.02f
                && hit.y() >= -0.02f && hit.y() <= 1.02f
                ? hit
                : null;
    }

    /**
     * The window vanilla input must talk to, or {@code null} when input should
     * stay vanilla (no windows, screen not owned by a window, mod disabled).
     */
    private static SpatialWindow activeWindow() {
        if (!MultiSpaceUI.isEnabled()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui == null) {
            return null;
        }
        SpatialWindow active = WindowManager.get().active();
        if (active == null || active.isClosing() || active.isMinimized()) {
            return null;
        }
        Screen installed = mc.gui.screen();
        if (installed == null || installed != active.screen()) {
            return null;
        }
        // Without placement data the panel cannot be hit: fall back to vanilla input.
        if (!SpatialWindowRenderer.instance.targeting().hasPlacement()) {
            return null;
        }
        return active;
    }

    // ----------------------------------------------------------------------------------
    // Clicks
    // ----------------------------------------------------------------------------------

    /**
     * True when a click (or scroll) aimed at {@code screen} must be dropped
     * because the cursor is not over that screen's panel.
     */
    public static boolean shouldDropClick(Screen screen) {
        SpatialWindow active = activeWindow();
        if (active == null || screen == null || active.screen() != screen) {
            return false;
        }
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        if (cfg.closeOnClickOutside) {
            // The user explicitly wants "click outside to close" to work.
            return false;
        }
        return hitOf(active) == null;
    }

    /**
     * Button value of the left mouse button.
     *
     * <p>Minecraft 26.3 numbers its mouse buttons the way SDL does, so the left button is
     * {@code 1} (middle {@code 2}, right {@code 3}) rather than the old GLFW numbering where
     * the left button is {@code 0}. Every input path of that version agrees on it:
     * {@code MouseHandler} sets {@code isLeftPressed} for 1, the container screens treat 1 and
     * 3 as the buttons that interact with a slot, and {@code key.use} is bound to 3.</p>
     */
    public static final int LEFT_BUTTON = 1;

    /** Called at the head of every mouse button event: focus the window that was hit. */
    public static void onButton(int button, int action) {
        if (action != 1 || button != LEFT_BUTTON || !MultiSpaceUI.isEnabled()) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            WindowTargeting targeting = SpatialWindowRenderer.instance.targeting();
            SpatialWindow pointed = targeting.pointed();
            if (pointed == null || !pointed.isInteractionEnabled()) {
                return;
            }
            WindowManager manager = WindowManager.get();
            if (pointed == manager.active() || pointed.isClosing() || pointed.isMinimized()) {
                return;
            }
            manager.activate(pointed);
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not focus the clicked window", t);
        }
    }

    /** True when {@code screen} is rendered as a spatial window and not in the HUD. */
    public static boolean isManaged(Screen screen) {
        if (screen == null || !MultiSpaceUI.isEnabled()) {
            return false;
        }
        return WindowManager.get().findByScreen(screen) != null;
    }
}
