package dev.multispaceui.mixin.gui;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps vanilla and the spatial windows from fighting over the active screen.
 *
 * <p>Two things happen here:</p>
 * <ul>
 *   <li>{@code Screen.removed()} is skipped when the screen is being swapped for
 *       another window: the old screen (and its menu) must stay alive as a
 *       background window,</li>
 *   <li>after every screen change the window list is reconciled and the window the
 *       game just switched to is measured at its own panel size.</li>
 * </ul>
 *
 * <p>Extracting the active screen into an off-screen texture instead of the HUD is the
 * Spatial GUI base's own {@code GuiMixin}: it swaps the extractor argument for its
 * isolated one, so there is nothing to do here - and doing it as well would only make
 * the screen disappear from both.</p>
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Shadow
    private Screen screen;

    /** Screen {@code setScreen} is switching to, read by the {@code removed()} redirect. */
    @Unique
    private Screen multispaceui$incoming;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void multispaceui$captureIncoming(Screen next, CallbackInfo ci) {
        this.multispaceui$incoming = next;
    }

    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V")
    )
    private void multispaceui$keepBackgroundWindow(Screen old) {
        if (MultiSpaceUI.isEnabled()
                && WindowManager.get().shouldKeepOnReplace(old, this.multispaceui$incoming)) {
            // The screen stays alive as a background window: no removed(), no menu close.
            return;
        }
        old.removed();
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void multispaceui$reconcileWindows(CallbackInfo ci) {
        if (!MultiSpaceUI.isEnabled()) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            WindowManager manager = WindowManager.get();
            Screen current = this.screen;
            if (current != null) {
                manager.onScreenOpened(current);
            }
            manager.onScreenReplaced(current);

            if (current != null) {
                SpatialWindow window = manager.findByScreen(current);
                if (window != null) {
                    SpatialWindowRenderer.ensureSize(mc, window);
                }
            }
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Could not reconcile spatial windows", t);
        }
    }
}
