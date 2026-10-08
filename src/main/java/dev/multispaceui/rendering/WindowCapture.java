package dev.multispaceui.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.mixin.gui.GuiRendererAccessor;
import dev.multispaceui.mixin.render.GameRendererAccessor;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Off-screen target one window is rendered into.
 *
 * <p>Each window owns a {@link GuiRenderState} and a private {@link GuiRenderer}: the screen
 * writes its widgets into the state, the renderer flushes the state into the window's own
 * {@link TextureTarget}, and the panel quad then samples that texture in world space.</p>
 *
 * <p>Every private renderer is registered in {@link #TARGETS} so {@code GuiRendererMixin}
 * can redirect its draws into the right texture, while every other renderer of the game
 * keeps drawing to the main render target.</p>
 */
public final class WindowCapture implements AutoCloseable {

    /** Which off-screen target each private {@link GuiRenderer} draws into. */
    private static final Map<GuiRenderer, TextureTarget> TARGETS = new IdentityHashMap<>();

    private static List<PictureInPictureRenderer<?>> vanillaPipRenderers;

    private final GuiRenderState renderState = new GuiRenderState();
    private GuiRenderer guiRenderer;
    private TextureTarget target;
    private int targetWidth;
    private int targetHeight;
    private boolean stale = true;

    /** Redirect target of {@code renderer}, or {@code null} when it is not one of ours. */
    public static RenderTarget targetOf(GuiRenderer renderer) {
        return renderer == null ? null : TARGETS.get(renderer);
    }

    private static List<PictureInPictureRenderer<?>> pipRenderers(Minecraft mc) {
        if (vanillaPipRenderers != null) {
            return vanillaPipRenderers;
        }
        try {
            GuiRenderer main = ((GameRendererAccessor) mc.gameRenderer).multispaceui$getGuiRenderer();
            vanillaPipRenderers = List.copyOf(
                    ((GuiRendererAccessor) main).multispaceui$getPictureInPictureRenderers().values());
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not borrow the vanilla picture-in-picture renderers", t);
            vanillaPipRenderers = List.of();
        }
        return vanillaPipRenderers;
    }

    private void ensureRenderer(Minecraft mc) {
        if (guiRenderer != null) {
            return;
        }
        guiRenderer = new GuiRenderer(renderState, mc.gameRenderer.featureRenderDispatcher(), pipRenderers(mc));
    }

    /**
     * Sizes the target to the window content, honouring {@code renderScalePercent}: one GUI
     * unit maps to {@code guiScale * percent} texels.
     */
    void prepareTarget(Minecraft mc, SpatialWindow window, int renderScalePercent) {
        int guiScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        double scale = Math.max(25, Math.min(renderScalePercent, 100)) / 100.0;
        int w = Math.max(1, (int) Math.round(window.innerWidth() * guiScale * scale));
        int h = Math.max(1, (int) Math.round(window.innerHeight() * guiScale * scale));
        if (target == null) {
            target = new TextureTarget("MultiSpaceUI window", w, h, GpuFormat.RGBA8_UNORM, GpuFormat.D16_UNORM);
            targetWidth = w;
            targetHeight = h;
        } else if (targetWidth != w || targetHeight != h) {
            target.resize(w, h);
            targetWidth = w;
            targetHeight = h;
        }
        ensureRenderer(mc);
        TARGETS.put(guiRenderer, target);
    }

    /** Opens a fresh extraction for {@code window} and returns the graphics to extract with. */
    GuiGraphicsExtractor beginExtract(Minecraft mc, SpatialWindow window, int mouseX, int mouseY) {
        renderState.reset();
        MultiSpaceLayout.push(window);
        stale = false;
        return new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY);
    }

    void endExtract() {
        MultiSpaceLayout.pop();
    }

    /** True when the window content still has to be extracted this frame. */
    boolean isStale() {
        return stale;
    }

    void markStale() {
        this.stale = true;
    }

    /**
     * Extracts {@code window} (unless vanilla already did it) and flushes the result into
     * the window texture. The projection used by the private renderer is patched so GUI unit
     * {@code [0, innerWidth] x [0, innerHeight]} covers the whole target.
     */
    void capture(Minecraft mc, SpatialWindow window, int mouseX, int mouseY, float partialTick) {
        if (target == null || guiRenderer == null) {
            return;
        }
        clearTarget();

        WindowRenderState state = mc.gameRenderer.gameRenderState().windowRenderState;
        int previousWidth = state.width;
        int previousHeight = state.height;
        int guiScale = Math.max(1, (int) mc.getWindow().getGuiScale());
        state.width = window.innerWidth() * guiScale;
        state.height = window.innerHeight() * guiScale;
        try {
            if (stale) {
                GuiGraphicsExtractor extractor = beginExtract(mc, window, mouseX, mouseY);
                try {
                    SpatialWindowRenderer.captureInProgress = true;
                    window.screen().extractRenderStateWithTooltipAndSubtitles(extractor, mouseX, mouseY, partialTick);
                } finally {
                    SpatialWindowRenderer.captureInProgress = false;
                    endExtract();
                }
            }
            guiRenderer.render();
            guiRenderer.endFrame();
        } finally {
            state.width = previousWidth;
            state.height = previousHeight;
        }
    }

    private void clearTarget() {
        GpuTexture color = target.getColorTexture();
        GpuTexture depth = target.getDepthTexture();
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(color, new Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        if (depth != null) {
            encoder.clearDepthTexture(depth, 1.0);
        }
    }

    TextureTarget target() {
        return target;
    }

    GuiRenderer guiRenderer() {
        ensureRenderer(Minecraft.getInstance());
        return guiRenderer;
    }

    @Override
    public void close() {
        MultiSpaceLayout.popAll();
        if (guiRenderer != null) {
            TARGETS.remove(guiRenderer);
            try {
                guiRenderer.close();
            } catch (Exception e) {
                MultiSpaceUI.LOGGER.debug("Could not close window GuiRenderer", e);
            }
            guiRenderer = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        stale = true;
    }
}
