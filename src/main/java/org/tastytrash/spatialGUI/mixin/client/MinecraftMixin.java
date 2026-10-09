package org.tastytrash.spatialGUI.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1.2 {
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(Minecraft.class)
//?} else {
/*@Mixin(Minecraft.class)
*///?}
public class MinecraftMixin {
    // setScreenAndShow forces a frame without the level rendered, which would flash black
    // behind the screen, so render it
    //? if >=26.1.2 {
    @ModifyArg(
            method = "setScreenAndShow",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;renderFrame(Z)V"),
            index = 0
    )
    private boolean spatialGUI$renderWorldInForcedFrame(boolean original) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null && SpatialGUI.config.enabled && renderer.shouldCapture()) {
            return true;
        }
        return original;
    }
    //?}
}
