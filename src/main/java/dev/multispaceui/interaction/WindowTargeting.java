package dev.multispaceui.interaction;

import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.rendering.PanelGeometry;
import dev.multispaceui.rendering.PanelGeometry.QuadBasis;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * Decides which window the player is looking at, which one the cursor is over, and which
 * window a click must be routed to.
 *
 * <p>Three notions are kept distinct on purpose, so the visual focus and the interaction
 * focus can never silently diverge:
 * <ul>
 *   <li><b>pointed</b> — window hit by the cursor: a click on it focuses it,</li>
 *   <li><b>focused</b> — window driving the presentation (cursor first, then the
 *       crosshair when {@code crosshairTargeting} is on),</li>
 *   <li><b>active</b> — window installed as the Minecraft screen, the only one vanilla
 *       input is ever dispatched to.</li>
 * </ul></p>
 *
 * <p>Picking is done by projecting the four corners of every panel with the projection
 * matrix of the current frame and testing the cursor against that quad, so what the
 * pointer hits is exactly what is on screen.</p>
 */
public final class WindowTargeting {

    private SpatialWindow focused;
    private SpatialWindow hovered;
    private SpatialWindow aimed;
    private SpatialWindow pointed;

    /** True once {@link #update} has placed at least one frame worth of panels. */
    private boolean hasPlacement;

    private final Vector3f cursorRay = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f centerRay = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f inputRay = new Vector3f(0.0f, 0.0f, -1.0f);
    private double cursorX;
    private double cursorY;
    private boolean hasCursor;

    /** Recomputes every ray and every pick for this frame. */
    public void update(Minecraft mc, WindowManager manager, Map<SpatialWindow, QuadBasis> bases,
                       Matrix4fc projection, int width, int height) {
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        if (mc.getWindow() == null || projection == null || width <= 0 || height <= 0) {
            focused = hovered = aimed = pointed = null;
            hasCursor = false;
            hasPlacement = false;
            return;
        }

        readCursor(mc);
        Vector3f fromCursor = hasCursor
                ? PanelGeometry.viewRay(projection, cursorX, cursorY, width, height)
                : null;
        Vector3f fromCenter = PanelGeometry.viewRay(projection, width * 0.5, height * 0.5, width, height);
        if (fromCursor != null) {
            cursorRay.set(fromCursor);
        }
        if (fromCenter != null) {
            centerRay.set(fromCenter);
        }

        List<SpatialWindow> windows = manager.windows();
        lastBases = bases;
        hasPlacement = bases != null && !bases.isEmpty();
        hovered = pick(windows, bases, cursorRay);
        aimed = cfg.crosshairTargeting ? pick(windows, bases, centerRay) : null;
        pointed = hovered;

        SpatialWindow next = hovered != null ? hovered : aimed;
        if (next == null) {
            next = manager.active();
        }
        focused = next;
        inputRay.set(hovered != null ? cursorRay : centerRay);
    }

    /** Cursor position, in framebuffer pixels, converted from the raw window position. */
    private void readCursor(Minecraft mc) {
        var window = mc.getWindow();
        double screenW = window.getScreenWidth();
        double screenH = window.getScreenHeight();
        if (screenW <= 0.0 || screenH <= 0.0) {
            hasCursor = false;
            return;
        }
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        cursorX = x * window.getWidth() / screenW;
        cursorY = y * window.getHeight() / screenH;
        hasCursor = !Double.isNaN(cursorX) && !Double.isNaN(cursorY);
    }

    /** Nearest window hit by {@code ray}, or null. */
    private SpatialWindow pick(List<SpatialWindow> windows, Map<SpatialWindow, QuadBasis> bases, Vector3f ray) {
        SpatialWindow best = null;
        float bestDepth = Float.MAX_VALUE;
        for (SpatialWindow window : windows) {
            if (!window.isVisible() || window.isMinimized() || window.isClosing()) {
                continue;
            }
            QuadBasis basis = bases.get(window);
            Vector3f hit = PanelGeometry.intersect(ray, basis);
            if (hit == null) {
                continue;
            }
            float depth = PanelGeometry.viewDepth(basis);
            if (depth < bestDepth) {
                bestDepth = depth;
                best = window;
            }
        }
        return best;
    }

    public SpatialWindow focused() {
        return focused;
    }

    public SpatialWindow hovered() {
        return hovered;
    }

    public SpatialWindow aimed() {
        return aimed;
    }

    /** Window hit by the cursor; a click on it must focus it. */
    public SpatialWindow pointed() {
        return pointed;
    }

    public Vector3f cursorRay() {
        return cursorRay;
    }

    public Vector3f centerRay() {
        return centerRay;
    }

    public Vector3f inputRay() {
        return inputRay;
    }

    public double cursorX() {
        return cursorX;
    }

    public double cursorY() {
        return cursorY;
    }

    public boolean hasCursor() {
        return hasCursor;
    }

    /** True once at least one panel has been placed in view space this session. */
    public boolean hasPlacement() {
        return hasPlacement;
    }

    /** Normalized hit of {@code window} against the cursor ray, or null. */
    public Vector3f hitOf(SpatialWindow window) {
        if (window == null) {
            return null;
        }
        return PanelGeometry.intersect(cursorRay, hitBasis(window));
    }

    private QuadBasis hitBasis(SpatialWindow window) {
        return lastBases == null ? null : lastBases.get(window);
    }

    private Map<SpatialWindow, QuadBasis> lastBases;
}
