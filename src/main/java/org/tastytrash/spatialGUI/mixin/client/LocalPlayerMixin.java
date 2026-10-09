package org.tastytrash.spatialGUI.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow public float xBob;
    @Shadow public float yBob;
    @Shadow public float xBobO;
    @Shadow public float yBobO;

    @Shadow protected abstract boolean isControlledCamera();

    @Unique private float spatialGUI$xBobBefore;
    @Unique private float spatialGUI$yBobBefore;
    @Unique private boolean spatialGUI$wasActive = false;

    @Unique
    private static boolean spatialGUI$swayActive() {
        var renderer = SpatialGUIClient.renderer();
        return renderer != null
                && SpatialGUIClient.isEnabled()
                && SpatialGUIClient.getEffectiveFirstPersonMode()
                && renderer.getHookedScreen() != null
                && !SpatialGUI.config.firstPersonHands.hideHandsInFirstPerson;
    }

    @Unique
    private static float spatialGUI$cameraPitch() {
        //? if >1.21.1 {
        return Minecraft.getInstance().gameRenderer.mainCamera().xRot();
        //?} else {
        /*return Minecraft.getInstance().gameRenderer.mainCamera().getXRot();
        *///?}
    }

    @Unique
    private static float spatialGUI$cameraYaw() {
        //? if >1.21.1 {
        return Minecraft.getInstance().gameRenderer.mainCamera().yRot();
        //?} else {
        /*return Minecraft.getInstance().gameRenderer.mainCamera().getYRot();
        *///?}
    }

    //? if >1.21.1 {
    @Inject(method = "applyInput", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "serverAiStep", at = @At("HEAD"))
    *///?}
    private void spatialGUI$captureBob(CallbackInfo ci) {
        spatialGUI$xBobBefore = this.xBob;
        spatialGUI$yBobBefore = this.yBob;
    }

    //? if >1.21.1 {
    @Inject(method = "applyInput", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "serverAiStep", at = @At("TAIL"))
    *///?}
    private void spatialGUI$bobFollowsCamera(CallbackInfo ci) {
        if (!spatialGUI$swayActive() || !this.isControlledCamera()) {
            spatialGUI$wasActive = false;
            return;
        }

        float camPitch = spatialGUI$cameraPitch();
        float camYaw = spatialGUI$cameraYaw();

        if (!spatialGUI$wasActive) {
            this.xBob = this.xBobO = camPitch;
            this.yBob = this.yBobO = camYaw;
            spatialGUI$wasActive = true;
            return;
        }

        this.xBob = spatialGUI$xBobBefore + (camPitch - spatialGUI$xBobBefore) * 0.5F;
        this.yBob = spatialGUI$yBobBefore + Mth.wrapDegrees(camYaw - spatialGUI$yBobBefore) * 0.5F;
    }

    //? if <26.3 {
    /*@ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float spatialGUI$cameraViewXRot(float original, float a) {
        if (!spatialGUI$swayActive()) return original;
        return spatialGUI$cameraPitch();
    }
    *///?}

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float spatialGUI$cameraViewYRot(float original, float a) {
        if (!spatialGUI$swayActive()) return original;
        float bob = Mth.lerp(a, this.yBobO, this.yBob);
        return bob + Mth.wrapDegrees(spatialGUI$cameraYaw() - bob);
    }
}
