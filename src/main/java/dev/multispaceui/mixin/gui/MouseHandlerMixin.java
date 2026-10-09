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

/**
 * Focus and click routing of the window layer.
 *
 * <p>Mapping the cursor into the panel belongs to the Spatial GUI base: its own mixin
 * rewrites {@code getScaledXPos/getScaledYPos} with the position inside the panel of the
 * window it hooked, and this mod only ever opens windows for screens that mixin would
 * hook too (see {@code ScreenCompatibility}). Two injects therefore stay here, both about
 * deciding <i>which</i> window input belongs to:</p>
 *
 * <ul>
 *   <li>a left click first focuses the window it hits, then is delivered to that window,</li>
 *   <li>a click or a scroll that misses every panel is dropped: it must never be
 *       interpreted by a container screen as "clicked outside", which would silently
 *       close the inventory.</li>
 * </ul>
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

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
