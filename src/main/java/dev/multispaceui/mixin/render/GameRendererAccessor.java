package dev.multispaceui.mixin.render;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read-only access to the vanilla {@link GuiRenderer}, used to clone its
 * picture-in-picture renderers for the off-screen window renderers.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("guiRenderer")
    GuiRenderer multispaceui$getGuiRenderer();
}
