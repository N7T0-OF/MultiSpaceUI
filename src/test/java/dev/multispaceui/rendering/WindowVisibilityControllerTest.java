package dev.multispaceui.rendering;

import dev.multispaceui.config.MultiSpaceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The priority system of the mod, without a running game.
 *
 * <p>These are the rules the whole mod is built on: the window the player looks at keeps its
 * size and its opacity, the others shrink and fade with distance, and the focused window is
 * protected from the distance falloff.</p>
 */
class WindowVisibilityControllerTest {

    /** Focus weight of a window the crosshair points at. */
    private static final float FOCUSED = 1.0f;
    /** Focus weight of a plain background window. */
    private static final float BACKGROUND = 0.0f;

    private MultiSpaceConfig cfg;

    @BeforeEach
    void setUp() {
        cfg = new MultiSpaceConfig();
        cfg.normalise();
    }

    // --- focus priority --------------------------------------------------------------

    @Test
    void focusedWindowIgnoresTheDistanceFalloff() {
        assertEquals(1.0f, WindowVisibilityController.distanceWeight(cfg.maxDistance, FOCUSED, cfg),
                "a focused window must keep its full size at any distance");
        assertEquals(1.0f, WindowVisibilityController.distanceOpacity(cfg.maxDistance, FOCUSED, cfg),
                "a focused window must stay opaque at any distance");
    }

    @Test
    void focusedWindowIsStillVisibleAtMaxDistance() {
        WindowVisibilityController.Presentation presentation =
                WindowVisibilityController.present(FOCUSED, cfg.maxDistance, false, false, cfg);
        assertTrue(presentation.visible(), "the window being looked at must never vanish");
        assertTrue(presentation.interactive());
        assertEquals(cfg.baseScale, presentation.scale(), 1.0E-3f);
        assertEquals(cfg.maxOpacity, presentation.opacity(), 1.0E-3f);
    }

    @Test
    void backgroundWindowFadesAwayAtMaxDistance() {
        assertEquals(0.0f, WindowVisibilityController.distanceWeight(cfg.maxDistance, BACKGROUND, cfg),
                1.0E-4f);
        assertEquals(0.0f, WindowVisibilityController.distanceOpacity(cfg.maxDistance, BACKGROUND, cfg),
                1.0E-4f);

        WindowVisibilityController.Presentation presentation =
                WindowVisibilityController.present(BACKGROUND, cfg.maxDistance, false, false, cfg);
        assertFalse(presentation.visible(), "a distant background window must disappear");
        assertFalse(presentation.interactive());
    }

    @Test
    void panelsWithinThePanelDistanceNeverFallOff() {
        assertEquals(1.0f, WindowVisibilityController.distanceWeight(cfg.panelDistance, BACKGROUND, cfg));
        assertEquals(1.0f, WindowVisibilityController.distanceOpacity(cfg.panelDistance, BACKGROUND, cfg));
    }

    // --- the focused window wins over the background one -----------------------------

    @Test
    void focusedWindowIsLargerAndMoreOpaqueThanTheBackgroundOne() {
        float distance = cfg.panelDistance + 0.4f;
        WindowVisibilityController.Presentation focused =
                WindowVisibilityController.present(FOCUSED, distance, false, false, cfg);
        WindowVisibilityController.Presentation background =
                WindowVisibilityController.present(BACKGROUND, distance, false, false, cfg);

        assertTrue(background.scale() < focused.scale(),
                "the background window must be smaller than the focused one");
        assertTrue(background.opacity() < focused.opacity(),
                "the background window must be more transparent than the focused one");
        assertTrue(background.visible(), "a window just behind the panel must stay readable");
        assertFalse(background.interactive(),
                "a window that is not focused must never receive clicks");
    }

    @Test
    void activeButUnfocusedWindowStaysInteractive() {
        // 0.6 is the weight of the window the game is talking to while the player looks elsewhere.
        WindowVisibilityController.Presentation active =
                WindowVisibilityController.present(0.6f, cfg.panelDistance, false, false, cfg);
        assertTrue(active.visible());
        assertTrue(active.interactive());
        assertTrue(active.scale() < WindowVisibilityController
                .present(FOCUSED, cfg.panelDistance, false, false, cfg).scale());
    }

    // --- lifecycle -------------------------------------------------------------------

    @Test
    void minimizedWindowIsNeverVisible() {
        WindowVisibilityController.Presentation minimized =
                WindowVisibilityController.present(FOCUSED, cfg.panelDistance, true, false, cfg);
        assertFalse(minimized.visible());
        assertFalse(minimized.interactive());
    }

    @Test
    void closingWindowStaysVisibleButStopsReceivingInput() {
        WindowVisibilityController.Presentation closing =
                WindowVisibilityController.present(FOCUSED, cfg.panelDistance, false, true, cfg);
        assertTrue(closing.visible(), "the close animation must still be seen");
        assertFalse(closing.interactive());
    }

    // --- bounds ----------------------------------------------------------------------

    @Test
    void presentationAlwaysStaysInsideTheConfiguredBounds() {
        for (float weight = 0.0f; weight <= 1.001f; weight += 0.1f) {
            for (float distance = cfg.panelDistance; distance <= cfg.maxDistance + 2.0f; distance += 0.5f) {
                WindowVisibilityController.Presentation presentation =
                        WindowVisibilityController.present(weight, distance, false, false, cfg);
                String at = "weight=" + weight + " distance=" + distance;
                assertTrue(presentation.scale() >= cfg.minScale - 1.0E-4f, "scale too small " + at);
                assertTrue(presentation.scale() <= cfg.maxScale + 1.0E-4f, "scale too large " + at);
                assertTrue(presentation.opacity() >= 0.0f, "negative opacity " + at);
                assertTrue(presentation.opacity() <= cfg.maxOpacity + 1.0E-4f, "opacity too large " + at);
                assertTrue(presentation.distance() == distance, "distance must be reported back");
            }
        }
    }

    @Test
    void scaleAndOpacityAreMonotonicInFocus() {
        float previousScale = -1.0f;
        float previousOpacity = -1.0f;
        for (float weight = 0.0f; weight <= 1.001f; weight += 0.1f) {
            WindowVisibilityController.Presentation presentation =
                    WindowVisibilityController.present(weight, cfg.panelDistance + 1.0f, false, false, cfg);
            assertTrue(presentation.scale() >= previousScale - 1.0E-4f,
                    "gaining focus must never shrink a window");
            assertTrue(presentation.opacity() >= previousOpacity - 1.0E-4f,
                    "gaining focus must never dim a window");
            previousScale = presentation.scale();
            previousOpacity = presentation.opacity();
        }
    }

    // --- focus beats distance ----------------------------------------------------------

    @Test
    void focusedWindowIsNeverHiddenByTheDistanceRule() {
        float tooFar = cfg.maxDistance * 3.0f;

        WindowVisibilityController.Presentation focused =
                WindowVisibilityController.present(FOCUSED, tooFar, false, false, cfg);
        assertTrue(focused.visible(),
                "the window being looked at must stay on screen even past maxDistance");
        assertTrue(focused.interactive(), "and it must keep answering to the pointer");
        assertEquals(cfg.baseScale, focused.scale(), 1.0E-3f, "and keep its size");
        assertEquals(cfg.maxOpacity, focused.opacity(), 1.0E-3f, "and its opacity");

        WindowVisibilityController.Presentation background =
                WindowVisibilityController.present(BACKGROUND, tooFar, false, false, cfg);
        assertFalse(background.visible(), "a plain background window must vanish past maxDistance");
        assertFalse(background.interactive(), "and must stop receiving input");
    }
}
