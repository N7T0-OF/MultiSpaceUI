package org.tastytrash.spatialGUI.mixin.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if >=26.3 {
import org.lwjgl.sdl.SDLMouse;
//?} else {
/*import org.lwjgl.glfw.GLFW;
*///?}

@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Unique
    private static boolean spatialGUI$shouldKeepMouseGrabbed() {
        if (!SpatialGUIClient.isEnabled() || !SpatialGUI.config.useCrosshairForFirstPerson) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        //? if >=26.2 {
        Screen screen = mc.gui.screen();
        //?} else {
        /*Screen screen = mc.screen;
        *///?}
        if (screen == null || !SpatialGUIClient.shouldHookScreen(screen)) return false;

        return SpatialGUIClient.shouldUseFirstPersonMode(screen);
    }

    //? if <=1.21.1 {
    /*@Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
    private static void spatialGUI$cancelRelease(long window, int cursorValue, double x, double y, CallbackInfo ci) {
        if (cursorValue != GLFW.GLFW_CURSOR_NORMAL) return;
        if (GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED) return;
        if (spatialGUI$shouldKeepMouseGrabbed()) ci.cancel();
    }
    *///?} else if <26.3 {
    /*@Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
    private static void spatialGUI$cancelRelease(com.mojang.blaze3d.platform.Window window, int cursorValue, double x, double y, CallbackInfo ci) {
        if (cursorValue != GLFW.GLFW_CURSOR_NORMAL) return;
        if (GLFW.glfwGetInputMode(window.handle(), GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED) return;
        if (spatialGUI$shouldKeepMouseGrabbed()) ci.cancel();
    }
    *///?} else {
    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private static void spatialGUI$cancelRelease(com.mojang.blaze3d.platform.Window window, double x, double y, CallbackInfo ci) {
        if (!SDLMouse.SDL_GetWindowRelativeMouseMode(window.handle())) return;
        if (spatialGUI$shouldKeepMouseGrabbed()) ci.cancel();
    }
    //?}
}
