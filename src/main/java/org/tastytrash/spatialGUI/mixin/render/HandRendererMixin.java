package org.tastytrash.spatialGUI.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >26.2 {
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
//? } else {
/*import net.minecraft.client.renderer.ItemInHandRenderer;
*///? }

//? if >1.21.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
 //?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if >26.2 {
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class HandRendererMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideShield(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (itemStack.getItem() == Items.SHIELD) {
            var renderer = SpatialGUIClient.renderer();
            if (renderer.shouldCapture() && SpatialGUIClient.isEnabled() && SpatialGUI.config.firstPersonHands.hideShieldInFirstPerson) {
                ci.cancel();
            }
        }
    }
}
//? } else {
/*@Mixin(ItemInHandRenderer.class)
public class HandRendererMixin {

    //? if 26.2 {
    /^@Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideShield(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (itemStack.getItem() == Items.SHIELD) {
            var renderer = SpatialGUIClient.renderer();
            if (renderer.shouldCapture() && SpatialGUIClient.isEnabled() && SpatialGUI.config.firstPersonHands.hideShieldInFirstPerson) {
                ci.cancel();
            }
        }
    }
    ^///?} else if >1.21.1 {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideShield(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (itemStack.getItem() == Items.SHIELD) {
            var renderer = SpatialGUIClient.renderer();
            if (renderer.shouldCapture() && SpatialGUIClient.isEnabled() && SpatialGUI.config.firstPersonHands.hideShieldInFirstPerson) {
                ci.cancel();
            }
        }
    }
    //?} else {
    /^@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void spatialGUI$hideShield(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, MultiBufferSource buffer, int lightCoords, CallbackInfo ci) {
        if (itemStack.getItem() == Items.SHIELD) {
            var renderer = SpatialGUIClient.renderer();
            if (renderer.shouldCapture() && SpatialGUIClient.isEnabled() && SpatialGUI.config.firstPersonHands.hideShieldInFirstPerson) {
                ci.cancel();
            }
        }
    }
    ^///?}
}
*///? }
