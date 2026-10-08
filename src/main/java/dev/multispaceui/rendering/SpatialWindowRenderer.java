package dev.multispaceui.rendering;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.interaction.WindowTargeting;
import dev.multispaceui.rendering.PanelGeometry.QuadBasis;
import dev.multispaceui.window.SpatialWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Drives the whole spatial pipeline for every open window, once per frame:
 *
 * <ol>
 *   <li>read the frame the level pass produced (projection, view rotation, camera),</li>
 *   <li>pick the window under the cursor and the one being looked at,</li>
 *   <li>evaluate focus / distance / opacity and smooth it over time,</li>
 *   <li>place every panel in view space and remember its frame for mouse picking,</li>
 *   <li>extract each screen into its own texture and flush it,</li>
 *   <li>draw one textured quad per window, farthest first.</li>
 * </ol>
 *
 * <p>The panels are drawn after the vanilla GUI pass so they always sit on top of the HUD,
 * using the world projection captured during the level pass: they are part of the scene,
 * not an overlay.</p>
 */
public final class SpatialWindowRenderer {

    /** True while a screen is being extracted into an isolated window target. */
    static boolean captureInProgress;

    public static SpatialWindowRenderer instance = new SpatialWindowRenderer();

    private static final RenderPipeline PANEL_PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static final StagedVertexBuffer PANEL_BUFFER =
            new StagedVertexBuffer(() -> "MultiSpaceUI panel buffer", 1 << 18);
    private static final ProjectionMatrixBuffer PROJECTION_BUFFER =
            new ProjectionMatrixBuffer("multispaceui");
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f ZERO = new Vector3f();

    private final Map<SpatialWindow, WindowCapture> captures = new IdentityHashMap<>();
    private final Map<SpatialWindow, QuadBasis> bases = new IdentityHashMap<>();
    private final Map<SpatialWindow, Matrix4f> poses = new IdentityHashMap<>();
    private final WindowVisibilityController visibility = new WindowVisibilityController();
    private final WindowTargeting targeting = new WindowTargeting();
    private final PoseStack poseStack = new PoseStack();

    // --- frame captured during the level pass ---------------------------------------
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewRotation = new Matrix4f();
    private GpuBufferSlice projectionSlice;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private int frameWidth;
    private int frameHeight;
    private boolean hasFrame;
    /** Set when {@link #reset()} was called off the render thread. */
    private volatile boolean pendingReset;
    private long lastFrameNanos = System.nanoTime();

    public WindowTargeting targeting() {
        return targeting;
    }

    /** The off-screen target of a window, used by {@code GuiRendererMixin}. */
    public WindowCapture captureOf(SpatialWindow window) {
        return captures.get(window);
    }

    public WindowCapture captureFor(SpatialWindow window) {
        return captures.computeIfAbsent(window, w -> {
            WindowCapture capture = new WindowCapture();
            capture.markStale();
            return capture;
        });
    }

    // ----------------------------------------------------------------------------------
    // Frame capture (LevelRenderer.render TAIL)
    // ----------------------------------------------------------------------------------

    public void captureFrame(Matrix4fc frameProjection, Matrix4fc frameViewRotation,
                             double camX, double camY, double camZ, int width, int height) {
        this.projectionMatrix.set(frameProjection);
        this.viewRotation.set(frameViewRotation);
        this.cameraX = camX;
        this.cameraY = camY;
        this.cameraZ = camZ;
        this.frameWidth = width;
        this.frameHeight = height;
        try {
            this.projectionSlice = PROJECTION_BUFFER.getBuffer(this.projectionMatrix);
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.debug("Could not upload the MultiSpaceUI projection", t);
            this.projectionSlice = null;
        }
        this.hasFrame = this.projectionSlice != null;
    }

    public boolean hasFrame() {
        return hasFrame;
    }

    public Matrix4fc projectionMatrix() {
        return projectionMatrix;
    }

    /** Size of the frame the panels were placed in, in framebuffer pixels. */
    public int frameWidth() {
        return frameWidth;
    }

    public int frameHeight() {
        return frameHeight;
    }

    /** Placement of a panel in view space, or {@code null} while it is not placed yet. */
    public QuadBasis basisOf(SpatialWindow window) {
        return bases.get(window);
    }

    /** True once the screen of this window has been extracted into its own texture. */
    public boolean hasCaptureContent(SpatialWindow window) {
        WindowCapture capture = captures.get(window);
        return capture != null && capture.hasTarget();
    }

