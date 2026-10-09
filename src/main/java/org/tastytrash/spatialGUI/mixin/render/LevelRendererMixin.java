package org.tastytrash.spatialGUI.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if <26.1.2 && >1.20.1 {
/*import com.llamalad7.mixinextras.sugar.Local;
 *///?}

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    //? if >=26.2 {
    @Inject(method = "render", at = @At("TAIL"))
     //?} else {
    /*@Inject(method = "renderLevel", at = @At("TAIL"))
            *///?}
    private void spatialGUI$renderScreen(CallbackInfo ci
            //? if <26.1.2 && >1.20.1 {
            /*, @Local(argsOnly = true, ordinal = 0) Matrix4f modelViewMatrix
             *///?}
    ) {
        var renderer = SpatialGUIClient.renderer();
        if (!SpatialGUIClient.isEnabled() || renderer == null || !renderer.shouldCapture()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        //? if >=26.1.2 {
        //? if >=26.2 {
        var camera = Minecraft.getInstance().gameRenderer.mainCamera();
         //?} else {
        /*var camera = Minecraft.getInstance().gameRenderer.mainCamera();
        *///?}
        poseStack.mulPose(new Quaternionf()
                .rotateX((float) Math.toRadians(camera.xRot()))
                .rotateY((float) Math.toRadians(camera.yRot() + 180.0f))
                .get(new Matrix4f())
        );
        //?} else if >1.20.1 {
        /*poseStack.mulPose(modelViewMatrix);
         *///?} else {
        /*// 1.20.1: the only Matrix4f arg to renderLevel is the projection matrix,
        // so rebuild the camera rotation from the Camera instead.
        var camera = Minecraft.getInstance().gameRenderer.mainCamera();
        poseStack.mulPose(new Quaternionf()
                .rotateX((float) Math.toRadians(camera.xRot()))
                .rotateY((float) Math.toRadians(camera.yRot() + 180.0f))
        );
        *///?}

        //? if >1.21.1 {
        renderer.capturePerspectiveState(
                RenderSystem.getProjectionMatrixBuffer(),
                RenderSystem.getProjectionType(),
                poseStack
        );
        //?} else {
        /*renderer.capturePerspectiveState(
                RenderSystem.getProjectionMatrix(),
                RenderSystem.getVertexSorting(),
                poseStack
        );
        *///?}
    }
}