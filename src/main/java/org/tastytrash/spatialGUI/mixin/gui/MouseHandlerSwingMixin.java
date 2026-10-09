package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

//? if >=1.21.11 {
import net.minecraft.client.input.MouseButtonInfo;
//?}

@Mixin(MouseHandler.class)
public class MouseHandlerSwingMixin {

    //? if >=1.21.11 {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void spatialGUI$swingOnClick(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        spatialGUI$trySwing(action);
    }
    //?} else {
    /*@Inject(method = "onPress", at = @At("HEAD"))
    private void spatialGUI$swingOnClick(long handle, int button, int action, int mods, CallbackInfo ci) {
        spatialGUI$trySwing(action);
    }
    *///?}

    @Unique
    private static void spatialGUI$trySwing(int action) {
        if (!SpatialGUIClient.isEnabled() || !SpatialGUI.config.firstPersonHands.swingArmOnFirstPersonClick) return;
        if (action != 1) return;

        var renderer = SpatialGUIClient.renderer();
        if (renderer == null) return;

        //? if >=26.2 {
        Screen screen = Minecraft.getInstance().gui.screen();
         //?} else {
        /*Screen screen = Minecraft.getInstance().screen;
        *///?}
        if (screen == null || screen != renderer.getHookedScreen()) return;
        if (!SpatialGUIClient.getEffectiveFirstPersonMode()) return;

        var player = Minecraft.getInstance().player;
        if (player != null) {
            //? if >=26.3 {
            var heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND, heldItem.getAttackAnimation(), false);
            //?} else {
            /*player.swing(InteractionHand.MAIN_HAND);
             *///?}
        }
    }
}