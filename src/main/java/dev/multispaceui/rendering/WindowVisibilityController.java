package dev.multispaceui.rendering;

import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.window.SpatialWindow;

/**
 * The priority system of the mod.
 *
 * <p>Every frame, for every window, this controller turns three inputs — focus, distance and
 * orientation relative to the look direction — into the presentation the window should reach:
 *
 * <pre>
 *   targetScale   = baseScale × focusScale × distanceScale × orientation
 *   targetOpacity = baseOpacity × focusOpacity × distanceOpacity
 *   interactive   = visible &amp; prioritised &amp; within maxDistance
 * </pre>
 *
 * <p>These are targets, not applied values: {@link SpatialWindow#interpolate} smooths them
 * over time so windows never pop between states. The focused window is protected from the
 * distance falloff — looking at a distant panel keeps it full size and fully opaque.</p>
 */
public final class WindowVisibilityController {

    /** Result of one evaluation. */
    public record Presentation(float scale, float opacity, float distance, boolean visible, boolean interactive) {
    }

    /**
     * @param window         window being evaluated
     * @param focused        window targeted by the crosshair / gaze
     * @param hovered        window under the cursor
     * @param active         window currently installed as the Minecraft screen
     * @param cameraDistance distance from the camera to the panel centre, in blocks
     */
    public Presentation evaluate(SpatialWindow window,
                                 SpatialWindow focused,
                                 SpatialWindow hovered,
                                 SpatialWindow active,
                                 float cameraDistance) {
        return present(focusWeight(window, focused, hovered, active), cameraDistance,
                window.isMinimized(), window.isClosing(), MultiSpaceConfig.get());
    }

    /**
     * Presentation of a window from its focus weight, in blocks.
     *
     * <p>Kept free of any Minecraft type so the whole priority system can be verified by unit
     * tests as well as in the world. Priority order: focus beats distance, so the window the
     * player looks at is never cut off by {@code maxDistance} even when it is further away.</p>
     *
     * @param focusWeight    see {@link #focusWeight}
     * @param cameraDistance distance from the camera to the panel centre, in blocks
     * @param minimized      the window was folded by the player
     * @param closing        the window is playing its close animation
     * @param cfg            configuration to evaluate against
     */
    public static Presentation present(float focusWeight, float cameraDistance,
                                       boolean minimized, boolean closing, MultiSpaceConfig cfg) {
        float distanceWeight = distanceWeight(cameraDistance, focusWeight, cfg);
        float orientation = focusWeight >= 0.999f ? 1.0f : 1.0f - 0.15f * cfg.focusScaleIntensity;

        float scale = cfg.baseScale * focusScale(focusWeight, cfg) * distanceWeight * orientation;
        scale = clamp(scale, cfg.minScale, cfg.maxScale);

        float opacity = cfg.maxOpacity * focusOpacity(focusWeight, cfg)
                * distanceOpacity(cameraDistance, focusWeight, cfg);
        opacity = clamp(opacity, 0.0f, cfg.maxOpacity);

        boolean visible = opacity > 0.01f
                && (focusWeight >= 0.999f || cameraDistance <= cfg.maxDistance)
                && !minimized;
        boolean interactive = visible && focusWeight >= 0.5f && !closing;

        return new Presentation(scale, opacity, cameraDistance, visible, interactive);
    }

    /**
     * 1 = the player looks straight at it, 0.75 = cursor hovers it, 0.6 = it is the window
     * the game is currently talking to, 0 = plain background window.
     */
    public static float focusWeight(SpatialWindow window, SpatialWindow focused, SpatialWindow hovered, SpatialWindow active) {
        if (window == focused) {
            return 1.0f;
        }
        if (window == hovered) {
            return 0.75f;
        }
        if (window == active) {
            return 0.6f;
        }
        return 0.0f;
    }

    /** Falls off with distance; a focused window ignores the falloff entirely. */
    public static float distanceWeight(float distance, float focusWeight, MultiSpaceConfig cfg) {
        if (focusWeight >= 0.999f || distance <= cfg.panelDistance) {
            return 1.0f;
        }
        float t = normaliseDistance(distance, cfg);
        float falloff = smoothstep(1.0f - t);
        return lerp(falloff, 1.0f, focusWeight);
    }

    public static float distanceOpacity(float distance, float focusWeight, MultiSpaceConfig cfg) {
        if (focusWeight >= 0.999f || distance <= cfg.panelDistance) {
            return 1.0f;
        }
        float t = normaliseDistance(distance, cfg);
        float falloff = smoothstep(1.0f - t);
        return lerp(falloff, 1.0f, focusWeight * 0.5f);
    }

    /** Panels the player is not looking at lose a bit of size on top of the focus falloff. */
    public static float orientationWeight(SpatialWindow window, SpatialWindow focused, MultiSpaceConfig cfg) {
        if (window == focused) {
            return 1.0f;
        }
        return 1.0f - 0.15f * cfg.focusScaleIntensity;
    }

    public static float focusOpacity(float focusWeight, MultiSpaceConfig cfg) {
        float attenuated = 1.0f - cfg.focusFadeIntensity * (1.0f - focusWeight);
        return clamp(attenuated, cfg.minOpacity, 1.0f);
    }

    public static float focusScale(float focusWeight, MultiSpaceConfig cfg) {
        float attenuated = 1.0f - cfg.focusScaleIntensity * (1.0f - focusWeight);
        return clamp(attenuated, 0.05f, 1.0f);
    }

    private static float normaliseDistance(float distance, MultiSpaceConfig cfg) {
        float span = Math.max(0.001f, cfg.maxDistance - cfg.panelDistance);
        return clamp((distance - cfg.panelDistance) / span, 0.0f, 1.0f);
    }

    private static float smoothstep(float v) {
        float t = clamp(v, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
