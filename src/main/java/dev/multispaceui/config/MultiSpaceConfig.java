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
    /** Reference distance of a window, in blocks: how far it drifts before fading. */
    public float panelDistance = 1.6f;
    /** Anchors follow the player; when false the panels stay in the world where they opened. */
    public boolean panelsFollowPlayer = true;
    /** Angular spacing between two consecutive windows of the fan layout, degrees. */
    public float windowSpacingDegrees = 18.0f;
    /** Extra depth pushed onto windows that are not the active one. */
    public float backgroundPushBack = 0.35f;

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
    /** Interpolation speed of every animated value, higher = snappier. */
    public float transitionSpeed = 9.0f;

    // --- interaction -------------------------------------------------------------
    public boolean crosshairTargeting = true;
    /** Clicking the world through a gap between panels does not close the container. */
    public boolean closeOnClickOutside = false;

    // --- rendering options -------------------------------------------------------
    /** Off-screen buffer resolution of a background window, in percent of its size. */
    public int renderScalePercent = 100;
    public boolean linearFiltering = true;

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
        minOpacity = clamp(minOpacity, 0.0f, 1.0f);
        maxOpacity = clamp(maxOpacity, minOpacity, 1.0f);
        maxDistance = clamp(maxDistance, 1.0f, 64.0f);
        focusScaleIntensity = clamp(focusScaleIntensity, 0.0f, 1.0f);
        focusFadeIntensity = clamp(focusFadeIntensity, 0.0f, 1.0f);
        transitionSpeed = clamp(transitionSpeed, 0.0f, 60.0f);
        panelDistance = clamp(panelDistance, 0.3f, 16.0f);
        windowSpacingDegrees = clamp(windowSpacingDegrees, 4.0f, 90.0f);
        backgroundPushBack = clamp(backgroundPushBack, 0.0f, 8.0f);
        renderScalePercent = (int) clamp(renderScalePercent, 25, 100);
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
