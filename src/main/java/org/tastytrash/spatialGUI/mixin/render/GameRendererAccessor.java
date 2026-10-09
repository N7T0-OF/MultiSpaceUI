package org.tastytrash.spatialGUI.mixin.render;

import org.spongepowered.asm.mixin.Mixin;

//? if fabric && >1.21.1 {
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("guiRenderer")
    GuiRenderer spatialGUI$getGuiRenderer();
}
//?} else {
/*@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public interface GameRendererAccessor {}
*///?}