package org.tastytrash.spatialGUI.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    //? if <=1.21.1 {
    /*@Invoker("getFov")
    double spatialGUI$getFov(Camera camera, float partialTick, boolean useFovSetting);
    *///? } else if 1.21.11 {
    /*@Invoker("getFov")
    float spatialGUI$getFov(Camera camera, float partialTick, boolean useFovSetting);
    *///?}
}