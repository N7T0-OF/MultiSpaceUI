package org.tastytrash.spatialGUI.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.render.WorldBlurRenderer;

@Mixin(EntityRenderDispatcher.class)
//?} else {
/*@Mixin(EntityRenderDispatcher.class)
*///?}
public class EntityRenderDispatcherMixin {
    //? if >=26.1.2 {
    @Inject(method = "extractEntity", at = @At("TAIL"))
    private void spatialGUI$deferLocalPlayerRenderState(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player) {
            return;
        }

        var renderer = SpatialGUIClient.renderer();
        if (renderer == null || !SpatialGUI.config.enabled || !SpatialGUI.config.blurWorldBackground) {
            WorldBlurRenderer.setDeferredPlayerRenderState(null);
            return;
        }

        if (SpatialGUIRenderer.isExtractingScreen) {
            return;
        }

        boolean defer = !SpatialGUIClient.getEffectiveFirstPersonMode() && renderer.shouldCapture();
        WorldBlurRenderer.setDeferredPlayerRenderState(defer ? cir.getReturnValue() : null);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$skipLocalPlayerSubmit(EntityRenderState renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector output, CallbackInfo ci) {
        var deferred = WorldBlurRenderer.getDeferredPlayerRenderState();
        if (deferred != null && renderState == deferred) {
            ci.cancel();
        }
    }
    //?}
}
