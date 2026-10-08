package dev.multispaceui.mixin.gui;

import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.interaction.SpatialInput;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * <p>Three things happen here:</p>
 * <ul>
 *   <li>a windowed screen is <b>not</b> extracted into the HUD anymore - its own
 *       off-screen capture does it, so the panel and the HUD never double draw
 *       the same widgets,</li>
 *   <li>{@code Screen.removed()} is skipped when the screen is being swapped for
 *       another window: the old screen (and its menu) must stay alive as a
 *       background window,</li>
 *   <li>after every screen change the window list is reconciled and the new
 *       active window is re-laid out at its own panel size.</li>
 * </ul>
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
                    SpatialWindowRenderer.applyLayout(window);
                }
            }
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Could not reconcile spatial windows", t);
        }
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
            )
    )
    private void multispaceui$extractWindowedScreenElsewhere(
            Screen windowed, GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick
    ) {
        if (SpatialInput.isManaged(windowed)) {
            // Extracted by its own window capture instead: the HUD must not draw it too.
            return;
        }
        windowed.extractRenderStateWithTooltipAndSubtitles(extractor, mouseX, mouseY, partialTick);
    }
}
