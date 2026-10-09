package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.Minecraft;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if >1.20.1 && <26.1.2 {
/*import net.minecraft.client.gui.GuiGraphics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
*///?}


@Mixin(Screen.class)
public class ScreenMixin {
    //? if neoforge {
    /*@Inject(method = "added", at = @At("HEAD"))
    *///?} else {
    @Inject(method = "init()V", at = @At("HEAD"))
    //?}
    private void spatialGUI$hookOnShow(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUIClient.shouldHookScreen(screen)) {
            renderer.hookScreen(screen);
        }
    }

    //? if >=26.1.2 {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBackground(CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeTransparentBackground(CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBlur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractMenuBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeMenuBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }
    //?} else if >1.20.1 {
    /*@Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBackground(CallbackInfo ci) {
        if (SpatialGUIClient.renderer() != null && SpatialGUIClient.renderer().shouldCapture() && SpatialGUIClient.isEnabled()) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderTransparentBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"))
    private void spatialGUI$removeTransparentBackground(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, Operation<Void> original) {
        if (spatialGUI$isHooked()) return;
        original.call(graphics, x1, y1, x2, y2, colorFrom, colorTo);
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBlurredBackground(CallbackInfo ci) {
        if (SpatialGUIClient.renderer() != null && SpatialGUIClient.renderer().shouldCapture() && SpatialGUIClient.isEnabled()) {
            ci.cancel();
        }
    }
    *///?} else {
    /*@Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBackgroundOverlay(CallbackInfo ci) {
        if (SpatialGUIClient.renderer() != null && SpatialGUIClient.renderer().shouldCapture() && SpatialGUIClient.isEnabled()) {
            if (Minecraft.getInstance().level == null) return;
            ci.cancel();
        }
    }
    *///?}

    @Unique
    private boolean spatialGUI$isHooked() {
        var renderer = SpatialGUIClient.renderer();
        return renderer != null
                && SpatialGUIClient.isEnabled()
                && renderer.getHookedScreen() == (Object) this
                && renderer.shouldCapture();
    }
}