    public Matrix4fc viewRotation() {
        return viewRotation;
    }

    /**
     * Releases every capture. Off the render thread the request is only remembered:
     * textures and buffers may not be destroyed from a network or server thread.
     */
    public void reset() {
        if (!RenderSystem.isOnRenderThread()) {
            pendingReset = true;
            return;
        }
        pendingReset = false;
        resetNow();
    }

    private void resetNow() {
        for (WindowCapture capture : captures.values()) {
            capture.close();
        }
        captures.clear();
        bases.clear();
        poses.clear();
        hasFrame = false;
        projectionSlice = null;
    }

    /** Drops everything when the player leaves the world. */
    public void onDisconnect() {
        reset();
    }

    /** Runs a reset that was requested from another thread. Safe to call every frame. */
    public void applyPendingReset() {
        if (pendingReset && RenderSystem.isOnRenderThread()) {
            pendingReset = false;
            resetNow();
        }
    }

    // ----------------------------------------------------------------------------------
    // Frame
    // ----------------------------------------------------------------------------------

    /** Full frame: targeting, presentation, capture and draw. */
    public void renderFrame(float partialTick) {
        applyPendingReset();
        Minecraft mc = Minecraft.getInstance();
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        WindowManager manager = WindowManager.get();
        if (!cfg.enabled || mc.player == null || !manager.hasWindows() || !hasFrame) {
            return;
        }

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrameNanos) / 1.0E9f);
        lastFrameNanos = now;

        // 1. What the pointer and the crosshair hit, using the frames placed last time.
        targeting.update(mc, manager, bases, projectionMatrix, frameWidth, frameHeight);
        SpatialWindow focused = targeting.focused();
        SpatialWindow hovered = targeting.hovered();

        // 2. Presentation targets, then temporal smoothing.
        for (SpatialWindow window : manager.windows()) {
            ensureSize(mc, window);
            followAnchor(mc, window, cfg);
            float focusWeight = WindowVisibilityController.focusWeight(window, focused, hovered, manager.active());
            float placed = cfg.panelDistance + window.distanceOffset()
                    + (focusWeight >= 0.999f ? 0.0f : cfg.backgroundPushBack);
            // The falloff reacts to where the panel really is: the same as `placed` while the
            // panels follow the player, and to how far the player walked away once a panel is
            // anchored in the world. The placement target stays `placed`, so the measurement
            // can never feed back into it.
            float cameraDistance = cameraDistance(mc, window, placed);
            WindowVisibilityController.Presentation presentation =
                    visibility.evaluate(window, focused, hovered, manager.active(), cameraDistance);
            window.setTargets(presentation.scale(), presentation.opacity(), placed);
            window.setVisible(presentation.visible());
            window.setInteractionEnabled(presentation.interactive());
            window.setFocused(window == focused);
            window.setHovered(window == hovered);
            window.interpolate(dt, cfg.transitionSpeed, cfg.animationsEnabled);
        }

        // 3. Placement: builds the view space pose and refreshes the picking frames.
        List<SpatialWindow> drawable = new ArrayList<>();
        for (SpatialWindow window : manager.windows()) {
            if (!window.isVisible() || window.isMinimized() || window.isClosing()) {
                bases.remove(window);
                poses.remove(window);
                continue;
            }
            QuadBasis basis = place(mc, window);
            if (basis != null) {
                bases.put(window, basis);
                drawable.add(window);
            } else {
                bases.remove(window);
                poses.remove(window);
            }
        }

        // 4. Capture every drawable window into its own texture.
        for (SpatialWindow window : drawable) {
            WindowCapture capture = captureFor(window);
            // Screens are animated and hovered: their content is re-extracted every frame.
            capture.markStale();
            capture.prepareTarget(mc, window, cfg.renderScalePercent);
            int[] mouse = mouseFor(window);
            capture.capture(mc, window, mouse[0], mouse[1], partialTick);
        }

        // 5. Draw, farthest window first so overlapping panels composite correctly.
        drawable.sort(Comparator.comparingDouble(w -> PanelGeometry.viewDepth(bases.get(w))).reversed());
        drawPanels(drawable);

        // 6. Drop dead entries.
        bases.keySet().retainAll(manager.windows());
        captures.entrySet().removeIf(entry -> {
            if (!manager.windows().contains(entry.getKey())) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }

    // ----------------------------------------------------------------------------------
    // Placement
    // ----------------------------------------------------------------------------------

    /**
     * Distance the panel is evaluated at: the distance it is placed in front of its anchor,
     * plus how far the player's eye stands from that anchor.
     *
     * <p>While the panels follow the player the anchor sits exactly on the eye, so this is the
     * configured placement distance and the falloff behaves as before. When a panel is anchored
     * in the world the second term grows with every block the player walks away and shrinks
     * again when they come back, which is what the distance falloff and the {@code maxDistance}
     * cutoff have to react to. Measured from the player rather than from the camera so that the
     * third person camera offset is not mistaken for a panel that is suddenly far away.</p>
     */
    private float cameraDistance(Minecraft mc, SpatialWindow window, float placed) {
        if (mc.player == null) {
            return placed;
        }
        double dx = window.anchorX() - mc.player.getX();
        double dy = window.anchorY() - mc.player.getEyeY();
        double dz = window.anchorZ() - mc.player.getZ();
        double away = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Float.isFinite((float) away) ? placed + (float) away : placed;
    }

    private QuadBasis place(Minecraft mc, SpatialWindow window) {
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        int activeIndex = indexOfActive();
        float arcDegrees = (window.layoutIndex() - activeIndex) * cfg.windowSpacingDegrees;

        poseStack.pushPose();
        try {
            PanelGeometry.placePanel(poseStack, viewRotation,
                    cameraX, cameraY, cameraZ,
                    window.anchorX(), window.anchorY(), window.anchorZ(),
                    window.anchorYaw() + cfg.panelYawOffset,
                    window.anchorPitch() + cfg.panelPitchOffset,
                    arcDegrees,
                    window.distance(),
                    cfg.panelSideOffset,
                    cfg.panelHeightOffset,
                    window.scale());

            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            poses.put(window, pose);
            float aspect = window.aspect();
            float halfWidth = cfg.panelWidth * 0.5f;
            float halfHeight = (cfg.panelWidth / Math.max(0.05f, aspect)) * 0.5f;
            return PanelGeometry.basisOf(pose, halfWidth * window.scale(), halfHeight * window.scale());
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Could not place a spatial panel", t);
            poses.remove(window);
            return null;
        } finally {
            poseStack.popPose();
        }
    }

    /** Keeps the anchor where the player is when the panels are meant to follow them. */
    private void followAnchor(Minecraft mc, SpatialWindow window, MultiSpaceConfig cfg) {
        if (mc.player == null) {
            return;
        }
        float pitch = cfg.perspective == MultiSpaceConfig.Perspective.FIRST_PERSON
                ? clampPitch(mc.gameRenderer.mainCamera().xRot())
                : 0.0f;
        // A fixed-anchor window still gets placed once, at the position it was opened from.
        if (cfg.panelsFollowPlayer || !window.hasAnchor()) {
            window.moveAnchor(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
                    mc.player.getYRot(), pitch);
        }
    }

    private static float clampPitch(float pitch) {
        return Math.max(-60.0f, Math.min(60.0f, pitch));
    }

    private int indexOfActive() {
        SpatialWindow active = WindowManager.get().active();
        return active == null ? 0 : active.layoutIndex();
    }

    // ----------------------------------------------------------------------------------
    // Capture helpers
    // ----------------------------------------------------------------------------------

    /** Mouse position, in this window's GUI units, or -1 when the cursor is off the panel. */
    private int[] mouseFor(SpatialWindow window) {
        int[] result = new int[]{-1, -1};
        QuadBasis basis = bases.get(window);
        if (basis == null) {
            return result;
        }
        Vector3f ray = PanelGeometry.viewRay(projectionMatrix, targeting.cursorX(), targeting.cursorY(),
                frameWidth, frameHeight);
        Vector3f hit = PanelGeometry.intersect(ray, basis);
        if (hit == null) {
            return result;
        }
        // The panel texture maps its top row to v = 1 (see PanelGeometry.emitQuad).
        result[0] = Math.round(hit.x * window.innerWidth());
        result[1] = Math.round((1.0f - hit.y) * window.innerHeight());
        return result;
    }

    /**
     * Lays a window out at the size its content actually needs: container screens are
     * measured through their own image size, everything else gets the configured default.
     */
    public static void ensureSize(Minecraft mc, SpatialWindow window) {
        if (window.innerWidth() > 0) {
            return;
        }
        Screen screen = window.screen();
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        int width = cfg.defaultWindowWidth;
        int height = cfg.defaultWindowHeight;
        if (screen instanceof AbstractContainerScreen<?> container) {
            var accessor = (dev.multispaceui.mixin.gui.AbstractContainerScreenAccessor) container;
            width = accessor.multispaceui$imageWidth();
            height = accessor.multispaceui$imageHeight();
        }
        int fullWidth = Math.max(64, mc.getWindow().getGuiScaledWidth());
        int fullHeight = Math.max(48, mc.getWindow().getGuiScaledHeight());
        window.resize(Math.min(width, fullWidth), Math.min(height, fullHeight));
        window.markLayoutDirty();
        applyLayout(window);
    }

    /** Re-initialises the screen for the current window size, when that size changed. */
    public static void applyLayout(SpatialWindow window) {
        if (!window.consumeLayoutDirty()) {
            return;
        }
        try {
            window.screen().init(window.innerWidth(), window.innerHeight());
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.warn("Could not re-layout {}", window.screenClass(), t);
        }
    }

    // ----------------------------------------------------------------------------------
    // Drawing
    // ----------------------------------------------------------------------------------

    private void drawPanels(List<SpatialWindow> ordered) {
        if (ordered.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        GpuTextureView output = main.getColorTextureView();
        if (output == null || projectionSlice == null) {
            return;
        }

        GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousType = RenderSystem.getProjectionType();
        List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
        List<SpatialWindow> drawn = new ArrayList<>();
        boolean uploaded = false;
        try {
            RenderSystem.setProjectionMatrix(projectionSlice, ProjectionType.PERSPECTIVE);

            MultiSpaceConfig cfg = MultiSpaceConfig.get();
            for (SpatialWindow window : ordered) {
                WindowCapture capture = captures.get(window);
                if (capture == null || capture.target() == null) {
                    continue;
                }
                int alpha = Math.round(window.opacity() * 255.0f);
                if (alpha <= 0) {
                    continue;
                }
                QuadBasis basis = bases.get(window);
                if (basis == null) {
                    continue;
                }
                VertexFormat format = PANEL_PIPELINE.getVertexFormatBinding(0);
                if (format == null) {
                    continue;
                }
                Matrix4f pose = poses.get(window);
                if (pose == null) {
                    continue;
                }
                StagedVertexBuffer.Draw draw = PANEL_BUFFER.appendDraw(format,
                        PANEL_PIPELINE.getPrimitiveTopology(), ProjectionType.PERSPECTIVE.vertexSorting());
                VertexConsumer consumer = PANEL_BUFFER.getVertexBuilder(draw);
                float halfWidth = cfg.panelWidth * 0.5f;
                float halfHeight = (cfg.panelWidth / Math.max(0.05f, window.aspect())) * 0.5f;
                PanelGeometry.emitQuad(consumer, pose, halfWidth, halfHeight, 0x00FFFFFF | (alpha << 24));
                draws.add(draw);
                drawn.add(window);
            }
            PANEL_BUFFER.upload();
            uploaded = true;
            if (drawn.isEmpty()) {
                return;
            }

            FilterMode filter = cfg.linearFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(IDENTITY, WHITE, ZERO, IDENTITY);
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            for (int i = 0; i < drawn.size(); i++) {
                WindowCapture capture = captures.get(drawn.get(i));
                GpuTextureView texture = capture.target().getColorTextureView();
                StagedVertexBuffer.ExecuteInfo info = PANEL_BUFFER.getExecuteInfo(draws.get(i));
                if (texture == null || info == null) {
                    continue;
                }
                try (RenderPass pass = encoder.createRenderPass(
                        () -> "MultiSpaceUI panel", output, Optional.empty(), null, OptionalDouble.empty())) {
                    pass.setPipeline(RenderSystem.getCompiledPipeline(PANEL_PIPELINE));
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setUniform("Sampler0", texture, RenderSystem.getSamplerCache().getSampler(
                            AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, false));
                    pass.setVertexBuffer(0, info.vertexBuffer().slice());
                    pass.setIndexBuffer(info.indexBuffer(), info.indexType());
                    pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
                }
            }
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Failed to draw spatial panels", t);
        } finally {
            if (uploaded || !draws.isEmpty()) {
                PANEL_BUFFER.endFrame();
            }
            RenderSystem.setProjectionMatrix(previousProjection, previousType);
        }
    }
}
