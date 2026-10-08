package dev.multispaceui.mixin.gui;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.rendering.WindowCapture;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Redirects the off-screen render target used while drawing the GUI.
 *
 * <p>{@code GuiRenderer.draw} always passes {@code GameRenderer.mainRenderTarget()}
 * to {@code executeDrawRange}. Every private renderer created by
 * {@link WindowCapture} is registered in {@code WindowCapture.TARGETS}, so for
 * those renderers the argument is swapped for the window's own
 * {@code TextureTarget} - each window then gets a texture containing its own
 * GUI. Vanilla renderers keep drawing to the main target because
 * {@link WindowCapture#targetOf} returns {@code null} for them.</p>
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @ModifyArg(
            method = "draw()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V"
            ),
            index = 1
    )
    private com.mojang.blaze3d.pipeline.RenderTarget multispaceui$redirectTarget(
            com.mojang.blaze3d.pipeline.RenderTarget target) {
        if (!MultiSpaceUI.isEnabled()) {
            return target;
        }
        try {
            Object ours = WindowCapture.targetOf((GuiRenderer) (Object) this);
            return ours != null ? (com.mojang.blaze3d.pipeline.RenderTarget) ours : target;
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Could not redirect the GUI render target", t);
            return target;
        }
    }
}
