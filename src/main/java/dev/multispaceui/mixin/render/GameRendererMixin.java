package dev.multispaceui.mixin.render;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the spatial panels pass right after the vanilla frame.
 *
 * <p>{@link GameRenderer#render} draws the whole frame in this order: world
 * (level renderer) → GUI (GuiRenderer.render) → overlays. Injecting at TAIL
 * means the panels are extracted, flushed into their textures and drawn with
 * the camera of the frame that was just rendered, overlaying the HUD like any
 * other GUI element.</p>
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void multispaceui$spatialPass(CallbackInfo ci) {
        if (!MultiSpaceUI.isEnabled()) {
            return;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            SpatialWindowRenderer.instance.renderFrame(partialTick);
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Spatial window pass failed, skipping this frame", t);
        }
    }
}
