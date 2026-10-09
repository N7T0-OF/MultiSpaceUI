package org.tastytrash.spatialGUI.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;

@Mixin(value = MouseHandler.class, priority = 1100)
public class MouseHandlerMixin {

    //? if <=1.21.1 {
    /*@Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    *///?}

    @Unique
    private static boolean shouldApplyMouseOverride() {
        if (!SpatialGUIClient.isEnabled()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        //? if >=26.2 {
        Screen screen = client.gui.screen();
         //?} else {
        /*Screen screen = client.gui.screen();
        *///?}
        if (SpatialGUIClient.shouldHookScreen(screen)) return true;
        var renderer = SpatialGUIClient.renderer();
        return renderer != null && SpatialGUIClient.shouldHookScreen(renderer.getHookedScreen());
    }

    //? if >1.21.1 {
    @ModifyReturnValue(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"))
    private double spatialGUI$overrideScaledX(double original) {
        return shouldApplyMouseOverride() ? MouseHandlerUtil.getLastPos(true, original) : original;
    }

    @ModifyReturnValue(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"))
    private double spatialGUI$overrideScaledY(double original) {
        return shouldApplyMouseOverride() ? MouseHandlerUtil.getLastPos(false, original) : original;
    }
    //?} else {
    /*@Unique
    private static double spatialGUI$toRaw(boolean x, double scaled) {
        var w = Minecraft.getInstance().getWindow();
        return x ? scaled * w.getScreenWidth()  / (double) w.getGuiScaledWidth()
                : scaled * w.getScreenHeight() / (double) w.getGuiScaledHeight();
    }

    @ModifyExpressionValue(method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;xpos:D"))
    private double spatialGUI$overrideX(double original) {
        return shouldApplyMouseOverride()
                ? spatialGUI$toRaw(true, MouseHandlerUtil.getLastPos(true, original))
                : original;
    }

    @ModifyExpressionValue(method = {"onPress", "onScroll", "handleAccumulatedMovement"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;ypos:D"))
    private double spatialGUI$overrideY(double original) {
        return shouldApplyMouseOverride()
                ? spatialGUI$toRaw(false, MouseHandlerUtil.getLastPos(false, original))
                : original;
    }
    *///?}

    @WrapMethod(method = "onMove")
    //? if >=26.3 {
    private void spatialGUI$onMove(long handle, double x, double y, double xrel, double yrel, Operation<Void> original) {
     //?} else {
    /*private void spatialGUI$onMove(long handle, double x, double y, Operation<Void> original) {
    *///?}
        Minecraft mc = Minecraft.getInstance();
        //? if >1.21.1 {
        if (handle != mc.getWindow().handle()) return;
        //?} else {
        /*if (handle != mc.getWindow().getWindow()) return;
         *///?}

        MouseHandlerAccessor acc = (MouseHandlerAccessor) this;
        double[] delta = MouseHandlerUtil.captureMove(x, y, acc.getMouseGrabbed());
        //? if >=26.3 {
        delta[0] = xrel;
        delta[1] = yrel;
        //?}

        double outX = x;
        double outY = y;

        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && shouldApplyMouseOverride()) {
            if (SpatialGUIRenderer.isCrosshairModeActive() && !acc.getIgnoreFirstMove()) {
                MouseHandlerUtil.addFreeLookDelta(delta[0], delta[1]);
            }

            Vector2d mouse = renderer.updateMousePosition(
                    MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY());
            if (mouse != null) {
                outX = mouse.x;
                outY = mouse.y;
            } else {
                outX = acc.getRawXpos();
                outY = acc.getRawYpos();
            }
        }

        //? if >=26.3 {
        original.call(handle, outX, outY, xrel, yrel);
         //?} else {
        /*original.call(handle, outX, outY);
        *///?}
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    //? if >1.20.1 {
    private void spatialGUI$cancelPlayerRotation(double mousea, CallbackInfo ci) {
    //?} else {
    /*private void spatialGUI$cancelPlayerRotation(CallbackInfo ci) {
    *///?}
        if (SpatialGUIRenderer.isCrosshairModeActive()) {
            //? if <=1.21.1 {
            /*if (!shouldApplyMouseOverride()) {
                MouseHandlerUtil.addFreeLookDelta(this.accumulatedDX, this.accumulatedDY);
            }
            this.accumulatedDX = 0.0;
            this.accumulatedDY = 0.0;
            *///?}
            ci.cancel();
        }
    }

    @Unique
    private double spatialGUI$preReleaseX;

    @Unique
    private double spatialGUI$preReleaseY;

    @Inject(method = "releaseMouse", at = @At("HEAD"))
    private void spatialGUI$capturePreRelease(CallbackInfo ci) {
        var spatialGUI$acc = (MouseHandlerAccessor) (Object) this;
        spatialGUI$preReleaseX = spatialGUI$acc.getRawXpos();
        spatialGUI$preReleaseY = spatialGUI$acc.getRawYpos();
    }

    @Unique
    private static boolean spatialGUI$crosshairIncoming() {
        return SpatialGUI.config.useCrosshairForFirstPerson && SpatialGUIClient.getEffectiveFirstPersonMode();
    }

    @Unique
    private static Screen spatialGUI$currentScreen(Minecraft spatialGUI$mc) {
        //? if >=26.2 {
        return spatialGUI$mc.gui.screen();
         //?} else {
        /*return spatialGUI$mc.screen;
        *///?}
    }

    @Inject(method = "releaseMouse", at = @At("TAIL"))
    private void spatialGUI$restoreCursorFields(CallbackInfo ci) {
        Minecraft spatialGUI$mc = Minecraft.getInstance();
        Screen spatialGUI$screen = spatialGUI$currentScreen(spatialGUI$mc);
        if (spatialGUI$crosshairIncoming() || spatialGUI$screen == null || !SpatialGUIClient.shouldHookScreen(spatialGUI$screen)) return;
        var spatialGUI$acc = (MouseHandlerAccessor) (Object) this;
        spatialGUI$acc.setRawXpos(spatialGUI$preReleaseX);
        spatialGUI$acc.setRawYpos(spatialGUI$preReleaseY);
    }
}
