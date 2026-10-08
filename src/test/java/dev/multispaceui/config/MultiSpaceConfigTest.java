package dev.multispaceui.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The configuration the mod ships with, without a running game.
 *
 * <p>The file in {@code config/multispaceui.json} can be edited by hand, so
 * {@link MultiSpaceConfig#normalise()} is the last line of defence: every threshold it clamps
 * here would otherwise become a broken panel (a scale of 0, an opacity above 1, a distance
 * below the player's head, ...).</p>
 */
class MultiSpaceConfigTest {

    // --- defaults -------------------------------------------------------------------

    @Test
    void defaultsAreTheDocumentedOnes() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();

        assertTrue(cfg.enabled, "the mod ships enabled");
        assertTrue(cfg.multiWindowEnabled, "several windows at once are the default");
        assertTrue(cfg.animationsEnabled, "animations are on by default");
        assertEquals(MultiSpaceConfig.Perspective.FIRST_PERSON, cfg.perspective);

        assertEquals(1.6f, cfg.panelDistance, 1.0E-4f, "panelDistance");
        assertEquals(1.25f, cfg.panelWidth, 1.0E-4f, "panelWidth");
        assertEquals(320, cfg.defaultWindowWidth, "defaultWindowWidth");
        assertEquals(240, cfg.defaultWindowHeight, "defaultWindowHeight");
        assertEquals(18.0f, cfg.windowSpacingDegrees, 1.0E-4f, "windowSpacingDegrees");
        assertEquals(0.35f, cfg.backgroundPushBack, 1.0E-4f, "backgroundPushBack");

        assertEquals(1.0f, cfg.baseScale, 1.0E-4f, "baseScale");
        assertEquals(0.35f, cfg.minScale, 1.0E-4f, "minScale");
        assertEquals(1.15f, cfg.maxScale, 1.0E-4f, "maxScale");
        assertEquals(0.12f, cfg.minOpacity, 1.0E-4f, "minOpacity");
        assertEquals(6.0f, cfg.maxDistance, 1.0E-4f, "maxDistance");
        assertEquals(9.0f, cfg.transitionSpeed, 1.0E-4f, "transitionSpeed");

        assertTrue(cfg.crosshairTargeting, "crosshairTargeting");
        assertTrue(cfg.strictClickRouting, "strictClickRouting");
        assertFalse(cfg.closeOnClickOutside,
                "a click outside a panel must never close the container by default");
        assertTrue(cfg.spatialContainers, "spatialContainers");
        assertTrue(cfg.spatialInventory, "spatialInventory");
        assertFalse(cfg.spatialOtherScreens,
                "an unknown screen stays a normal screen until it is whitelisted");
        assertTrue(cfg.includedScreens.isEmpty());
        assertTrue(cfg.excludedScreens.isEmpty());
    }

    @Test
    void normaliseLeavesAHealthyConfigurationUntouched() {
        MultiSpaceConfig expected = new MultiSpaceConfig();
        MultiSpaceConfig actual = new MultiSpaceConfig();
        actual.normalise();

        assertSameValues(expected, actual);
    }

    // --- clampings ------------------------------------------------------------------

    @Test
    void normaliseClampsEveryThresholdToItsRange() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();

        cfg.panelDistance = 0.0f;
        cfg.maxDistance = 1000.0f;
        cfg.windowSpacingDegrees = 0.0f;
        cfg.backgroundPushBack = 50.0f;
        cfg.transitionSpeed = -1.0f;
        cfg.renderScalePercent = 1;
        cfg.defaultWindowWidth = 1;
        cfg.defaultWindowHeight = 100_000;
        cfg.dragSensitivity = 100.0f;
        cfg.worldBlurStrength = 7.0f;
        cfg.taskbarOpacity = 9.0f;
        cfg.normalise();

        assertEquals(0.3f, cfg.panelDistance, 1.0E-4f, "a panel cannot sit inside the camera");
        assertEquals(64.0f, cfg.maxDistance, 1.0E-4f, "maxDistance is capped");
        assertEquals(4.0f, cfg.windowSpacingDegrees, 1.0E-4f, "windows must never overlap at 0 degrees");
        assertEquals(8.0f, cfg.backgroundPushBack, 1.0E-4f, "backgroundPushBack is capped");
        assertEquals(0.0f, cfg.transitionSpeed, 1.0E-4f, "no negative interpolation speed");
        assertEquals(25, cfg.renderScalePercent, "the off-screen buffer keeps a usable resolution");
        assertEquals(96, cfg.defaultWindowWidth, "a window is at least as wide as a slot row");
        assertEquals(1200, cfg.defaultWindowHeight, "a window cannot be taller than the screen");
        assertEquals(5.0f, cfg.dragSensitivity, 1.0E-4f, "dragSensitivity is capped");
        assertEquals(1.0f, cfg.worldBlurStrength, 1.0E-4f, "a blur strength is a 0..1 factor");
        assertEquals(1.0f, cfg.taskbarOpacity, 1.0E-4f, "an opacity is a 0..1 factor");

        cfg.baseScale = 0.0f;
        cfg.maxDistance = -3.0f;
        cfg.renderScalePercent = 5000;
        cfg.normalise();

        assertEquals(0.1f, cfg.baseScale, 1.0E-4f, "a scale never collapses to zero");
        assertEquals(1.0f, cfg.maxDistance, 1.0E-4f, "maxDistance stays positive");
        assertEquals(100, cfg.renderScalePercent, "the buffer is never bigger than the screen");
    }

    @Test
    void normaliseKeepsTheScaleBoundsOrdered() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();
        cfg.baseScale = 50.0f;
        cfg.minScale = 40.0f;
        cfg.maxScale = -4.0f;
        cfg.normalise();

        assertEquals(4.0f, cfg.baseScale, 1.0E-4f, "the base scale is capped");
        assertScaleBoundsOrdered(cfg);

        MultiSpaceConfig squeezed = new MultiSpaceConfig();
        squeezed.baseScale = 1.0f;
        squeezed.minScale = 3.0f;
        squeezed.maxScale = 0.001f;
        squeezed.normalise();

        assertEquals(1.0f, squeezed.minScale, 1.0E-4f, "minScale is dragged down to the base scale");
        assertEquals(1.0f, squeezed.maxScale, 1.0E-4f, "maxScale is dragged up to the base scale");
        assertScaleBoundsOrdered(squeezed);
    }

    private static void assertScaleBoundsOrdered(MultiSpaceConfig cfg) {
        assertTrue(cfg.minScale <= cfg.baseScale,
                "minScale (" + cfg.minScale + ") must not exceed baseScale (" + cfg.baseScale + ")");
        assertTrue(cfg.baseScale <= cfg.maxScale,
                "baseScale (" + cfg.baseScale + ") must not exceed maxScale (" + cfg.maxScale + ")");
    }

    @Test
    void normaliseKeepsTheOpacityBoundsOrderedAndRepairsNullValues() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();
        cfg.minOpacity = 0.9f;
        cfg.maxOpacity = 0.2f;
        cfg.perspective = null;
        cfg.normalise();

        assertTrue(cfg.minOpacity <= cfg.maxOpacity,
                "minOpacity (" + cfg.minOpacity + ") must not exceed maxOpacity (" + cfg.maxOpacity + ")");
        assertEquals(0.9f, cfg.maxOpacity, 1.0E-4f, "maxOpacity is dragged up to minOpacity");
        assertEquals(MultiSpaceConfig.Perspective.FIRST_PERSON, cfg.perspective,
                "a missing perspective falls back to the first person camera");
    }

    @Test
    void intensityWeightsNeverLeaveTheirZeroToOneRange() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();
        cfg.focusScaleIntensity = -1.0f;
        cfg.focusFadeIntensity = 3.0f;
        cfg.crosshairBoost = 9.0f;
        cfg.normalise();

        assertEquals(0.0f, cfg.focusScaleIntensity, 1.0E-4f);
        assertEquals(1.0f, cfg.focusFadeIntensity, 1.0E-4f);
        assertEquals(1.0f, cfg.crosshairBoost, 1.0E-4f);
    }

    // --- screen black and white lists -----------------------------------------------

    @Test
    void screenListsSurviveAJsonRoundTrip() {
        MultiSpaceConfig cfg = new MultiSpaceConfig();
        cfg.includedScreens.add("com.example.mod.FancyScreen");
        cfg.excludedScreens.add("net.minecraft.client.gui.screens.ChatScreen");

        cfg.normalise();

        JsonObject json = cfg.toJson().getAsJsonObject();
        JsonArray included = json.getAsJsonArray("includedScreens");
        JsonArray excluded = json.getAsJsonArray("excludedScreens");

        assertTrue(included.contains(new JsonPrimitive("com.example.mod.FancyScreen")),
                "the whitelist must be written out");
        assertTrue(excluded.contains(new JsonPrimitive("net.minecraft.client.gui.screens.ChatScreen")),
                "the blacklist must be written out");
        assertEquals(1, cfg.excludedList().size(), "excludedList() exposes the blacklist");
        assertFalse(cfg.excludedList().isEmpty());
    }

    /** Every field {@link MultiSpaceConfig#normalise()} is allowed to rewrite. */
    private static void assertSameValues(MultiSpaceConfig expected, MultiSpaceConfig actual) {
        assertEquals(expected.perspective, actual.perspective, "perspective");
        assertEquals(expected.panelDistance, actual.panelDistance, 1.0E-4f, "panelDistance");
        assertEquals(expected.panelWidth, actual.panelWidth, 1.0E-4f, "panelWidth");
        assertEquals(expected.defaultWindowWidth, actual.defaultWindowWidth, "defaultWindowWidth");
        assertEquals(expected.defaultWindowHeight, actual.defaultWindowHeight, "defaultWindowHeight");
        assertEquals(expected.windowSpacingDegrees, actual.windowSpacingDegrees, 1.0E-4f, "windowSpacingDegrees");
        assertEquals(expected.backgroundPushBack, actual.backgroundPushBack, 1.0E-4f, "backgroundPushBack");
        assertEquals(expected.baseScale, actual.baseScale, 1.0E-4f, "baseScale");
        assertEquals(expected.minScale, actual.minScale, 1.0E-4f, "minScale");
        assertEquals(expected.maxScale, actual.maxScale, 1.0E-4f, "maxScale");
        assertEquals(expected.minOpacity, actual.minOpacity, 1.0E-4f, "minOpacity");
        assertEquals(expected.maxOpacity, actual.maxOpacity, 1.0E-4f, "maxOpacity");
        assertEquals(expected.maxDistance, actual.maxDistance, 1.0E-4f, "maxDistance");
        assertEquals(expected.focusScaleIntensity, actual.focusScaleIntensity, 1.0E-4f, "focusScaleIntensity");
        assertEquals(expected.focusFadeIntensity, actual.focusFadeIntensity, 1.0E-4f, "focusFadeIntensity");
        assertEquals(expected.crosshairBoost, actual.crosshairBoost, 1.0E-4f, "crosshairBoost");
        assertEquals(expected.transitionSpeed, actual.transitionSpeed, 1.0E-4f, "transitionSpeed");
        assertEquals(expected.renderScalePercent, actual.renderScalePercent, "renderScalePercent");
        assertEquals(expected.taskbarScale, actual.taskbarScale, 1.0E-4f, "taskbarScale");
        assertEquals(expected.taskbarOpacity, actual.taskbarOpacity, 1.0E-4f, "taskbarOpacity");
        assertEquals(expected.dragSensitivity, actual.dragSensitivity, 1.0E-4f, "dragSensitivity");
        assertEquals(expected.worldBlurStrength, actual.worldBlurStrength, 1.0E-4f, "worldBlurStrength");
    }
}
