package org.tastytrash.spatialGUI.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.mixin.gui.MouseHandlerAccessor;

//? if >1.21.1 {
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Unique private static float smoothedHeadYaw = 0f;
    @Unique private static float smoothedHeadPitch = 0f;

    @Unique private static final float MAX_YAW_OFFSET = 40f;
    @Unique private static final float MAX_PITCH_OFFSET = 25f;
    @Unique private static final float SMOOTHING = 0.15f;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void spatialGUI$overrideHeadLook(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var renderer = SpatialGUIClient.renderer();

        if (entity != client.player || !renderer.shouldCapture() || !SpatialGUIClient.isEnabled()) {
            renderer.headLockInitialized = false;
            smoothedHeadYaw = 0f;
            smoothedHeadPitch = 0f;
            return;
        }

        boolean justOpened = !renderer.headLockInitialized;

        double mouseX = ((MouseHandlerAccessor) client.mouseHandler).getRawXpos();
        double mouseY = ((MouseHandlerAccessor) client.mouseHandler).getRawYpos();
        int width = client.getWindow().getScreenWidth();
        int height = client.getWindow().getScreenHeight();

        float normX = Math.clamp((float) (mouseX / width) * 2f - 1f, -1f, 1f);
        float normY = Math.clamp((float) (mouseY / height) * 2f - 1f, -1f, 1f);

        float bodyRotationOffset = (float) SpatialGUI.config.avatarBodyRotationOffset;
        if (SpatialGUI.config.mirrorThirdPerson) {
            bodyRotationOffset = -bodyRotationOffset;
        }
        state.bodyRot = client.player.getYRot() + bodyRotationOffset;

        float baseYawOffset = (float) SpatialGUI.config.avatarBaseYawOffset;
        if (SpatialGUI.config.mirrorThirdPerson) {
            baseYawOffset = -baseYawOffset;
        }

        float targetYaw = normX * MAX_YAW_OFFSET + baseYawOffset;
        float targetPitch = Mth.clamp(normY * MAX_PITCH_OFFSET, -60f, 60f);

        if (justOpened) {
            smoothedHeadYaw = targetYaw;
            smoothedHeadPitch = targetPitch;
            renderer.headLockInitialized = true;
        } else {
            smoothedHeadYaw = Mth.rotLerp(SMOOTHING, smoothedHeadYaw, targetYaw);
            smoothedHeadPitch = Mth.lerp(SMOOTHING, smoothedHeadPitch, targetPitch);
        }

        state.yRot = smoothedHeadYaw;
        state.xRot = smoothedHeadPitch;
    }
}
//?} else {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(PlayerRenderer.class)
public class AvatarRendererMixin {
    @Unique private static float smoothedHeadYaw = 0f;
    @Unique private static float smoothedHeadPitch = 0f;

    @Unique private static final float MAX_YAW_OFFSET = 40f;
    @Unique private static final float MAX_PITCH_OFFSET = 25f;
    @Unique private static final float SMOOTHING = 0.15f;

    @WrapMethod(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void spatialGUI$overrideHeadLook(AbstractClientPlayer entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, Operation<Void> original) {
        Minecraft client = Minecraft.getInstance();
        var renderer = SpatialGUIClient.renderer();

        if (entity != client.player || !renderer.shouldCapture() || !SpatialGUIClient.isEnabled()) {
            renderer.headLockInitialized = false;
            smoothedHeadYaw = 0f;
            smoothedHeadPitch = 0f;
            original.call(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        if (SpatialGUIRenderer.isExtractingScreen) {
            original.call(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        boolean justOpened = !renderer.headLockInitialized;

        double mouseX = ((MouseHandlerAccessor) client.mouseHandler).getRawXpos();
        double mouseY = ((MouseHandlerAccessor) client.mouseHandler).getRawYpos();
        int width = client.getWindow().getScreenWidth();
        int height = client.getWindow().getScreenHeight();

        float normX = Math.max(-1f, Math.min(1f, (float) mouseX / width * 2f - 1f));
        float normY = Math.max(-1f, Math.min(1f, (float) mouseY / height * 2f - 1f));

        float bodyRotationOffset = (float) SpatialGUI.config.avatarBodyRotationOffset;
        if (SpatialGUI.config.mirrorThirdPerson) {
            bodyRotationOffset = -bodyRotationOffset;
        }
        float bodyRot = client.player.getYRot() + bodyRotationOffset;

        float baseYawOffset = (float) SpatialGUI.config.avatarBaseYawOffset;
        if (SpatialGUI.config.mirrorThirdPerson) {
            baseYawOffset = -baseYawOffset;
        }

        float targetYaw = normX * MAX_YAW_OFFSET + baseYawOffset;
        float targetPitch = Mth.clamp(normY * MAX_PITCH_OFFSET, -60f, 60f);

        if (justOpened) {
            smoothedHeadYaw = targetYaw;
            smoothedHeadPitch = targetPitch;
            renderer.headLockInitialized = true;
        } else {
            smoothedHeadYaw = Mth.rotLerp(SMOOTHING, smoothedHeadYaw, targetYaw);
            smoothedHeadPitch = Mth.lerp(SMOOTHING, smoothedHeadPitch, targetPitch);
        }

        float oldBody = entity.yBodyRot, oldBodyO = entity.yBodyRotO;
        float oldHead = entity.yHeadRot, oldHeadO = entity.yHeadRotO;
        float oldXRot = entity.getXRot(), oldXRotO = entity.xRotO;

        float headRot = bodyRot + smoothedHeadYaw;
        entity.yBodyRot = bodyRot;
        entity.yBodyRotO = bodyRot;
        entity.yHeadRot = headRot;
        entity.yHeadRotO = headRot;
        entity.setXRot(smoothedHeadPitch);
        entity.xRotO = smoothedHeadPitch;

        try {
            original.call(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } finally {
            entity.yBodyRot = oldBody;
            entity.yBodyRotO = oldBodyO;
            entity.yHeadRot = oldHead;
            entity.yHeadRotO = oldHeadO;
            entity.setXRot(oldXRot);
            entity.xRotO = oldXRotO;
        }
    }
}
*///?}