package org.tastytrash.spatialGUI.mixin.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;import org.tastytrash.spatialGUI.client.SpatialGUIConfig;

//? if <=1.21.1 {
/*import net.minecraft.client.gui.components.DebugScreenOverlay;
@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideDebugOverlay(CallbackInfo ci) {
        if (SpatialGUIClient.isEnabled() && SpatialGUI.config.hudOptions.hideHud
                && SpatialGUIClient.renderer() != null
                && SpatialGUIClient.renderer().getHookedScreen() != null) {
            ci.cancel();
        }
    }
}
*///?} else {

@Mixin(net.minecraft.client.Minecraft.class)
public class DebugScreenOverlayMixin {}
//?}