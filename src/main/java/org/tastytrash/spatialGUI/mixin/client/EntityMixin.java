package org.tastytrash.spatialGUI.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(Entity.class)
public abstract class EntityMixin {
    //? if >=26.3 {
    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float spatialGUI$cameraViewXRot(float original, float partialTick) {
        if (!((Object) this instanceof LocalPlayer)) {
            return original;
        }

        var renderer = SpatialGUIClient.renderer();
        if (renderer == null
                || !SpatialGUIClient.isEnabled()
                || !SpatialGUIClient.getEffectiveFirstPersonMode()
                || renderer.getHookedScreen() == null
                || SpatialGUI.config.firstPersonHands.hideHandsInFirstPerson) {
            return original;
        }

        return Minecraft.getInstance().gameRenderer.mainCamera().xRot();
    }
    //?}
}
