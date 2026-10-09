package org.tastytrash.spatialGUI.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

//? if >1.21.1 {
import net.minecraft.client.renderer.fog.FogRenderer;
import static com.mojang.blaze3d.platform.Lighting.Entry.LEVEL;
//?}

//? if <26.1.2 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

//? if neoforge {

/*import net.neoforged.neoforge.client.ClientHooks;
*///?}

//? if >=26.1.2 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.tastytrash.spatialGUI.render.WorldBlurRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Final @Shadow private GameRenderState gameRenderState;
    @Final @Shadow private FogRenderer fogRenderer;
    @Shadow private net.minecraft.client.renderer.Projection hudProjection;
    @Shadow private net.minecraft.client.renderer.ProjectionMatrixBuffer hud3dProjectionMatrixBuffer;
    @Shadow @Final private net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow private boolean useUiLightmap;
    //? if >=26.2 {
    @Shadow @Final private net.minecraft.client.renderer.SubmitNodeStorage handAndScreenSubmitNodeStorage;
    //?} else {
    /*@Shadow @Final private net.minecraft.client.renderer.RenderBuffers renderBuffers;
    *///?}
    //? if <26.3 {
    /*@Shadow protected abstract void renderItemInHand(net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState, float partialTick, org.joml.Matrix4fc matrix4fc);
    *///?} else {
    @Shadow @Final private net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer firstPersonHandsAndItemsRenderer;
    @Shadow protected abstract void bobHurt(net.minecraft.client.renderer.state.level.CameraRenderState cameraState, com.mojang.blaze3d.vertex.PoseStack poseStack);
    @Shadow protected abstract void bobView(net.minecraft.client.renderer.state.level.CameraRenderState cameraState, com.mojang.blaze3d.vertex.PoseStack poseStack);
    //?}

    @Unique
    private boolean spatialGUI$isRedrawingHand = false;

    @Unique
    private void spatialGUI$renderHandsInFront() {
        Minecraft mc = Minecraft.getInstance();
        var cameraRenderState = this.gameRenderState.levelRenderState.cameraRenderState;
        if (cameraRenderState == null || !cameraRenderState.initialized) {
            return;
        }

        //? if <26.3 {
        /*int width = this.gameRenderState.windowRenderState.width;
        int height = this.gameRenderState.windowRenderState.height;
        this.hudProjection.setupPerspective(0.05F, 100.0F, cameraRenderState.hudFov, (float) width, (float) height);
        //? if >=26.2 {
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                this.hud3dProjectionMatrixBuffer.getBuffer(this.hudProjection),
                com.mojang.blaze3d.ProjectionType.PERSPECTIVE
        );
        var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(((GameRenderer) (Object) this).mainRenderTarget().getDepthTexture(), 0.0);
        com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD));
        ((GameRenderer) (Object) this).lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);
        //?} else {
        /^com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                this.hud3dProjectionMatrixBuffer.getBuffer(this.hudProjection),
                com.mojang.blaze3d.ProjectionType.PERSPECTIVE
        );
        com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD));
        var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), 1.0);
        ((GameRenderer) (Object) this).getLighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);
        ^///?}
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        boolean spatialGUI$prevUiLightmap = this.useUiLightmap;
        this.useUiLightmap = false;

        spatialGUI$isRedrawingHand = true;
        try {
            this.renderItemInHand(cameraRenderState, partialTick, cameraRenderState.viewRotationMatrix);
            spatialGUI$renderFeaturesToMainTarget("Item in hand in front of SpatialGUI");
        } finally {
            this.useUiLightmap = spatialGUI$prevUiLightmap;
            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            spatialGUI$isRedrawingHand = false;
        }
        *///?} else {
        var playerState = this.gameRenderState.levelRenderState.playerRenderState;
        if (!playerState.hasPlayer || playerState.firstPersonHandsAndItems == null) {
            return;
        }

        boolean spatialGUI$prevUiLightmap = this.useUiLightmap;
        this.useUiLightmap = false;

        spatialGUI$isRedrawingHand = true;
        try {
            int width = this.gameRenderState.windowRenderState.width;
            int height = this.gameRenderState.windowRenderState.height;
            this.hudProjection.setupPerspective(0.05F, cameraRenderState.depthFar, cameraRenderState.hudFov, (float) width, (float) height);
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                    this.hud3dProjectionMatrixBuffer.getBuffer(this.hudProjection),
                    com.mojang.blaze3d.ProjectionType.PERSPECTIVE
            );

            var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
            encoder.clearDepthTexture(((GameRenderer) (Object) this).mainRenderTarget().getDepthTexture(), 0.0);

            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD));
            ((GameRenderer) (Object) this).lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);

            var poseStack = new com.mojang.blaze3d.vertex.PoseStack();
            poseStack.pushPose();
            poseStack.mulPose(cameraRenderState.viewRotationMatrix.invert(new org.joml.Matrix4f()));
            var modelViewStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix().mul(cameraRenderState.viewRotationMatrix);
            this.bobHurt(cameraRenderState, poseStack);
            if (this.gameRenderState.optionsRenderState.bobView) {
                this.bobView(cameraRenderState, poseStack);
            }

            this.firstPersonHandsAndItemsRenderer.submitHandsWithItems(
                    cameraRenderState.cameraEntityPartialTicks,
                    poseStack,
                    this.handAndScreenSubmitNodeStorage,
                    playerState,
                    playerState.firstPersonHandsAndItems
            );

            spatialGUI$renderFeaturesToMainTarget("Item in hand in front of SpatialGUI");

            modelViewStack.popMatrix();
            poseStack.popPose();
        } finally {
            this.useUiLightmap = spatialGUI$prevUiLightmap;
            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            spatialGUI$isRedrawingHand = false;
        }
        //?}
    }

    @Unique
    private void spatialGUI$renderPlayerInFront() {
        var deferredState = WorldBlurRenderer.getDeferredPlayerRenderState();
        if (deferredState == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        var cameraRenderState = this.gameRenderState.levelRenderState.cameraRenderState;
        if (cameraRenderState == null || !cameraRenderState.initialized) {
            return;
        }
        var renderer = SpatialGUIClient.renderer();

        var prevProjectionBuffer = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrixBuffer();
        var prevProjectionType = com.mojang.blaze3d.systems.RenderSystem.getProjectionType();
        var modelViewStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            if (renderer.getCapturedProjectionBuffer() != null && renderer.getCapturedProjectionType() != null) {
                com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(renderer.getCapturedProjectionBuffer(), renderer.getCapturedProjectionType());
            }
            modelViewStack.mul(cameraRenderState.viewRotationMatrix);
            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD));

            //? if >=26.2 {
            ((GameRenderer) (Object) this).lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);
             //?} else {
            /*((GameRenderer) (Object) this).getLighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);
            *///?}

            var camPos = cameraRenderState.pos;
            WorldBlurRenderer.setDeferredPlayerRenderState(null);
            mc.getEntityRenderDispatcher().submit(deferredState, cameraRenderState,
                    deferredState.x - camPos.x, deferredState.y - camPos.y, deferredState.z - camPos.z,
                    new com.mojang.blaze3d.vertex.PoseStack(),
                    //? if >=26.2 {
                    this.handAndScreenSubmitNodeStorage
                     //?} else {
                    /*mc.gameRenderer.getSubmitNodeStorage()
                    *///?}
            );

            spatialGUI$renderFeaturesToMainTarget("Spatial GUI unblurred player");
        } finally {
            modelViewStack.popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(prevProjectionBuffer, prevProjectionType);
        }
    }

    @Unique
    private void spatialGUI$renderFeaturesToMainTarget(String passName) {
        //? if >=26.3 {
        var frame = this.featureRenderDispatcher.prepareFrame(this.handAndScreenSubmitNodeStorage);
        try {
            var mainTarget = ((GameRenderer) (Object) this).mainRenderTarget();
            try (var renderPass = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> passName,
                    mainTarget.getColorTextureView(),
                    java.util.Optional.empty(),
                    mainTarget.getDepthTextureView(),
                    java.util.OptionalDouble.empty()
            )) {
                com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(renderPass);
                net.minecraft.client.renderer.feature.FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
            }
        } finally {
            if (frame != null) {
                frame.close();
            }
        }
         //?} else {
        /*//? if >=26.2 {
        this.featureRenderDispatcher.renderAllFeatures(this.handAndScreenSubmitNodeStorage);
         //?} else {
        /^this.featureRenderDispatcher.renderAllFeatures();
        this.renderBuffers.bufferSource().endBatch();
        ^///?}
        *///?}
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$prepareTargetEarly(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.updateFovProgress();
        }
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            renderer.prepareTarget();
            renderer.onFrameStart();
        }
    }

    @Inject(method = "processBlurEffect()V", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$cancelBlurEffect(CallbackInfo ci) {
        if (WorldBlurRenderer.isApplyingBlur()) {
            return;
        }
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            ci.cancel();
        }
    }

    //? if >=26.2 {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
     //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    *///?}
    private void spatialGUI$beforeGuiRender(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();

        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            this.gameRenderState.windowRenderState.guiScale = Minecraft.getInstance().getWindow().getGuiScale();

            //? if >=26.2 {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            renderer.getScreenGuiRenderer().render();
             //?} else {
            /*renderer.getScreenGuiRenderer().render(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            *///?}
            renderer.getScreenGuiRenderer().endFrame();

            if (SpatialGUI.config.blurWorldBackground) {
                WorldBlurRenderer.applyBlur(SpatialGUI.config.worldBlurStrength);
                spatialGUI$renderPlayerInFront();
            }

            renderer.renderInWorldPost();
            renderer.clearTarget();

            if (SpatialGUIClient.getEffectiveFirstPersonMode() && !SpatialGUI.config.firstPersonHands.hideHandsInFirstPerson) {
                spatialGUI$renderHandsInFront();
            }

            //? if >=26.2 {
            this.gameRenderState.guiRenderState.isHudHidden = false;
             //?}

            SpatialGUIRenderer.skipWindowOverride = true;
            this.gameRenderState.windowRenderState.guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        }
    }

    //? if >=26.2 {
    @Inject(method = "extract", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "extractGui", at = @At("HEAD"))
    *///?}
    private void spatialGUI$refreshMousePosition(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.updateMousePosition();
        }
    }

    //? if fabric && <26.2 {
    /*@ModifyArg(method = "extractGui", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
    ), index = 0)
    private GuiGraphicsExtractor spatialGUI$extractScreenIntoIsolatedState(GuiGraphicsExtractor graphics) {
        var renderer = SpatialGUIClient.renderer();

        if (renderer != null && SpatialGUIClient.isEnabled() && renderer.shouldCapture()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            SpatialGUIRenderer.isExtractingScreen = true;
            return renderer.createIsolatedGraphics();
        }
        return graphics;
    }

    @Inject(method = "extractGui", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            shift = At.Shift.AFTER
    ))
    private void spatialGUI$afterScreenExtraction(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUI.config.enabled && renderer.shouldCapture()) {
            SpatialGUIRenderer.isExtractingScreen = false;
            SpatialGUIRenderer.skipWindowOverride = true;
        }
    }
    *///?}

    @Inject(method = "render", at = @At("TAIL"))
    private void spatialGUI$renderIsolatedScreen(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            this.gameRenderState.windowRenderState.guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        }
    }

    //? if <26.3 {
    /*@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$overrideHideHand(CallbackInfo ci) {
        if (this.spatialGUI$isRedrawingHand) {
            return;
        }
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            //? if >=26.2 {
            this.gameRenderState.guiRenderState.isHudHidden = false;
             //?}

            if (SpatialGUIClient.getEffectiveFirstPersonMode()) {
                ci.cancel();
            }
        }
    }
    *///?} else {
    @Inject(method = "render3dHud", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$overrideHideHand(CallbackInfo ci) {
        if (this.spatialGUI$isRedrawingHand) {
            return;
        }
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            this.gameRenderState.guiRenderState.isHudHidden = false;

            if (SpatialGUIClient.getEffectiveFirstPersonMode()) {
                ci.cancel();
            }
        }
    }
    //?}
}
//?} else if >1.21.1 {
/*@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Final @Shadow private FogRenderer fogRenderer;
    @Final @Shadow private net.minecraft.client.renderer.RenderBuffers renderBuffers;
    @Final @Shadow private net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher;
    @Final @Shadow private net.minecraft.client.Camera mainCamera;
    @Final @Shadow private net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer hud3dProjectionMatrixBuffer;
    @Shadow @Final private Lighting lighting;

    @Shadow protected abstract float getFov(net.minecraft.client.Camera camera, float partialTick, boolean useFovSetting);
    @Shadow protected abstract void renderItemInHand(float partialTick, boolean flag, org.joml.Matrix4f matrix4f);

    @Unique
    private boolean spatialGUI$isRedrawingHand = false;

    @Unique
    private void spatialGUI$renderHandsInFront() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (this.mainCamera == null || !this.mainCamera.isInitialized()) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        float fov = this.getFov(this.mainCamera, partialTick, false);

        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                this.hud3dProjectionMatrixBuffer.getBuffer(width, height, fov),
                com.mojang.blaze3d.ProjectionType.PERSPECTIVE
        );

        var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), 1.0);
        this.lighting.setupFor(LEVEL);
        com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.WORLD));

        org.joml.Matrix4f rotationMatrix = new org.joml.Matrix4f().rotation(
                this.mainCamera.rotation().conjugate(new org.joml.Quaternionf())
        );

        this.spatialGUI$isRedrawingHand = true;
        try {
            this.renderItemInHand(partialTick, false, rotationMatrix);
            this.featureRenderDispatcher.renderAllFeatures();
            this.renderBuffers.bufferSource().endBatch();
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            this.spatialGUI$isRedrawingHand = false;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$prepareTargetEarly(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.updateFovProgress();
        }
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            renderer.prepareTarget();
            renderer.onFrameStart();
        }
    }

    @Inject(method = "processBlurEffect()V", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$cancelBlurEffect(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void spatialGUI$beforeGuiRender(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();

        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            SpatialGUIRenderer.skipWindowOverride = false;

            com.mojang.blaze3d.systems.RenderSystem.setShaderFog(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            renderer.getScreenGuiRenderer().render(this.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            renderer.getScreenGuiRenderer().incrementFrameNumber();

            renderer.renderInWorldPost();
            renderer.clearTarget();

            if (SpatialGUIClient.getEffectiveFirstPersonMode() && !SpatialGUI.config.firstPersonHands.hideHandsInFirstPerson) {
                spatialGUI$renderHandsInFront();
            }

            SpatialGUIRenderer.skipWindowOverride = true;
        }
    }
    //? if fabric {
    @ModifyArg(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ), index = 0)
    private GuiGraphics spatialGUI$extractScreenIntoIsolatedState(GuiGraphics graphics) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUIClient.isEnabled() && renderer.shouldCapture()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            SpatialGUIRenderer.isExtractingScreen = true;
            return renderer.createIsolatedGraphics();
        }
        return graphics;
    }

    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = At.Shift.AFTER
    ))
    private void spatialGUI$afterScreenExtraction(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUI.config.enabled && renderer.shouldCapture()) {
            SpatialGUIRenderer.isExtractingScreen = false;
            SpatialGUIRenderer.skipWindowOverride = true;
        }
    }
    //?} else {
    /^@Redirect(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ))
    private void spatialGUI$redirectScreenExtraction(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var renderer = SpatialGUIClient.renderer();
        if (SpatialGUIClient.isEnabled() && screen instanceof AbstractContainerScreen<?> && screen == renderer.getHookedScreen()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            renderer.extractIsolatedScreen(screen, partialTick);
            SpatialGUIRenderer.skipWindowOverride = true;
        } else {
            net.neoforged.neoforge.client.ClientHooks.drawScreen(screen, graphics, mouseX, mouseY, partialTick);
        }
    }
    ^///?}
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float spatialGUI$overrideFov(float original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!useFovSetting) return original;

        var renderer = SpatialGUIClient.renderer();
        if (renderer == null) return original;

        float factor = renderer.getFovOverrideFactor();
        if (factor <= 0f) return original;

        return Mth.lerp(factor, original, (float) SpatialGUI.config.targetFov);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$refreshMousePosition(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.updateMousePosition();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void spatialGUI$renderIsolatedScreen(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            SpatialGUIRenderer.skipWindowOverride = false;
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$overrideHideHand(CallbackInfo ci) {
        if (this.spatialGUI$isRedrawingHand) {
            return;
        }
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            if (SpatialGUIClient.getEffectiveFirstPersonMode()) {
                ci.cancel();
            }
        }
    }
}
*///?} else {
/*@Mixin(value = GameRenderer.class, priority = 1100)
public class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double spatialGUI$overrideFov(double original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!useFovSetting) return original;

        var renderer = SpatialGUIClient.renderer();
        if (renderer == null) return original;

        float factor = renderer.getFovOverrideFactor();
        if (factor <= 0f) return original;

        return Mth.lerp(factor, original, SpatialGUI.config.targetFov);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$prepareTargetEarly(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.updateFovProgress();
        }
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            renderer.prepareTarget();
            renderer.onFrameStart();
        }
        SpatialGUIRenderer.skipWindowOverride = true;
    }

    //? if 1.21.1 {
    /^@Inject(method = "processBlurEffect(F)V", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$cancelBlurEffect(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            ci.cancel();
        }
    }
    ^///?}

    //? if 1.21.1 {
    /^@Inject(method = "render", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    ^///?} else {
    @Inject(method = "render", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    //?}
    private void spatialGUI$drawBeforeHud(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            renderer.renderInWorldPost();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    private void spatialGUI$suppressPostEffect(net.minecraft.client.renderer.PostChain postChain, float partialTick) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            return;
        }
        postChain.process(partialTick);
    }

    //? if fabric {
    @WrapOperation(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ))
    private void spatialGUI$wrapScreenRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> operation) {
        var renderer = SpatialGUIClient.renderer();
        if (SpatialGUIClient.isEnabled() && SpatialGUIClient.shouldHookScreen(screen) && screen == renderer.getHookedScreen()) {
            SpatialGUIRenderer.skipWindowOverride = false;
            renderer.extractIsolatedScreen(screen, partialTick, operation);
            SpatialGUIRenderer.skipWindowOverride = true;
            renderer.renderInWorldPost();
        } else {
            operation.call(screen, graphics, mouseX, mouseY, partialTick);
        }
    }
    //?} else {
    /^@Redirect(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ))
    private void spatialGUI$redirectScreenExtraction(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var renderer = SpatialGUIClient.renderer();
        if (SpatialGUIClient.isEnabled() && SpatialGUIClient.shouldHookScreen(screen) && screen == renderer.getHookedScreen()) {
            Operation<Void> operation = args -> {
                ClientHooks.drawScreen((Screen) args[0], (GuiGraphics) args[1], (int) args[2], (int) args[3], (float) args[4]);
                return null;
            };
            SpatialGUIRenderer.skipWindowOverride = false;
            renderer.extractIsolatedScreen(screen, partialTick, operation);
            SpatialGUIRenderer.skipWindowOverride = true;
            renderer.renderInWorldPost();
        } else {
            ClientHooks.drawScreen(screen, graphics, mouseX, mouseY, partialTick);
        }
    }
    ^///?}

    @Inject(method = "render", at = @At("TAIL"))
    private void spatialGUI$endRender(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            SpatialGUIRenderer.skipWindowOverride = true;
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$overrideHideHand(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer.shouldCapture() && SpatialGUIClient.isEnabled()) {
            renderer.renderInWorldPost();
            if (SpatialGUIClient.getEffectiveFirstPersonMode() && SpatialGUI.config.firstPersonHands.hideHandsInFirstPerson) {
                ci.cancel();
            }
        }
    }
}
*///?}
