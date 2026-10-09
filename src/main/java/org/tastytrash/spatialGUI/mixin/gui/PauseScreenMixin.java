package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    //? if >=26.1.2 {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBlur(CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }
    //?} else if >1.20.1 {
    /*@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$removeBlur(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (spatialGUI$isHooked()) {
            ci.cancel();
        }
    }
    *///?}

    @Unique
    private boolean spatialGUI$isHooked() {
        var renderer = SpatialGUIClient.renderer();
        return SpatialGUIRenderer.isExtractingScreen
                && renderer != null
                && renderer.getHookedScreen() == (Object) this;
    }
}