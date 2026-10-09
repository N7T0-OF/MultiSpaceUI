package dev.multispaceui.interaction;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;

/**
 * Focus and click routing for the window layer.
 *
 * <p>The cursor-to-panel mapping itself belongs to the Spatial GUI base: its
 * {@code MouseHandlerMixin} rewrites {@code getScaledXPos/getScaledYPos} so vanilla hands
 * the active screen coordinates <b>inside its own panel</b>. This class therefore only
 * answers the two questions the base does not know about, because the base only ever sees
 * one window:</p>
 *
 * <ul>
 *   <li><b>where does the cursor land?</b> - {@link #mapX}/{@link #mapY} recompute the
 *       very mapping the base applies, so tests and callers see exactly what the screen
 *       receives (and a miss reports {@link #OFF_PANEL}, far outside every widget),</li>
 *   <li><b>may this click go through?</b> - {@link #shouldDropClick} drops a click or a
 *       scroll that misses every panel, so a container can never interpret it as "clicked
 *       outside the slot area" and close itself,</li>
 *   <li><b>which window owns the input?</b> - {@link #onButton} focuses the window the
 *       cursor is over before the click is dispatched, and re-runs the base's mapping so
 *       that the very same click already lands on the window that just became active.</li>
 * </ul>
 *
 * <p>When the mapping data is missing (first frames, no window installed as the screen,
 * mod switched off) everything degrades to vanilla input instead of breaking.</p>
 */
public final class SpatialInput {

    /** Coordinate reported when the cursor is not over the active panel. */
    public static final double OFF_PANEL = -10000.0;

    private SpatialInput() {
    }

    // ----------------------------------------------------------------------------------
    // Coordinate mapping
    // ----------------------------------------------------------------------------------

    /**
     * X coordinate the active screen receives for this frame, in its own GUI units.
     *
     * @param vanilla value vanilla would have answered, kept when there is no window
     */
    public static double mapX(double vanilla) {
        double[] gui = activePanelPosition();
        if (gui == null) {
            return activeWindow() == null ? vanilla : OFF_PANEL;
        }
        return gui[0];
    }

    /** Y coordinate the active screen receives for this frame, in its own GUI units. */
    public static double mapY(double vanilla) {
        double[] gui = activePanelPosition();
        if (gui == null) {
            return activeWindow() == null ? vanilla : OFF_PANEL;
        }
        return gui[1];
    }

    /**
     * Cursor position inside the panel of the active window, in that screen's GUI units,
     * or {@code null} when the cursor is not over the panel.
     *
     * <p>Measured with the placement the renderer actually drew, so hover, clicks and the
     * content of the texture can never disagree by a pixel.</p>
     */
    private static double[] activePanelPosition() {
        SpatialWindow active = activeWindow();
        if (active == null) {
            return null;
        }
        try {
            return SpatialWindowRenderer.instance.cursorOn(active);
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not map the cursor onto the panel", t);
            return null;
        }
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
     *
     * <p>Without this guard a click landing in the world would reach the container screen
     * as a click in an empty part of its slot area, which vanilla answers by closing it.</p>
     */
    public static boolean shouldDropClick(Screen screen) {
        SpatialWindow active = activeWindow();
        if (active == null || screen == null || active.screen() != screen) {
            return false;
        }
        if (MultiSpaceConfig.get().closeOnClickOutside) {
            // The user explicitly wants "click outside to close" to work.
            return false;
        }
        return activePanelPosition() == null;
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

    /**
     * Called at the head of every mouse button event: focus the window that was hit, so the
     * click it triggered is delivered to it instead of to the window that had the focus.
     */
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
            refreshBaseMapping();
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not focus the clicked window", t);
        }
    }

    /**
     * Re-runs the base's cursor mapping right after the active window changed.
     *
     * <p>{@code setScreen} re-hooked the new screen, so its quad basis and its mapping are
     * rebuilt here: without this the click that caused the swap would be delivered with the
     * coordinates of the window that just lost the focus.</p>
     */
    private static void refreshBaseMapping() {
        SpatialGUIRenderer renderer = SpatialGUIRendererHolder.renderer();
        if (renderer == null || !renderer.shouldCapture()) {
            return;
        }
        try {
            renderer.getInventoryRenderer().updateQuadBasis();
            renderer.updateMousePosition(MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY());
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not refresh the panel mapping", t);
        }
    }

    /** True when {@code screen} is rendered as a spatial window and not in the HUD. */
    public static boolean isManaged(Screen screen) {
        if (screen == null || !MultiSpaceUI.isEnabled()) {
            return false;
        }
        return WindowManager.get().findByScreen(screen) != null;
    }

    /** Small indirection so the base renderer can be missing without an import cycle. */
    private static final class SpatialGUIRendererHolder {
        private static SpatialGUIRenderer renderer() {
            return SpatialGUIClient.renderer();
        }
    }
}
