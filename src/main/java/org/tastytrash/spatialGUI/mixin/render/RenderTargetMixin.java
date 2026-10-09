package org.tastytrash.spatialGUI.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

/**
 * Some mods rebind the main framebuffer mid-gui-render (e.g. Accessories'
 * hover-highlight PostEffectBuffer), which would hijack the rest of our
 * capture into 2D. Main-target binds are blocked for the duration, the
 * extractor rebinds it itself once the capture is done
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    //? if <=1.21.1 {
    /*@Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$blockMainBindDuringCapture(boolean setViewport, CallbackInfo ci) {
        if (SpatialGUIRenderer.isExtractingScreen && (Object) this == Minecraft.getInstance().getMainRenderTarget()) {
            ci.cancel();
        }
    }
    *///?}
}
