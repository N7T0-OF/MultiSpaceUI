package dev.multispaceui.mixin.gui;

import dev.multispaceui.interaction.SpatialInput;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes mouse input to the panel the cursor is actually over.
 *
 * <p>Vanilla converts the raw cursor position with
 * {@code getScaledXPos/getScaledYPos} and feeds the result to the active screen.
 * While a spatial window is active those two methods answer with the cursor
 * position <b>inside that window's panel</b> (or a value far outside every
 * widget when the cursor misses the panel), so hover, clicks and drags act on
 * the panel content instead of on a monitor-shaped screen.</p>
 *
 * <p>A left click first focuses the window it hits, then the click is delivered
 * to that window. A click that misses every panel is dropped: it must never be
 * interpreted by a container screen as "clicked outside", which would silently
 * close the inventory.</p>
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(
            method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D",
            at = @At("RETURN"),
            cancellable = true
    )
    private void multispaceui$mapX(com.mojang.blaze3d.platform.Window window,
                                   CallbackInfoReturnable<Double> cir) {
        double mapped = SpatialInput.mapX(cir.getReturnValueD());
        if (mapped != cir.getReturnValueD()) {
            cir.setReturnValue(mapped);
        }
    }

    @Inject(
            method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D",
            at = @At("RETURN"),
            cancellable = true
    )
    private void multispaceui$mapY(com.mojang.blaze3d.platform.Window window,
                                   CallbackInfoReturnable<Double> cir) {
        double mapped = SpatialInput.mapY(cir.getReturnValueD());
        if (mapped != cir.getReturnValueD()) {
            cir.setReturnValue(mapped);
        }
    }

    /** Focuses the window under the cursor before the click is dispatched. */
    @Inject(method = "onButton", at = @At("HEAD"))
    private void multispaceui$focusOnPress(long handle, MouseButtonInfo info, int action,
                                           CallbackInfo ci) {
        SpatialInput.onButton(info.button(), action);
    }

    @Redirect(
            method = "onButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"
            )
    )
    private boolean multispaceui$routeClick(Screen screen, MouseButtonEvent event, boolean doubleClick) {
        if (SpatialInput.shouldDropClick(screen)) {
            return false;
        }
        return screen.mouseClicked(event, doubleClick);
    }

    @Redirect(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"
            )
    )
    private boolean multispaceui$routeScroll(Screen screen, double a, double b, double c, double d) {
        if (SpatialInput.shouldDropClick(screen)) {
            return false;
        }
        return screen.mouseScrolled(a, b, c, d);
    }
}
