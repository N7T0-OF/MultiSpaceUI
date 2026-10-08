package dev.multispaceui.rendering;

import dev.multispaceui.window.SpatialWindow;

/**
 * The window whose content is currently being extracted or drawn off-screen.
 *
 * <p>While it is set, {@code WindowMixin} reports <i>that</i> window size as the GUI size of
 * the game window, so a screen lays itself out for the panel it is going to be shown in
 * instead of the whole monitor. Vanilla never sees the override: it is pushed and popped
 * around the isolated extraction only.</p>
 */
public final class MultiSpaceLayout {

    private static SpatialWindow current;

    private MultiSpaceLayout() {
    }

    public static void push(SpatialWindow window) {
        current = window;
    }

    public static void pop() {
        current = null;
    }

    /** Clears any window override left behind by a failed extraction. */
    public static void popAll() {
        current = null;
    }

    public static SpatialWindow current() {
        return current;
    }

    public static boolean isOverridden() {
        return current != null;
    }

    /** GUI width of the window currently being extracted. */
    public static int width(int original) {
        return current == null ? original : current.innerWidth();
    }

    /** GUI height of the window currently being extracted. */
    public static int height(int original) {
        return current == null ? original : current.innerHeight();
    }
}
