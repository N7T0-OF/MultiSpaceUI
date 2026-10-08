package dev.multispaceui.window;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A screen displayed as a floating panel in the world.
 *
 * <p>A window owns its {@link Screen} instance for as long as it is open. Only the active
 * window is installed as {@code Minecraft.gui.screen()}, so vanilla keeps feeding it ticks,
 * keys and clicks exactly like a normal screen; background windows keep their widgets and
 * state alive and are re-activated on demand.</p>
 */
public final class SpatialWindow {

    private final Screen screen;
    private final String screenClass;
    private final long openedAt = System.currentTimeMillis();

    /** Container id of the menu driving this window, or -1 for a screen without a menu. */
    private final int menuId;

    /** Order in the fan layout; recomputed whenever the window list changes. */
    private int layoutIndex;

    // --- geometry in GUI units ----------------------------------------------------
    private int innerWidth;
    private int innerHeight;
    private boolean layoutDirty = true;

    // --- world anchor -------------------------------------------------------------
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    /** Yaw, in degrees, the panel was opened with; its normal points back at the player. */
    private float anchorYaw;
    /** Pitch, in degrees, the panel was opened with (0 in third person). */
    private float anchorPitch;
    /** False until the window has been placed once in the world. */
    private boolean hasAnchor;

    // --- animated presentation ---------------------------------------------------
    private float scale;
    private float opacity;
    private float targetScale;
    private float targetOpacity;
    private float baseDistance;
    private float targetDistance;
    /** Per window depth adjustment, on top of the global panel distance. */
    private float distanceOffset;

    // --- lifecycle ---------------------------------------------------------------
    private boolean minimized;
    private boolean pinned;
    private boolean closing;
    private boolean visible = true;
    private boolean interactionEnabled = true;
    private long closedAt;

    // --- presentation bookkeeping ------------------------------------------------
    private boolean focused;
    private boolean hovered;

    public SpatialWindow(Screen screen, int menuId) {
        this.screen = screen;
        this.screenClass = screen.getClass().getName();
        this.menuId = menuId;
    }

    public Screen screen() {
        return screen;
    }

    public String screenClass() {
        return screenClass;
    }

    public int menuId() {
        return menuId;
    }

    public boolean hasMenu() {
        return menuId >= 0;
    }

    public long openedAt() {
        return openedAt;
    }

    public int layoutIndex() {
        return layoutIndex;
    }

    public void setLayoutIndex(int layoutIndex) {
        this.layoutIndex = layoutIndex;
    }

    // --- size --------------------------------------------------------------------

    /** Content size of the window, in GUI units. */
    public int innerWidth() {
        return innerWidth;
    }

    public int innerHeight() {
        return innerHeight;
    }

    public float aspect() {
        return innerHeight <= 0 ? 1.0f : (float) innerWidth / (float) innerHeight;
    }

    /** Resizes the window and asks the screen to lay itself out again. */
    public void resize(int width, int height) {
        int w = Math.max(64, Math.min(width, 2048));
        int h = Math.max(48, Math.min(height, 2048));
        if (w == innerWidth && h == innerHeight) {
            return;
        }
        innerWidth = w;
        innerHeight = h;
        layoutDirty = true;
    }

    /** True when the screen must be re-initialised for the current size. */
    public boolean consumeLayoutDirty() {
        if (!layoutDirty) {
            return false;
        }
        layoutDirty = false;
        return true;
    }

    public void markLayoutDirty() {
        this.layoutDirty = true;
    }

    // --- world anchor -------------------------------------------------------------

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }

    public float anchorYaw() {
        return anchorYaw;
    }

    public float anchorPitch() {
        return anchorPitch;
    }

    /** True once the window has been placed in the world at least once. */
    public boolean hasAnchor() {
        return hasAnchor;
    }

    public void setAnchor(double x, double y, double z, float yaw, float pitch) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.anchorYaw = yaw;
        this.anchorPitch = pitch;
        this.hasAnchor = true;
    }

    /** Moves the anchor with the player, keeping the relative placement untouched. */
    public void moveAnchor(double x, double y, double z, float yaw, float pitch) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.anchorYaw = yaw;
        this.anchorPitch = pitch;
        this.hasAnchor = true;
    }

    public float scale() {
        return scale;
    }

    public float opacity() {
        return opacity;
    }

    public float targetScale() {
        return targetScale;
    }

    public float targetOpacity() {
        return targetOpacity;
    }

    public void setTargets(float targetScale, float targetOpacity, float targetDistance) {
        this.targetScale = targetScale;
        this.targetOpacity = targetOpacity;
        this.targetDistance = targetDistance;
    }

    /** Seeds both the current and the target values; used when a window is created. */
    public void snapPresentation(float value, float opacity, float distance) {
        this.scale = this.targetScale = value;
        this.opacity = this.targetOpacity = opacity;
        this.baseDistance = this.targetDistance = distance;
    }

    /** Frame-step of the temporal smoothing; {@code dt} is in seconds. */
    public void interpolate(float dt, float speed, boolean animationsEnabled) {
        if (!animationsEnabled || speed <= 0.0f) {
            scale = targetScale;
            opacity = targetOpacity;
            baseDistance = targetDistance;
            return;
        }
        float alpha = 1.0f - (float) Math.exp(-dt * speed);
        scale += (targetScale - scale) * alpha;
        opacity += (targetOpacity - opacity) * alpha;
        baseDistance += (targetDistance - baseDistance) * alpha;
        if (Math.abs(targetScale - scale) < 1.0E-4f) {
            scale = targetScale;
        }
        if (Math.abs(targetOpacity - opacity) < 1.0E-4f) {
            opacity = targetOpacity;
        }
        if (Math.abs(targetDistance - baseDistance) < 1.0E-4f) {
            baseDistance = targetDistance;
        }
    }

    /** Distance of the panel from the camera, in blocks. */
    public float distance() {
        return baseDistance;
    }

    public float distanceOffset() {
        return distanceOffset;
    }

    public void setDistanceOffset(float distanceOffset) {
        this.distanceOffset = distanceOffset;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }

    public void setInteractionEnabled(boolean interactionEnabled) {
        this.interactionEnabled = interactionEnabled;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isClosing() {
        return closing;
    }

    /** Starts the close animation; the window is dropped once it has fully faded out. */
    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        closedAt = System.currentTimeMillis();
        targetOpacity = 0.0f;
        targetScale = targetScale * 0.6f;
    }

    public boolean isCloseFinished(long animationMs) {
        if (!closing) {
            return false;
        }
        return System.currentTimeMillis() - closedAt >= animationMs;
    }

    /** True as soon as the fade-out started, used to stop feeding it input. */
    public boolean blocksInput() {
        return closing || minimized || !interactionEnabled;
    }

    public AbstractContainerMenu menuOrNull() {
        return screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container
                ? container.getMenu()
                : null;
    }
}
