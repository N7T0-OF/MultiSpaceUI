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
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Picture-in-picture renderers of the game's own renderer, only kept as a last resort. */
    private static List<PictureInPictureRenderer<?>> gamePipRenderers;

    private final GuiRenderState renderState = new GuiRenderState();
    private GuiRenderer guiRenderer;
    private boolean borrowedPipRenderers;
    private TextureTarget target;
    private int targetWidth;
    private int targetHeight;
    private boolean stale = true;

    /** Redirect target of {@code renderer}, or {@code null} when it is not one of ours. */
    public static RenderTarget targetOf(GuiRenderer renderer) {
        return renderer == null ? null : TARGETS.get(renderer);
    }

    /** True once the screen has been extracted into this capture at least once. */
    public boolean hasTarget() {
        return target != null;
    }

    private static List<PictureInPictureRenderer<?>> gamePipRenderers(Minecraft mc) {
        if (gamePipRenderers != null) {
            return gamePipRenderers;
        }
        try {
            GuiRenderer main = ((GameRendererAccessor) mc.gameRenderer).multispaceui$getGuiRenderer();
            gamePipRenderers = List.copyOf(
                    ((GuiRendererAccessor) main).multispaceui$getPictureInPictureRenderers().values());
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not read the picture-in-picture renderers of the game", t);
            gamePipRenderers = List.of();
        }
        return gamePipRenderers;
    }

    /**
     * The picture-in-picture renderers, built for this window alone.
     *
     * <p>They hold the texture a preview (player entity, skin, book, banner, profiler chart) is
     * rendered into. That texture is closed together with the renderer they belong to, so a
     * window may never borrow the ones of the game: closing a single window would then destroy
     * the previews of the HUD and of every other window, and every later capture would fail
     * with "Color texture is closed".</p>
     */
    private static List<PictureInPictureRenderer<?>> ownPipRenderers(Minecraft mc) {
        return List.of(
                new GuiEntityRenderer(mc.getEntityRenderDispatcher()),
                new GuiSkinRenderer(),
                new GuiBookModelRenderer(),
                new GuiBannerResultRenderer(mc.getAtlasManager()),
                new GuiProfilerChartRenderer());
    }

    private void ensureRenderer(Minecraft mc) {
        if (guiRenderer != null) {
            return;
        }
        List<PictureInPictureRenderer<?>> pip;
        try {
            pip = ownPipRenderers(mc);
            borrowedPipRenderers = false;
        } catch (Throwable t) {
            // The renderer list of this Minecraft version changed: fall back to sharing so that
            // screens with a preview keep rendering instead of failing outright.
            MultiSpaceUI.LOGGER.warn("Could not create private picture-in-picture renderers", t);
            pip = gamePipRenderers(mc);
            borrowedPipRenderers = true;
        }
        guiRenderer = new GuiRenderer(renderState, mc.gameRenderer.featureRenderDispatcher(), pip);
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
        } catch (Throwable t) {
            diagnose(t, window);
        } finally {
            state.width = previousWidth;
            state.height = previousHeight;
        }
    }

    private int failures;

    /**
     * Keeps the frame alive while a capture fails, so that one broken window cannot blank every
     * other one: the panels keep their last texture and the failure is reported once.
     */
    private void diagnose(Throwable t, SpatialWindow window) {
        if (failures++ > 3) {
            return;
        }
        Set<String> states = new LinkedHashSet<>();
        renderState.forEachPictureInPicture(s -> states.add(s.getClass().getSimpleName()));
        MultiSpaceUI.LOGGER.error(
                "Could not render the content of {}: target={}x{} window={}x{} previews={}, giving up for now",
                window.screen().getClass().getSimpleName(), targetWidth, targetHeight,
                window.innerWidth(), window.innerHeight(), states, t);
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
            if (borrowedPipRenderers) {
                // These renderers belong to the game and to the other windows: handing them to
                // GuiRenderer.close() would destroy textures everybody still uses.
                ((GuiRendererAccessor) guiRenderer).multispaceui$getPictureInPictureRenderers().clear();
            }
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
