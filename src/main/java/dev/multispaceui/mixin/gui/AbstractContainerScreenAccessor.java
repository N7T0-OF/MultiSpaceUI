package dev.multispaceui.mixin.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the natural size of a container screen (176x166 for a chest, ...) so
 * the window it lives in can be opened at the size its content expects.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("imageWidth")
    int multispaceui$imageWidth();

    @Accessor("imageHeight")
    int multispaceui$imageHeight();
}
