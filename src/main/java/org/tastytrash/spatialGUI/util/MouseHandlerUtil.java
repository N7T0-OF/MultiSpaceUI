package org.tastytrash.spatialGUI.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.joml.Vector2d;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.mixin.gui.MouseHandlerAccessor;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.util.RenderUtil.QuadBasis;
import org.tastytrash.spatialGUI.util.RenderUtil.CylinderBasis;
//? if <26.3 {
/*import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
*///?}

public class MouseHandlerUtil {
    private static boolean weGrabbedMouse = false;

    private static double physicalX = Double.NaN;
    private static double physicalY = Double.NaN;

    private static double lastPhysicalX = Double.NaN;
    private static double lastPhysicalY = Double.NaN;

    private static double mappedX = Double.NaN;
    private static double mappedY = Double.NaN;

    private static int heldHoverX = Integer.MIN_VALUE;
    private static int heldHoverY = Integer.MIN_VALUE;
    private static final double HOVER_DEADBAND_PX = 1.5;

    private static double freeLookDeltaX = 0;
    private static double freeLookDeltaY = 0;

    public static void resetMouseState() {
        Minecraft mc = Minecraft.getInstance();
        mappedX = Double.NaN;
        mappedY = Double.NaN;
        heldHoverX = Integer.MIN_VALUE;
        heldHoverY = Integer.MIN_VALUE;
        freeLookDeltaX = 0;
        freeLookDeltaY = 0;

        if (((MouseHandlerAccessor) mc.mouseHandler).getMouseGrabbed()) {
            physicalX = mc.getWindow().getScreenWidth() / 2.0;
            physicalY = mc.getWindow().getScreenHeight() / 2.0;
            lastPhysicalX = physicalX;
            lastPhysicalY = physicalY;
        } else {
            //? if >=26.3 {
            mc.mouseHandler.resyncMousePosition();
            physicalX = ((MouseHandlerAccessor) mc.mouseHandler).getRawXpos();
            physicalY = ((MouseHandlerAccessor) mc.mouseHandler).getRawYpos();
            //?} else {
            /*try (MemoryStack stack = MemoryStack.stackPush()) {
                DoubleBuffer x = stack.mallocDouble(1);
                DoubleBuffer y = stack.mallocDouble(1);
                //? if >1.21.1 {
                GLFW.glfwGetCursorPos(mc.getWindow().handle(), x, y);
                //?} else {
                /^GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), x, y);
                ^///?}
                physicalX = x.get(0);
                physicalY = y.get(0);
            }
            *///?}
            lastPhysicalX = physicalX;
            lastPhysicalY = physicalY;
        }
    }

    public static double[] captureMove(double x, double y, boolean mouseGrabbed) {
        double[] delta = {x - lastPhysicalX, y - lastPhysicalY};
        lastPhysicalX = x;
        lastPhysicalY = y;
        if (!mouseGrabbed) {
            physicalX = x;
            physicalY = y;
        }
        return delta;
    }

    public static double getSourceX() {
        Minecraft mc = Minecraft.getInstance();
        return SpatialGUIRenderer.isCrosshairModeActive()
                ? mc.getWindow().getScreenWidth() / 2.0
                : getPhysicalX(mc.getWindow().getScreenWidth() / 2.0);
    }

    public static double getSourceY() {
        Minecraft mc = Minecraft.getInstance();
        return SpatialGUIRenderer.isCrosshairModeActive()
                ? mc.getWindow().getScreenHeight() / 2.0
                : getPhysicalY(mc.getWindow().getScreenHeight() / 2.0);
    }

    public static double getPhysicalX(double fallback) {
        return Double.isNaN(physicalX) ? fallback : physicalX;
    }

    public static double getPhysicalY(double fallback) {
        return Double.isNaN(physicalY) ? fallback : physicalY;
    }

    public static Vector2d mapMousePosition(double srcX, double srcY, QuadBasis quadBasis, CylinderBasis cylinderBasis, double guiScale, com.mojang.blaze3d.pipeline.TextureTarget target) {
        Vector2d mouse = cylinderBasis != null
                ? RenderUtil.getInventoryMousePositionRayCurved(srcX, srcY, cylinderBasis)
                : RenderUtil.getInventoryMousePositionRay(srcX, srcY, quadBasis, target);
        if (mouse == null) {
            return null;
        }
        mappedX = mouse.x / guiScale;
        mappedY = mouse.y / guiScale;
        return mouse;
    }

    public static double getFallback(boolean isX) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            return isX ? mc.getWindow().getGuiScaledWidth() / 2.0 : mc.getWindow().getGuiScaledHeight() / 2.0;
        }
        return 0.0;
    }

    public static double getLastPos(boolean isX, double fallback) {
        double val = isX ? mappedX : mappedY;
        return Double.isNaN(val) ? fallback : val;
    }

    public static double getLastPos(boolean isX) {
        return getLastPos(isX, getFallback(isX));
    }

    public static int getHoverX() {
        double v = getLastPos(true);
        if (heldHoverX != Integer.MIN_VALUE && Math.abs(v - heldHoverX) < HOVER_DEADBAND_PX) return heldHoverX;
        return heldHoverX = (int) v;
    }

    public static int getHoverY() {
        double v = getLastPos(false);
        if (heldHoverY != Integer.MIN_VALUE && Math.abs(v - heldHoverY) < HOVER_DEADBAND_PX) return heldHoverY;
        return heldHoverY = (int) v;
    }

    public static void addFreeLookDelta(double xrel, double yrel) {
        freeLookDeltaX += xrel;
        freeLookDeltaY += yrel;
    }

    public static double[] resetFreeLookDelta() {
        double[] result = {freeLookDeltaX, freeLookDeltaY};
        freeLookDeltaX = 0;
        freeLookDeltaY = 0;
        return result;
    }

    public static void grabMouseForFirstPerson() {
        Minecraft mc = Minecraft.getInstance();
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        if (!accessor.getMouseGrabbed()) {
            accessor.setMouseGrabbed(true);
            double centerX = mc.getWindow().getScreenWidth() / 2.0;
            double centerY = mc.getWindow().getScreenHeight() / 2.0;
            //? if >26.2 {
            InputConstants.grabMouse(mc.getWindow(), centerX, centerY);
            //?} else {
             /*InputConstants.grabOrReleaseMouse(mc.getWindow(), InputConstants.CURSOR_DISABLED, centerX, centerY);
            *///?}
            mc.mouseHandler.setIgnoreFirstMove();
        }
        weGrabbedMouse = true;
    }

    public static void releaseMouseFromFirstPerson() {
        if (!weGrabbedMouse) return;
        Minecraft mc = Minecraft.getInstance();
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        if (accessor.getMouseGrabbed()) {
            accessor.setMouseGrabbed(false);
            double centerX = mc.getWindow().getScreenWidth() / 2.0;
            double centerY = mc.getWindow().getScreenHeight() / 2.0;
            //? if >26.2 {
            InputConstants.releaseMouse(mc.getWindow(), centerX, centerY);
            //?} else {
             /*InputConstants.grabOrReleaseMouse(mc.getWindow(), InputConstants.CURSOR_NORMAL, centerX, centerY);
            *///?}
        }
        weGrabbedMouse = false;
    }

    public static void updateMouseGrabForFirstPerson(boolean isFirstPerson) {
        if (!SpatialGUI.config.useCrosshairForFirstPerson || !isFirstPerson) {
            releaseMouseFromFirstPerson();
        } else {
            grabMouseForFirstPerson();
        }
    }
}
