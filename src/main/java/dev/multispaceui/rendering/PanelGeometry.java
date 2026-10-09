package dev.multispaceui.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Geometry of one floating panel: how it is anchored in the world, how that anchor is
 * projected into view space for drawing, and how the cursor is mapped back onto it.
 *
 * <p>Everything the player sees is expressed in <b>view space</b> (camera at the origin,
 * -Z looking forward), which is the space the world projection matrix expects when the
 * model view matrix bound with the draw is the identity. Picking projects the four
 * corners of a panel with the very same projection matrix Minecraft used to render the
 * frame, so a click can never land one pixel away from what is displayed.</p>
 */
public final class PanelGeometry {

    /**
     * How far outside the panel a hit still counts as a hit.
     *
     * <p>A pixel sitting exactly on the edge of a panel comes back as {@code u = 1.0000001}
     * once it travelled through the projection and back; rejecting that would silently drop
     * the clicks made on the border of a window. The tolerance is far below one GUI unit, so
     * a click that really is outside the panel is still refused.</p>
     */
    private static final float EDGE_TOLERANCE = 1.0E-4f;

    /** Orthonormal frame of a panel, in view space; used both for drawing and picking. */
    public record QuadBasis(Vector3f center, Vector3f right, Vector3f up, Vector3f normal,
                            float halfWidth, float halfHeight) {
    }

    private PanelGeometry() {
    }

    // ----------------------------------------------------------------------------------
    // Placement
    // ----------------------------------------------------------------------------------

