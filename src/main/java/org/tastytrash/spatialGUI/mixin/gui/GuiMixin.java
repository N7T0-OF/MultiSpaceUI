package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void spatialGUI$beginExtract(CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = true;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void spatialGUI$endExtract(CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = false;
    }

    // swap the graphics so wrappers of this call (e.g. Architectury events REI uses)
    // draw onto the spatial screen
    //? if fabric && >=26.2 {
    @ModifyArg(method = "extractRenderState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
    ), index = 0)
    private GuiGraphicsExtractor spatialGUI$extractScreenIntoIsolatedState(GuiGraphicsExtractor graphics) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUIClient.isEnabled() && renderer.shouldCapture()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            SpatialGUIRenderer.isExtractingScreen = true;
            return renderer.createIsolatedGraphics();
        }
        return graphics;
    }

    @Inject(method = "extractRenderState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            shift = At.Shift.AFTER
    ))
    private void spatialGUI$afterScreenExtraction(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUI.config.enabled && renderer.shouldCapture()) {
            SpatialGUIRenderer.isExtractingScreen = false;
            SpatialGUIRenderer.skipWindowOverride = true;
        }
    }
    //? }
}
//?} else if >=1.21.1 {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$beginGuiRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void spatialGUI$endGuiRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = false;
    }
}
*///?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$beginGuiRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void spatialGUI$endGuiRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = false;
    }
}
*///?}