package org.tastytrash.spatialGUI.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.mixin.gui.MouseHandlerAccessor;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.util.AnimationUtil;
import org.tastytrash.spatialGUI.util.MathUtil;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;
import org.tastytrash.spatialGUI.util.CameraUtil;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Entity entity;
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Final @Shadow private Quaternionf rotation;

    @Unique private static Vec3 startPos;
    @Unique private static float startYRot;
    @Unique private static float startXRot;
    @Unique private static long transitionStartTime, TRANSITION_DURATION_MS = 1;
    @Unique private static boolean wasCapturing, isTransitioning;
    @Unique private static final float MAX_YAW_OFFSET = 90f;
    @Unique private static float smoothedCameraYaw, smoothedCameraPitch;
    @Unique private static float baseXRot;

    @Unique private static float parallaxYawOffset, parallaxPitchOffset;
    @Unique private static long lastParallaxNanos;
    @Unique private static final float PARALLAX_TAU_MS = 50.0f;

    @Unique private static float freeLookYaw = 0f, freeLookPitch = 0f;
    @Unique private static boolean wasCrosshairMode;

    @Unique
    private record CameraTransform(Vec3 pos, float yaw, float pitch) {}

    //? if >=26.1.2 {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float spatialGUI$overrideFov(float original) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer == null) return original;
        float factor = renderer.getFovOverrideFactor();
        if (factor <= 0f) return original;
        return MathUtil.lerp(original, SpatialGUI.config.targetFov, factor);
    }
    //?}

    //? if >=26.1.2 {
    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void spatialGUI$modifyCamera(float partialTicks, CallbackInfo ci) {
    //?} else if >1.21.1 {
    /*@Inject(method = "setup", at = @At("TAIL"))
    private void spatialGUI$modifyCamera(net.minecraft.world.level.Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
    *///?} else {
    /*@Inject(method = "setup", at = @At("TAIL"))
    private void spatialGUI$modifyCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        *///?}
        var renderer = SpatialGUIClient.renderer();
        boolean isCapturing = renderer.shouldCapture();

        if (isCapturing && this.entity != null && SpatialGUIClient.isEnabled()) {
            TRANSITION_DURATION_MS = SpatialGUI.config.transitionDurationMs;
            boolean isFirstPerson = SpatialGUIClient.shouldUseFirstPersonMode(renderer.getHookedScreen());
            SpatialGUIClient.setEffectiveFirstPersonMode(isFirstPerson);
            MouseHandlerUtil.updateMouseGrabForFirstPerson(isFirstPerson);

            if (!wasCapturing) {
                baseXRot = Math.max(-SpatialGUI.config.firstPersonPitchClamp, Math.min(SpatialGUI.config.firstPersonPitchClamp, xRot));
            }

            boolean crosshairActive = SpatialGUIRenderer.isCrosshairModeActive();
            if (crosshairActive && !wasCrosshairMode) {
                freeLookYaw = 0f;
                freeLookPitch = 0f;
            }
            wasCrosshairMode = crosshairActive;

            if (!isFirstPerson && !SpatialGUIClient.getSwitchedToFirstPersonDueToBlock()) {
                CameraUtil.checkBlockCollision(this.entity);
            }

            CameraTransform transform = this.calculateCameraTransform(isFirstPerson, partialTicks);
            Vec3 newTargetPos = transform.pos();
            float newTargetYRot = transform.yaw();
            float newTargetXRot = transform.pitch();

            if (!wasCapturing || !isTransitioning) {
                this.startTransition(renderer, newTargetYRot, newTargetXRot, isFirstPerson);
            }

            this.applyCameraUpdate(newTargetPos, newTargetYRot, newTargetXRot, isFirstPerson);
            this.rotation.rotationYXZ((float) Math.PI - this.yRot * ((float) Math.PI / 180F), -this.xRot * ((float) Math.PI / 180F), 0.0F);
            wasCapturing = true;
        } else {
            this.resetCameraState();
        }
    }


    @Unique
    private CameraTransform calculateCameraTransform(boolean isFirstPerson, float partialTicks) {
        float distance = CameraUtil.calculateAutoFovDistance((float) SpatialGUI.config.cameraDistance, isFirstPerson);
        float sideOffset = CameraUtil.calculateAutoFovSideOffset((float) SpatialGUI.config.cameraSideOffset, isFirstPerson);
        float heightOffset = isFirstPerson ? entity.getEyeHeight() : Math.max(-4f, Math.min(4f, (float) SpatialGUI.config.cameraHeightOffset));
        float yaw, pitch;
        float positionYaw;

        Minecraft client = Minecraft.getInstance();

        if (!isFirstPerson && SpatialGUI.config.mirrorThirdPerson) {
            sideOffset = -sideOffset;
        }

        var acc = (MouseHandlerAccessor) client.mouseHandler;
        double rawMouseX = MouseHandlerUtil.getPhysicalX(acc.getRawXpos());
        double rawMouseY = MouseHandlerUtil.getPhysicalY(acc.getRawYpos());

        float normX = Math.max(-1f, Math.min(1f, (float) rawMouseX / client.getWindow().getScreenWidth() * 2f - 1f));
        float normY = Math.max(-1f, Math.min(1f, (float) rawMouseY / client.getWindow().getScreenHeight() * 2f - 1f));

        if (isFirstPerson) {
            float maxPitch = 90;
            float entityXRot = entity.getXRot();

            if (SpatialGUIRenderer.isCrosshairModeActive()) {
                double[] rel = MouseHandlerUtil.resetFreeLookDelta();
                double deltaX = rel[0];
                double deltaY = rel[1];

                double sens = CameraUtil.calculateMouseSensitivity();
                //? if >1.21.1 {
                double xOffset = deltaX * sens * (client.options.invertMouseX().get() ? -1 : 1);
                //?} else {
                /*double xOffset = deltaX * sens;
                *///?}
                double yOffset = deltaY * sens * (client.options.invertMouseY().get() ? -1 : 1);

                freeLookYaw += (float) xOffset;
                if (SpatialGUI.config.lockFirstPersonYaw) {
                    freeLookYaw = Math.max(-MAX_YAW_OFFSET, Math.min(MAX_YAW_OFFSET, freeLookYaw));
                }
                freeLookPitch = Math.max(-maxPitch - entityXRot, Math.min(maxPitch - entityXRot, freeLookPitch + (float) yOffset));

                smoothedCameraYaw = freeLookYaw;
                smoothedCameraPitch = entityXRot + freeLookPitch;
            } else if (SpatialGUI.config.disableFirstPersonParallax) {
                this.smoothParallaxOffsets(0f, 0f);
                smoothedCameraYaw = parallaxYawOffset;
                smoothedCameraPitch = baseXRot + parallaxPitchOffset;
            } else {
                this.smoothParallaxOffsets(
                        normX * MAX_YAW_OFFSET * (float) SpatialGUI.config.firstPersonMouseSensitivityYaw,
                        normY * 180f * (float) SpatialGUI.config.firstPersonMouseSensitivityPitch);
                smoothedCameraYaw = parallaxYawOffset;
                smoothedCameraPitch = baseXRot + parallaxPitchOffset;
            }

            smoothedCameraPitch = Math.max(-maxPitch, Math.min(maxPitch, smoothedCameraPitch));
            yaw = entity.getYRot() + smoothedCameraYaw;
            pitch = smoothedCameraPitch;
            positionYaw = yaw;
        } else {
            if (SpatialGUI.config.disableThirdPersonParallax) {
                this.smoothParallaxOffsets(0f, 0f);
            } else {
                this.smoothParallaxOffsets(
                        normX * MAX_YAW_OFFSET * (float) SpatialGUI.config.thirdPersonMouseSensitivityYaw,
                        normY * 180f * (float) SpatialGUI.config.thirdPersonMouseSensitivityPitch);
            }
            smoothedCameraYaw = parallaxYawOffset;
            smoothedCameraPitch = parallaxPitchOffset;

            yaw = entity.getYRot() + smoothedCameraYaw;
            pitch = (float) SpatialGUI.config.cameraTargetPitch + smoothedCameraPitch;
            positionYaw = entity.getYRot();
        }

        float yawRadians = (float) Math.toRadians(positionYaw);
        Vec3 entityPos = MathUtil.lerpEntityPosition(entity, partialTicks);

        double camX = entityPos.x + Math.sin(yawRadians) * distance + Math.cos(yawRadians) * sideOffset;
        double camY = entityPos.y + heightOffset;
        double camZ = entityPos.z - Math.cos(yawRadians) * distance + Math.sin(yawRadians) * sideOffset;

        Vec3 targetCamPos = new Vec3(camX, camY, camZ);
        if (!isFirstPerson) {
            Vec3 playerEyePos = new Vec3(entityPos.x, entityPos.y + entity.getEyeHeight(), entityPos.z);
            targetCamPos = CameraUtil.adjustCameraPositionForCollision(entity, playerEyePos, targetCamPos);
        }

        return new CameraTransform(targetCamPos, yaw, pitch);
    }

    @Unique
    private void smoothParallaxOffsets(float targetYaw, float targetPitch) {
        long now = System.nanoTime();
        float dt = lastParallaxNanos == 0 ? 0.016f : Math.min((now - lastParallaxNanos) / 1_000_000_000f, 0.1f);
        lastParallaxNanos = now;
        float alpha = 1f - (float) Math.exp(-dt / (PARALLAX_TAU_MS / 1000f));
        parallaxYawOffset += (targetYaw - parallaxYawOffset) * alpha;
        parallaxPitchOffset += (targetPitch - parallaxPitchOffset) * alpha;
    }

    @Unique
    private void startTransition(SpatialGUIRenderer renderer, float newTargetYRot, float newTargetXRot, boolean isFirstPerson) {
        Vec3 savedStartPos = renderer.getCameraStartPos();
        if (savedStartPos != null) {
            startPos = savedStartPos;
            startYRot = renderer.getCameraStartYRot();
        } else {
            startPos = new Vec3(position.x, position.y, position.z);
            startYRot = yRot;
        }
        startXRot = xRot;
        transitionStartTime = System.currentTimeMillis();
        isTransitioning = true;

        if (isFirstPerson) {
            smoothedCameraYaw = newTargetYRot - entity.getYRot();
            smoothedCameraPitch = newTargetXRot;
        } else {
            smoothedCameraYaw = 0f;
            smoothedCameraPitch = 0f;
        }
    }

    @Unique
    private void applyCameraUpdate(Vec3 newTargetPos, float newTargetYRot, float newTargetXRot, boolean isFirstPerson) {
        if (isFirstPerson) {
            position = newTargetPos;
            yRot = newTargetYRot;
            xRot = newTargetXRot;
        } else {
            long elapsed = System.currentTimeMillis() - transitionStartTime;
            float skipPercentage = SpatialGUI.config.transitionSkipPercentage / 100.0f;
            float adjustedElapsed = elapsed + (skipPercentage * TRANSITION_DURATION_MS);
            float progress = Math.min(adjustedElapsed / TRANSITION_DURATION_MS, 1.0f);
            float easedProgress = AnimationUtil.easeInOutSine(progress);

            position = new Vec3(MathUtil.lerp(startPos.x, newTargetPos.x, easedProgress), MathUtil.lerp(startPos.y, newTargetPos.y, easedProgress), MathUtil.lerp(startPos.z, newTargetPos.z, easedProgress));
            yRot = progress >= 1.0f ? newTargetYRot : MathUtil.rotLerp(startYRot, newTargetYRot, easedProgress);
            xRot = (SpatialGUI.config.lerpCameraPitch && SpatialGUIClient.getWasThirdPersonCamera() && progress < 1.0f) ? MathUtil.lerp(startXRot, newTargetXRot, easedProgress) : newTargetXRot;
        }
    }

    @Unique
    private void resetCameraState() {
        startPos = null;
        wasCapturing = false;
        isTransitioning = false;
        smoothedCameraYaw = 0f;
        smoothedCameraPitch = 0f;
        freeLookYaw = 0f;
        freeLookPitch = 0f;
        parallaxYawOffset = 0f;
        parallaxPitchOffset = 0f;
        lastParallaxNanos = 0;
        baseXRot = 0f;
        wasCrosshairMode = false;
        SpatialGUIClient.setWasThirdPersonCamera(false);
        SpatialGUIClient.setSwitchedToFirstPersonDueToBlock(false);
        SpatialGUIClient.setEffectiveFirstPersonMode(false);
        MouseHandlerUtil.updateMouseGrabForFirstPerson(false);
    }
}
