package dev.multispaceui.mixin.gui;

import com.mojang.blaze3d.platform.Window;
import dev.multispaceui.rendering.MultiSpaceLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the game window report the size of the window currently being captured.
 *
 * <p>While {@link MultiSpaceLayout} has a window pushed, every GUI size query
 * (screen initialisation, tooltip clamping, scissor setup) answers with that
 * window's panel size instead of the monitor size, so a screen lays itself out
 * for the panel it is going to appear in. The override exists only inside the
 * isolated extraction - vanilla never sees it.</p>
 */
@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "getGuiScaledWidth()I", at = @At("RETURN"), cancellable = true)
    private void multispaceui$overrideWidth(CallbackInfoReturnable<Integer> cir) {
        if (MultiSpaceLayout.isOverridden()) {
            cir.setReturnValue(MultiSpaceLayout.width(cir.getReturnValueI()));
        }
    }

    @Inject(method = "getGuiScaledHeight()I", at = @At("RETURN"), cancellable = true)
    private void multispaceui$overrideHeight(CallbackInfoReturnable<Integer> cir) {
        if (MultiSpaceLayout.isOverridden()) {
            cir.setReturnValue(MultiSpaceLayout.height(cir.getReturnValueI()));
        }
    }
}
