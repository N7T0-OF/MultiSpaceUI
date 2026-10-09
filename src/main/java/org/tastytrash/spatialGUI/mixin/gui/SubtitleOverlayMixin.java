package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {
    @Unique
    private boolean wasSkipped;

    //? if >=26.1.2 {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
     *///?}
    private void spatialGUI$skipWindowOverrideSubtitles(CallbackInfo ci) {
        wasSkipped = SpatialGUIRenderer.skipWindowOverride;
        SpatialGUIRenderer.skipWindowOverride = true;
    }

    //? if >=26.1.2 {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    //?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
     *///?}
    private void spatialGUI$restoreWindowOverrideSubtitles(CallbackInfo ci) {
        SpatialGUIRenderer.skipWindowOverride = wasSkipped;
    }
}