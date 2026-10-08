package dev.multispaceui.mixin.render;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the camera state of the current frame.
 *
 * <p>{@link LevelRenderer#render} receives the exact projection and view
 * rotation matrices of the frame (after FOV updates, bobbing and nausea have
 * been applied) plus the camera position. Capturing them here - once per
 * frame, at TAIL - is what lets the spatial pass project its panels with the
 * very same matrices the world was rendered with, so panel and world can never
 * drift apart.</p>
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V",
            at = @At("TAIL")
    )
    private void multispaceui$captureCamera(
            com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            com.mojang.renderpearl.api.buffers.GpuBufferSlice fogBuffer,
            org.joml.Vector4f fogColor,
            boolean renderWorld,
            boolean renderEntities,
            CallbackInfo ci
    ) {
        if (!MultiSpaceUI.isEnabled() || cameraState == null || !cameraState.initialized) {
            return;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            var window = mc.getWindow();
            SpatialWindowRenderer.instance.captureFrame(
                    cameraState.projectionMatrix,
                    cameraState.viewRotationMatrix,
                    cameraState.pos.x,
                    cameraState.pos.y,
                    cameraState.pos.z,
                    window.getWidth(),
                    window.getHeight()
            );
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Camera capture failed", t);
        }
    }
}
