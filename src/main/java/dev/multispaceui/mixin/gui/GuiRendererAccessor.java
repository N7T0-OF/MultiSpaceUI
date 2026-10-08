package dev.multispaceui.mixin.gui;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Read-only access to the picture-in-picture renderers owned by the vanilla
 * {@link GuiRenderer}. The spatial pass clones this collection for every
 * private renderer so that item rendering (chests, furnaces, ...) works inside
 * the off-screen targets exactly like it does on the main HUD.
 */
@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {

    @Accessor("pictureInPictureRenderers")
    Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> multispaceui$getPictureInPictureRenderers();
}