    /**
     * Extracts the panel frame out of a view space transform: the frame the panel was drawn
     * with, in the space the picking rays live in.
     *
     * @param halfWidth  half width of the panel in world units
     * @param halfHeight half height of the panel in world units
     */
    public static QuadBasis basisOf(Matrix4fc pose, float halfWidth, float halfHeight) {
        Vector3f center = pose.transformPosition(new Vector3f());
        Vector3f right = pose.transformDirection(1.0f, 0.0f, 0.0f, new Vector3f());
        if (right.lengthSquared() > 1.0E-12f) {
            right.normalize();
        }
        Vector3f up = pose.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f());
        if (up.lengthSquared() > 1.0E-12f) {
            up.normalize();
        }
        Vector3f normal = new Vector3f(right).cross(up);
        if (normal.lengthSquared() > 1.0E-12f) {
            normal.normalize();
        }
        return new QuadBasis(center, right, up, normal, halfWidth, halfHeight);
    }

    /** Submits the panel quad: a rectangle of {@code halfWidth} / {@code halfHeight}. */
    public static void emitQuad(VertexConsumer buffer, Matrix4fc pose, float halfWidth, float halfHeight, int argb) {
        vertex(buffer, pose, -halfWidth, -halfHeight, 0.0f, 0.0f, 0.0f, argb);
        vertex(buffer, pose, halfWidth, -halfHeight, 0.0f, 1.0f, 0.0f, argb);
        vertex(buffer, pose, halfWidth, halfHeight, 0.0f, 1.0f, 1.0f, argb);
        vertex(buffer, pose, -halfWidth, halfHeight, 0.0f, 0.0f, 1.0f, argb);
    }

    private static void vertex(VertexConsumer buffer, Matrix4fc pose, float x, float y, float z, float u, float v, int argb) {
        buffer.addVertex(pose, x, y, z)
                .setUv(u, v)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    // ----------------------------------------------------------------------------------
    // Picking
    // ----------------------------------------------------------------------------------

    /**
     * Projects the four corners of a panel into framebuffer pixels.
     *
     * @return the eight coordinates (x0,y0 .. x3,y3) or {@code null} when any corner is
     *         behind the camera, which means the panel cannot be clicked at all
     */
    public static float[] projectCorners(QuadBasis basis, Matrix4fc projection, int width, int height) {
        if (basis == null || projection == null || width <= 0 || height <= 0) {
            return null;
        }
        Vector3f[] local = {
                corner(basis, -1.0f, -1.0f),
                corner(basis, 1.0f, -1.0f),
                corner(basis, 1.0f, 1.0f),
                corner(basis, -1.0f, 1.0f),
        };
        float[] out = new float[8];
        float clipW = 0.0f;
        for (int i = 0; i < 4; i++) {
            float x = local[i].x();
            float y = local[i].y();
            float z = local[i].z();
            float cx = projection.m00() * x + projection.m10() * y + projection.m20() * z + projection.m30();
            float cy = projection.m01() * x + projection.m11() * y + projection.m21() * z + projection.m31();
            float cz = projection.m02() * x + projection.m12() * y + projection.m22() * z + projection.m32();
            float cw = projection.m03() * x + projection.m13() * y + projection.m23() * z + projection.m33();
            if (!(cw > 1.0E-5f) || Float.isNaN(cw)) {
                return null;
            }
            clipW = cw;
            float ndcX = cx / cw;
            float ndcY = cy / cw;
            out[i * 2] = (ndcX * 0.5f + 0.5f) * width;
            out[i * 2 + 1] = (0.5f - ndcY * 0.5f) * height;
        }
        return Float.isFinite(clipW) ? out : null;
    }

    private static Vector3f corner(QuadBasis basis, float sx, float sy) {
        Vector3f v = new Vector3f(basis.center());
        v.fma(sx * basis.halfWidth(), basis.right());
        v.fma(sy * basis.halfHeight(), basis.up());
        return v;
    }

    /** True when {@code px,py} lies inside the convex quad described by eight coordinates. */
    public static boolean pointInQuad(float[] corners, float px, float py) {
        if (corners == null) {
            return false;
        }
        boolean positive = false;
        boolean negative = false;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) & 3;
            float cross = (corners[j * 2] - corners[i * 2]) * (py - corners[i * 2 + 1])
                    - (corners[j * 2 + 1] - corners[i * 2 + 1]) * (px - corners[i * 2]);
            if (cross > 0.0f) {
                positive = true;
            } else if (cross < 0.0f) {
                negative = true;
            }
            if (positive && negative) {
                return false;
            }
        }
        return true;
    }

    /** Depth of the panel centre along the view axis, in blocks; larger is farther. */
    public static float viewDepth(QuadBasis basis) {
        return basis == null ? Float.MAX_VALUE : -basis.center().z();
    }

    /**
     * Builds the view space ray through a pixel of the frame.
     *
     * <p>The projection matrix of the frame is inverted, so the ray stays exact for every
     * effect Minecraft may have applied to it (dynamic field of view included).</p>
     *
     * @return normalized direction leaving the eye, or null when it cannot be built
     */
    public static Vector3f viewRay(Matrix4fc projection, double px, double py, int width, int height) {
        if (projection == null || width <= 0 || height <= 0) {
            return null;
        }
        float ndcX = (float) (px / width * 2.0 - 1.0);
        float ndcY = (float) (1.0 - py / height * 2.0);
        Matrix4f inverse = new Matrix4f(projection);
        float determinant = inverse.determinant();
        if (determinant == 0.0f || Float.isNaN(determinant)) {
            return null;
        }
        inverse.invert();
        Vector4f near = inverse.transform(new Vector4f(ndcX, ndcY, 0.5f, 1.0f));
        Vector4f far = inverse.transform(new Vector4f(ndcX, ndcY, 0.9f, 1.0f));
        if (near.w() == 0.0f || far.w() == 0.0f) {
            return null;
        }
        near.div(near.w());
        far.div(far.w());
        // Minecraft renders with a reversed depth buffer: depending on the frame the 0.9
        // sample may be *closer* to the eye than the 0.5 one. Both samples lie on the same
        // line through the camera, so ordering them by their distance to the eye always
        // yields a direction pointing away from it - a backwards ray would make every panel
        // intersection return t < 0, i.e. no click would ever reach a window.
        Vector3f start = new Vector3f(near.x(), near.y(), near.z());
        Vector3f end = new Vector3f(far.x(), far.y(), far.z());
        if (start.lengthSquared() > end.lengthSquared()) {
            Vector3f swap = start;
            start = end;
            end = swap;
        }
        Vector3f direction = end.sub(start);
        if (direction.lengthSquared() < 1.0E-12f) {
            return null;
        }
        return direction.normalize();
    }

    /**
     * Intersects a camera ray with a panel and returns the hit in normalized panel space.
     *
     * @return {u, v, t} with u/v in [0,1], or null when the ray misses the panel
     */
    public static Vector3f intersect(Vector3f ray, QuadBasis basis) {
        Vector3f hit = intersectUnclamped(ray, basis);
        if (hit == null) {
            return null;
        }
        float u = hit.x();
        float v = hit.y();
        if (u < -EDGE_TOLERANCE || u > 1.0f + EDGE_TOLERANCE
                || v < -EDGE_TOLERANCE || v > 1.0f + EDGE_TOLERANCE) {
            return null;
        }
        return hit;
    }

    /**
     * Same intersection, but the edge test is left out: the returned u / v say where the ray
     * lands on the plane of the panel <b>even when it lands outside of it</b> (u &lt; 0,
     * u &gt; 1, ...).
     *
     * <p>Anything that has to tell "the cursor is over this panel" from "the cursor is
     * somewhere in the world" needs that distinction: a clamped answer always claims the
     * cursor is on the border, and a click made in the world would then be routed to the
     * screen instead of being dropped.</p>
     *
     * @return {u, v, t}, or null when the ray does not hit the plane in front of the camera
     */
    public static Vector3f intersectUnclamped(Vector3f ray, QuadBasis basis) {
        if (ray == null || basis == null) {
            return null;
        }
        Vector3fc normal = basis.normal();
        float denom = ray.x() * normal.x() + ray.y() * normal.y() + ray.z() * normal.z();
        if (Math.abs(denom) < 1.0E-6f) {
            return null;
        }
        Vector3fc center = basis.center();
        float t = (center.x() * normal.x() + center.y() * normal.y() + center.z() * normal.z()) / denom;
        if (!(t > 0.0f) || Float.isNaN(t) || Float.isInfinite(t)) {
            return null;
        }
        if (basis.halfWidth() <= 0.0f || basis.halfHeight() <= 0.0f) {
            return null;
        }
        float hx = ray.x() * t - center.x();
        float hy = ray.y() * t - center.y();
        float hz = ray.z() * t - center.z();
        Vector3fc right = basis.right();
        Vector3fc up = basis.up();
        float localX = hx * right.x() + hy * right.y() + hz * right.z();
        float localY = hx * up.x() + hy * up.y() + hz * up.z();
        float u = (localX / basis.halfWidth() + 1.0f) * 0.5f;
        float v = (localY / basis.halfHeight() + 1.0f) * 0.5f;
        if (Float.isNaN(u) || Float.isNaN(v)) {
            return null;
        }
        return new Vector3f(u, v, t);
    }
}
