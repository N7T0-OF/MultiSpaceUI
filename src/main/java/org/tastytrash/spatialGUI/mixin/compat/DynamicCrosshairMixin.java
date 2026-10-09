package org.tastytrash.spatialGUI.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Pseudo
@Mixin(targets = "mod.crend.dynamiccrosshair.component.CrosshairHandler", remap = false)
public class DynamicCrosshairMixin {

    @ModifyReturnValue(method = "shouldShowCrosshair", at = @At("RETURN"), remap = false, require = 0)
    private static boolean spatialGUI$forceShowForSpatialScreen(boolean original) {
        if (original) return true;
        if (!SpatialGUIClient.isEnabled()) return false;
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
        if (renderer == null) return false;
        var hooked = renderer.getHookedScreen();
        //? if >=26.2 {
        return hooked != null && hooked == Minecraft.getInstance().gui.screen();
         //?} else {
        /*return hooked != null && hooked == Minecraft.getInstance().screen;
        *///?}
    }
}
