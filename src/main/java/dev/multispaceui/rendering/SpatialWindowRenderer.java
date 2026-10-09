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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.util.RenderUtil;

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

    /**
     * Where the cursor lands inside this window's panel, in the GUI units of its screen.
     *
     * @return {@code {x, y}}, or {@code null} when the cursor is not over the panel - which
     *         is exactly the distinction a dropped click needs, so this never clamps the
     *         answer onto the border of the texture the way the base's mapper does
     */
    public double[] cursorOn(SpatialWindow window) {
        QuadBasis basis = bases.get(window);
        if (basis == null || !hasFrame) {
            return null;
        }
        Vector3f ray = PanelGeometry.viewRay(projectionMatrix, targeting.cursorX(),
                targeting.cursorY(), frameWidth, frameHeight);
        // Bounded on purpose: a cursor anywhere in the world is off the panel, and off the
        // panel is what lets a click be dropped instead of being routed to the screen.
        Vector3f hit = PanelGeometry.intersect(ray, basis);
        if (hit == null) {
            return null;
        }
        return new double[]{hit.x() * window.innerWidth(), (1.0f - hit.y()) * window.innerHeight()};
    }

    /**
     * True once the screen of this window has been extracted into its own texture.
     *
     * <p>While the Spatial GUI base draws the active window itself, that texture is its
     * target and not ours, so the two are accepted indifferently.</p>
     */
    public boolean hasCaptureContent(SpatialWindow window) {
        WindowCapture capture = captures.get(window);
        if (capture != null && capture.hasTarget()) {
            return true;
        }
        if (window == WindowManager.get().active() && baseRendersActive()) {
            SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
            return renderer != null && renderer.getTargetManager().getInventoryTarget() != null;
        }
        return false;
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
            // `panelDistance` stays the distance at which the falloff starts (a knob of our
            // own config, in blocks), not the distance the base places panels at - the two
            // are independent, so a player who moves the Spatial GUI panel closer does not
            // silently change how fast the background windows fade.
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
        //    The window the player is talking to is placed as well, so picking and focus
        //    switching can see every panel - but the Spatial GUI base draws that one, from
        //    its own isolated texture, so it only leaves the list of panels to draw here.
        SpatialWindow active = manager.active();
        boolean baseOwnsActive = active != null && baseRendersActive();
        List<SpatialWindow> drawable = new ArrayList<>();
        for (SpatialWindow window : manager.windows()) {
            if (!window.isVisible() || window.isMinimized() || window.isClosing()) {
                bases.remove(window);
                poses.remove(window);
                continue;
            }
            QuadBasis basis = place(mc, window, window == active);
            if (basis != null) {
                bases.put(window, basis);
                if (window != active || !baseOwnsActive) {
                    drawable.add(window);
                }
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

    /**
     * True while the Spatial GUI base is drawing the active window itself: it has hooked
     * that screen, it is installed as the Minecraft screen, and its own target is the one
     * showing its content. MultiSpaceUI then only draws the background windows, so a panel
     * is never rendered twice.
     *
     * <p>When the base cannot take it (its config is off, the screen is folded away, the
     * screen is not one the base hooks) the active window stays in our own list and is
     * drawn from our capture like any other one.</p>
     */
    public static boolean baseRendersActive() {
        if (!SpatialGUIClient.isEnabled()) {
            return false;
        }
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
        return renderer != null && renderer.shouldCapture();
    }

    /**
     * Places one panel in view space.
     *
     * <p>The base transform - distance, side / height offsets, yaw offset and scale - comes
     * straight from {@link RenderUtil#getScreenTransformConfig} and
     * {@link RenderUtil#applyScreenTransform}, the very same calls Spatial GUI uses for its
     * own panel, so the window the player talks to sits exactly where the base draws it.
     * The extra ingredients are ours: the fan angle that spreads the other windows around
     * it, the push back that separates them, and the focus / distance scale.</p>
     *
     * <p>Result space is <b>view space</b>: the captured view rotation is applied first, so
     * the camera sits at the origin and -Z looks forward, which is the space the picking
     * rays are built in.</p>
     */
    private QuadBasis place(Minecraft mc, SpatialWindow window, boolean isActive) {
        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        int activeIndex = indexOfActive();
        float arcDegrees = isActive ? 0.0f : (window.layoutIndex() - activeIndex) * cfg.windowSpacingDegrees;

        // The panel the Spatial GUI base is drawing right now defines its own geometry:
        // its FOV reading, its open animation and its camera-relative anchor are all taken
        // from the pass that actually renders it. Borrowing that basis means the window the
        // player talks to is picked exactly where it is displayed - and a background panel
        // is sized by the very same scale, so the fan never mixes two different panel sizes.
        QuadBasis drawn = isActive && baseRendersActive() ? baseBasisInViewSpace() : null;
        if (drawn != null) {
            // The base draws it, so no pose of ours is needed for it.
            poses.remove(window);
            return drawn;
        }

        boolean firstPerson = SpatialGUIClient.getEffectiveFirstPersonMode();
        RenderUtil.ScreenTransformConfig base = RenderUtil.getScreenTransformConfig(firstPerson);
        float extraDistance = isActive ? 0.0f : cfg.backgroundPushBack;
        RenderUtil.ScreenTransformConfig transform = extraDistance == 0.0f ? base
                : new RenderUtil.ScreenTransformConfig(base.distance() + extraDistance,
                        base.sideOffset(), base.heightOffset(),
                        base.yawOffset(), base.pitchOffset(), base.scale());

        float yawRad = (float) Math.toRadians(mc.player.getYRot());
        float pitchRad = firstPerson ? (float) Math.toRadians(clampPitch(mc.gameRenderer.mainCamera().xRot())) : 0.0f;
        float lookX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = (float) (-Math.sin(pitchRad));
        float lookZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        float panelScale = baseQuadScale();
        if (panelScale <= 0.0f) {
            panelScale = transform.scale();
        }

        poseStack.pushPose();
        try {
            // View space first, then the world anchor of this window (zero while the panels
            // follow the player), then the fan rotation, then the base placement itself.
            poseStack.mulPose(viewRotation);
            poseStack.translate(window.anchorX() - cameraX, window.anchorY() - cameraY,
                    window.anchorZ() - cameraZ);
            if (arcDegrees != 0.0f) {
                // PoseStack takes a matrix on 26.3, exactly like the base's own transform.
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(arcDegrees)).get(new Matrix4f()));
            }
            RenderUtil.applyScreenTransform(poseStack, firstPerson, yawRad, pitchRad,
                    transform, lookX, lookY, lookZ);
            // The scale comes from the base when it can be read: same panel, same size.
            float visibilityScale = isActive ? 1.0f : window.scale();
            float totalScale = panelScale * visibilityScale;
            poseStack.scale(totalScale, totalScale, totalScale);

            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            poses.put(window, pose);
            float aspect = Math.max(0.05f, window.aspect());
            return PanelGeometry.basisOf(pose, aspect * 0.5f * totalScale, 0.5f * totalScale);
        } catch (Throwable t) {
            MultiSpaceUI.LOGGER.error("Could not place a spatial panel", t);
            poses.remove(window);
            return null;
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Quad the Spatial GUI base is drawing its active panel with, rotated into view space,
     * or {@code null} when the base is not drawing anything.
     *
     * <p>The base keeps that basis in world space (a camera-relative frame with no view
     * rotation), which is exactly how its ray based mouse mapping reads it. Rotating it with
     * the view rotation of this frame puts it in the space our own picking rays live in, so
     * a click and the drawn panel can no longer drift apart.</p>
     */
    private QuadBasis baseBasisInViewSpace() {
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
        if (renderer == null || !renderer.shouldCapture()) {
            return null;
        }
        var base = renderer.getInventoryRenderer().getQuadBasis();
        if (base == null || base.halfWidth() <= 0.0f || base.halfHeight() <= 0.0f) {
            return null;
        }
        Matrix4f toView = new Matrix4f(viewRotation);
        Vector3f center = new Vector3f(base.centerOffset()).mulPosition(toView);
        Vector3f right = new Vector3f(base.right()).mulDirection(toView);
        Vector3f up = new Vector3f(base.up()).mulDirection(toView);
        Vector3f normal = new Vector3f(base.normal()).mulDirection(toView);
        return new QuadBasis(center, right, up, normal, base.halfWidth(), base.halfHeight());
    }

    /** Size multiplier the base currently draws with, or a negative value when unknown. */
    private float baseQuadScale() {
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
        if (renderer == null || !renderer.shouldCapture()) {
            return -1.0f;
        }
        var base = renderer.getInventoryRenderer().getQuadBasis();
        return base == null ? -1.0f : base.halfHeight() * 2.0f;
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
     * Measures a window at the size its screen actually laid itself out for.
     *
     * <p>Every window holds a full-screen sized texture, exactly like the target the
     * Spatial GUI base extracts its own screen into: a background panel is then made
     * smaller by the focus / distance presentation instead of being re-laid out for a
     * smaller box. Reading the screen's own width and height - rather than the game
     * window's - keeps the texture and the widgets inside it in agreement even when the
     * base overrides the GUI scale for a hooked screen.</p>
     */
    public static void ensureSize(Minecraft mc, SpatialWindow window) {
        if (window.innerWidth() > 0) {
            return;
        }
        Screen screen = window.screen();
        int width = screen == null ? 0 : screen.width;
        int height = screen == null ? 0 : screen.height;
        if (width <= 0 || height <= 0) {
            width = Math.max(64, mc.getWindow().getGuiScaledWidth());
            height = Math.max(48, mc.getWindow().getGuiScaledHeight());
        }
        window.resize(width, height);
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
                // The pose already carries the scale: this is the unit quad the base draws,
                // sized so that its aspect matches the texture it is filled with.
                float halfWidth = Math.max(0.05f, window.aspect()) * 0.5f;
                float halfHeight = 0.5f;
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
