package dev.multispaceui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.multispaceui.MultiSpaceUI;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent client configuration.
 *
 * <p>All thresholds exposed here are starting values, not hard limits: the visibility
 * controller ({@code WindowVisibilityController}) smooths every transition, so changing a
 * value never produces a jump on screen.</p>
 */
public final class MultiSpaceConfig {

    public enum Perspective {
        FIRST_PERSON,
        THIRD_PERSON
    }

    // --- global -----------------------------------------------------------------
    public boolean enabled = true;
    public Perspective perspective = Perspective.FIRST_PERSON;
    public boolean multiWindowEnabled = true;
    public boolean animationsEnabled = true;

    // --- panel placement (world units, degrees) ----------------------------------
    public float panelDistance = 1.6f;
    public float panelSideOffset = 0.0f;
    public float panelHeightOffset = 0.0f;
    public float panelYawOffset = 0.0f;
    public float panelPitchOffset = 0.0f;
    /** Width of a panel, in blocks, at full scale; the height follows the window aspect. */
    public float panelWidth = 1.25f;
    /** Anchors follow the player; when false the panels stay in the world where they opened. */
    public boolean panelsFollowPlayer = true;
    /** Angular spacing between two consecutive windows of the fan layout, degrees. */
    public float windowSpacingDegrees = 18.0f;
    /** Extra depth pushed onto windows that are not the active one. */
    public float backgroundPushBack = 0.35f;
    /** Allows the edges of a panel to be dragged to resize it. */
    public boolean resizeEnabled = true;

    // --- default window size for screens that have no natural size ----------------
    public int defaultWindowWidth = 320;
    public int defaultWindowHeight = 240;

    // --- size and opacity --------------------------------------------------------
    /** Size multiplier of the focused window. */
    public float baseScale = 1.0f;
    public float minScale = 0.35f;
    public float maxScale = 1.15f;
    public float minOpacity = 0.12f;
    public float maxOpacity = 1.0f;

    // --- priority system ---------------------------------------------------------
    /** Distance beyond which a window becomes invisible and inert, in blocks. */
    public float maxDistance = 6.0f;
    /** How strongly a non-focused window shrinks: 0 = none, 1 = down to {@link #minScale}. */
    public float focusScaleIntensity = 0.75f;
    /** How strongly a non-focused window fades: 0 = none, 1 = down to {@link #minOpacity}. */
    public float focusFadeIntensity = 0.85f;
    /** Extra priority granted to the window targeted by the crosshair. */
    public float crosshairBoost = 1.0f;
    /** Interpolation speed of every animated value, higher = snappier. */
    public float transitionSpeed = 9.0f;

    // --- interaction -------------------------------------------------------------
    public boolean crosshairTargeting = true;
    /** Only the focused window reacts to clicks. */
    public boolean strictClickRouting = true;
    /** Clicking the world through a gap between panels does not close the container. */
    public boolean closeOnClickOutside = false;
    public float dragSensitivity = 1.0f;

    // --- rendering options -------------------------------------------------------
    public boolean blurBackground = false;
    public float worldBlurStrength = 0.6f;
    public boolean showHud = true;
    public boolean showHotbar = true;
    public boolean showCrosshair = true;
    /** Off-screen buffer resolution, in percent of the window size. */
    public int renderScalePercent = 100;
    public boolean linearFiltering = true;

    // --- taskbar -----------------------------------------------------------------
    public boolean showTaskbar = true;
    public float taskbarScale = 1.0f;
    public float taskbarOpacity = 0.95f;
    public float taskbarYOffset = 6.0f;

    // --- screen compatibility ----------------------------------------------------
    /**
     * Screens forced into a spatial window even when their category is disabled.
     * Screen is identified by its fully qualified class name.
     */
    public final Set<String> includedScreens = new LinkedHashSet<>();
    /** Screens that are never turned into a spatial window. */
    public final Set<String> excludedScreens = new LinkedHashSet<>();
    public boolean spatialContainers = true;
    public boolean spatialInventory = true;
    /** Anything else (mod screens): only when explicitly whitelisted, unless this is on. */
    public boolean spatialOtherScreens = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "multispaceui.json";

    private static MultiSpaceConfig instance = new MultiSpaceConfig();

    public static MultiSpaceConfig get() {
        return instance;
    }

    public static Path filePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = filePath();
        if (!Files.exists(path)) {
            instance = new MultiSpaceConfig();
            save();
            return;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            MultiSpaceConfig loaded = GSON.fromJson(raw, MultiSpaceConfig.class);
            instance = loaded != null ? loaded : new MultiSpaceConfig();
            instance.normalise();
        } catch (Exception e) {
            MultiSpaceUI.LOGGER.error("Could not read {}, falling back to defaults", path, e);
            instance = new MultiSpaceConfig();
        }
    }

    public static void save() {
        instance.normalise();
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MultiSpaceUI.LOGGER.error("Could not write {}", path, e);
        }
    }

    /** Clamps everything to sane ranges so a hand-edited file cannot break rendering. */
    public void normalise() {
        perspective = perspective == null ? Perspective.FIRST_PERSON : perspective;
        baseScale = clamp(baseScale, 0.1f, 4.0f);
        minScale = clamp(minScale, 0.05f, baseScale);
        maxScale = clamp(maxScale, baseScale, 4.0f);
        panelWidth = clamp(panelWidth, 0.2f, 8.0f);
        defaultWindowWidth = (int) clamp(defaultWindowWidth, 96, 1600);
        defaultWindowHeight = (int) clamp(defaultWindowHeight, 64, 1200);
        minOpacity = clamp(minOpacity, 0.0f, 1.0f);
        maxOpacity = clamp(maxOpacity, minOpacity, 1.0f);
        maxDistance = clamp(maxDistance, 1.0f, 64.0f);
        focusScaleIntensity = clamp(focusScaleIntensity, 0.0f, 1.0f);
        focusFadeIntensity = clamp(focusFadeIntensity, 0.0f, 1.0f);
        crosshairBoost = clamp(crosshairBoost, 0.0f, 1.0f);
        transitionSpeed = clamp(transitionSpeed, 0.0f, 60.0f);
        panelDistance = clamp(panelDistance, 0.3f, 16.0f);
        windowSpacingDegrees = clamp(windowSpacingDegrees, 4.0f, 90.0f);
        backgroundPushBack = clamp(backgroundPushBack, 0.0f, 8.0f);
        renderScalePercent = (int) clamp(renderScalePercent, 25, 100);
        taskbarScale = clamp(taskbarScale, 0.4f, 3.0f);
        taskbarOpacity = clamp(taskbarOpacity, 0.0f, 1.0f);
        dragSensitivity = clamp(dragSensitivity, 0.1f, 5.0f);
        worldBlurStrength = clamp(worldBlurStrength, 0.0f, 1.0f);
        if (includedScreens == null || excludedScreens == null) {
            throw new IllegalStateException("screen lists must not be null");
        }
    }

    public List<String> excludedList() {
        return new ArrayList<>(excludedScreens);
    }

    public JsonElement toJson() {
        JsonObject o = GSON.toJsonTree(this).getAsJsonObject();
        JsonArray in = new JsonArray();
        includedScreens.forEach(in::add);
        o.add("includedScreens", in);
        JsonArray out = new JsonArray();
        excludedScreens.forEach(out::add);
        o.add("excludedScreens", out);
        return o;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
