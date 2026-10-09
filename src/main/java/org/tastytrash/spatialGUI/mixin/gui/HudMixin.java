package org.tastytrash.spatialGUI.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if >=26.2 {
@Mixin(net.minecraft.client.gui.Hud.class)
//?} else {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?}
public class HudMixin {
    //? if >=26.1.2 {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
     *///?}
    private void spatialGUI$hideCrosshair(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideCrosshair()) {
            ci.cancel();
        }
    }

    //? if 1.21.11 && neoforge {
    /*@Inject(method = {"renderHotbar", "renderItemHotbar", "renderHealthLevel", "renderArmorLevel",
            "renderFoodLevel", "renderAirLevel", "renderVehicleHealth", "renderContextualInfoBarBackground",
            "renderExperienceLevel", "renderContextualInfoBar", "renderSelectedItemName", "renderEffects", "renderBossOverlay"},
            at = @At("HEAD"), cancellable = true)
    *///?} else if >=26.1.2 {
    //? if neoforge {
    /*@Inject(method = {"extractHotbar", "extractHealthLevel", "extractVehicleHealth",
            "extractContextualInfoBarBackground", "extractExperienceLevel", "extractContextualInfoBar",
            "maybeExtractSelectedItemName", "maybeExtractSpectatorTooltip", "extractEffects", "extractBossOverlay"},
            at = @At("HEAD"), cancellable = true)
    *///?} else {
    @Inject(method = {"extractHotbarAndDecorations", "extractEffects", "extractBossOverlay"},
            at = @At("HEAD"), cancellable = true)
    //?}
    //?} else if >1.21.1 {
    /*@Inject(method = {"renderHotbarAndDecorations", "renderEffects", "renderBossOverlay"},
            at = @At("HEAD"), cancellable = true)
    *///?} else if >1.20.1 {
    /*@Inject(method = {"renderItemHotbar", "renderPlayerHealth", "renderVehicleHealth",
            "renderExperienceBar", "renderExperienceLevel", "renderJumpMeter", "renderSelectedItemName"},
            at = @At("HEAD"), cancellable = true, require = 0)
    *///?} else {
    /*@Inject(method = {"renderHotbar", "renderPlayerHealth", "renderVehicleHealth",
            "renderExperienceBar", "renderJumpMeter", "renderSelectedItemName"},
            at = @At("HEAD"), cancellable = true, require = 0)
    *///?}
    private void spatialGUI$hideHotbar(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideHotbar()) {
            ci.cancel();
        }
    }

    //? if 1.21.11 && neoforge {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideHud(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideHud()) {
            ci.cancel();
        }
    }
    *///?} else if >=26.1.2 {
    //? if neoforge {
    /*@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideHud(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideHud()) {
            ci.cancel();
        }
    }
    *///?} else {
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD",
    //? if >=26.2 {
    target = "Lnet/minecraft/client/gui/Hud;isHidden:Z"))
    //?} else {
    /*target = "Lnet/minecraft/client/Options;hideGui:Z"))
    *///?}
    private boolean spatialGUI$hideHud(boolean original) {
        return original || SpatialGUIClient.shouldHideHud();
    }
    //?}
    //?} else if >1.21.1 {
    /*@ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z"))
    private boolean spatialGUI$hideHud(boolean original) {
        return original || SpatialGUIClient.shouldHideHud();
    }
     
    *///?} else if 1.21.1 {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideHud(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideHud()) {
            org.tastytrash.spatialGUI.render.SpatialGUIRenderer.skipWindowOverride = false;
            ci.cancel();
        }
    }
    *///?} else {
    /*@ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z"))
    private boolean spatialGUI$hideHud(boolean original) {
        return original || SpatialGUIClient.shouldHideHud();
    }
    *///?}

    //? if >=26.1.2 {
    @Inject(method = {"extractDebugOverlay", "extractDeferredSubtitles"}, at = @At("HEAD"), cancellable = true)
    //?} else if >1.21.1 {
    /*@Inject(method = {"renderDebugOverlay", "renderSubtitleOverlay"}, at = @At("HEAD"), cancellable = true)
    *///?}
    //? if >1.21.1 {
    private void spatialGUI$hideHudOverlays(CallbackInfo ci) {
        if (SpatialGUIClient.shouldHideHud()) {
            ci.cancel();
        }
    }
    //?}
}
