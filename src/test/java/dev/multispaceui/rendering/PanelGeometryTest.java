package dev.multispaceui.rendering;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cursor geometry of a panel: projecting it, and casting a ray back onto it.
 *
 * <p>The two projections below are the two depth conventions the mod has to survive: the
 * classic one (near at -1, far at +1) and the reversed, infinite one recent Minecraft
 * versions render with (near at 1, far at 0). Casting the ray with the wrong convention
 * yields a direction pointing back at the camera, every intersection then reports
 * {@code t < 0} and no click would ever reach a window.</p>
 */
class PanelGeometryTest {

    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;
    private static final float FOV = (float) Math.toRadians(70.0);
    private static final float Z_NEAR = 0.05f;
    private static final float Z_FAR = 100.0f;

    /** A panel centred in front of the camera, one and a half blocks away. */
    private static final PanelGeometry.QuadBasis PANEL = new PanelGeometry.QuadBasis(
            new Vector3f(0.0f, 0.0f, -1.6f),
            new Vector3f(1.0f, 0.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(0.0f, 0.0f, 1.0f),
            0.625f,
            0.59f);

    private static List<Matrix4f> bothConventions() {
        return List.of(standardProjection(), reversedProjection());
    }

    /** Classic perspective: the near plane lands on -1, the far plane on +1. */
    private static Matrix4f standardProjection() {
        return new Matrix4f().perspective(FOV, WIDTH / (float) HEIGHT, Z_NEAR, Z_FAR);
    }

    /**
     * Reversed, infinite depth buffer: the near plane lands on 1 and everything farther away
     * tends to 0. This is what Minecraft renders with, so the mod must handle it as well.
     */
    private static Matrix4f reversedProjection() {
        return standardProjection().m22(0.0f).m32(Z_NEAR);
    }

    /** Framebuffer pixel of a point, projected exactly like the game renders it. */
    private static float[] pixelOf(Matrix4fc projection, Vector3f point) {
        Vector4f clip = projection.transform(new Vector4f(point, 1.0f));
        assertTrue(Math.abs(clip.w()) > 1.0E-6f, "point must not sit on the projection plane");
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        return new float[]{(ndcX + 1.0f) * 0.5f * WIDTH, (1.0f - ndcY) * 0.5f * HEIGHT};
    }

    // --- rays ------------------------------------------------------------------------

    @Test
    void aRayThroughTheFrameCentrePointsIntoTheWorld() {
        for (Matrix4f projection : bothConventions()) {
            Vector3f ray = PanelGeometry.viewRay(projection, WIDTH * 0.5, HEIGHT * 0.5, WIDTH, HEIGHT);
            assertNotNull(ray, "a ray must be produced for the centre of the frame");
            assertTrue(ray.z() < 0.0f,
                    "the ray must leave the eye towards the scene, got " + ray);
            assertEquals(1.0f, ray.length(), 1.0E-4f, "the ray must be normalized");
        }
    }

    @Test
    void everyCornerOfTheFrameCastsForward() {
        for (Matrix4f projection : bothConventions()) {
            float[][] corners = {{0, 0}, {WIDTH, 0}, {0, HEIGHT}, {WIDTH, HEIGHT}};
            for (float[] corner : corners) {
                Vector3f ray = PanelGeometry.viewRay(projection, corner[0], corner[1], WIDTH, HEIGHT);
                assertNotNull(ray, "corner " + corner[0] + "," + corner[1]);
                assertTrue(ray.z() < 0.0f,
                        "corner " + corner[0] + "," + corner[1] + " must point forward, got " + ray);
            }
        }
    }

    @Test
    void aFrameWithoutAProjectionProducesNoRay() {
        assertNull(PanelGeometry.viewRay(null, 10, 10, WIDTH, HEIGHT));
        assertNull(PanelGeometry.viewRay(standardProjection(), 10, 10, 0, HEIGHT));
        assertNull(PanelGeometry.viewRay(standardProjection(), 10, 10, WIDTH, 0));
    }

    // --- picking ---------------------------------------------------------------------

    @Test
    void pickingAPanelPointGivesBackTheSamePoint() {
        for (Matrix4f projection : bothConventions()) {
            // Middle of the right edge, then of the top edge, then of the panel itself.
            Vector3f rightEdge = new Vector3f(PANEL.center()).fma(PANEL.halfWidth(), PANEL.right());
            Vector3f topEdge = new Vector3f(PANEL.center()).fma(PANEL.halfHeight(), PANEL.up());
            Vector3f centre = new Vector3f(PANEL.center());

            assertHit(projection, rightEdge, 1.0f, 0.5f);
            assertHit(projection, topEdge, 0.5f, 1.0f);
            assertHit(projection, centre, 0.5f, 0.5f);
        }
    }

    @Test
    void aRayOutsideThePanelMissesIt() {
        for (Matrix4f projection : bothConventions()) {
            Vector3f ray = PanelGeometry.viewRay(projection, 4, 4, WIDTH, HEIGHT);
            assertNotNull(ray);
            assertNull(PanelGeometry.intersect(ray, PANEL),
                    "the top left corner of the frame is well outside the panel");
        }
    }

    @Test
    void aNullRayOrBasisNeverHits() {
        assertNull(PanelGeometry.intersect(null, PANEL));
        assertNull(PanelGeometry.intersect(new Vector3f(0, 0, -1), null));
    }

    // --- helpers ---------------------------------------------------------------------

    private static void assertHit(Matrix4fc projection, Vector3f viewPoint, float expectedU, float expectedV) {
        float[] pixel = pixelOf(projection, viewPoint);
        Vector3f ray = PanelGeometry.viewRay(projection, pixel[0], pixel[1], WIDTH, HEIGHT);
        assertNotNull(ray, "a ray must be produced for " + pixel[0] + "," + pixel[1]);
        Vector3f hit = PanelGeometry.intersect(ray, PANEL);
        assertNotNull(hit, "the projected point must fall inside the panel");
        assertEquals(expectedU, hit.x(), 1.0E-3f, "u of " + viewPoint);
        assertEquals(expectedV, hit.y(), 1.0E-3f, "v of " + viewPoint);
        assertEquals(viewPoint.length(), hit.z(), 1.0E-3f,
                "distance from the eye to the hit of " + viewPoint);
    }
}
