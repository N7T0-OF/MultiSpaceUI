package org.tastytrash.spatialGUI.mixin.gui;

import org.spongepowered.asm.mixin.Mixin;

//? if fabric && >1.21.1 {
import java.util.Map;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
//? if >=26.1.2 {
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
//?} else {
/*import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
*///?}
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("pictureInPictureRenderers")
    Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> spatialGUI$getPictureInPictureRenderers();

    @Mutable
    @Accessor("pictureInPictureRenderers")
    void spatialGUI$setPictureInPictureRenderers(Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> renderers);
}
//?} else {
/*@Mixin(net.minecraft.client.Minecraft.class)
public interface GuiRendererAccessor {}
*///?}