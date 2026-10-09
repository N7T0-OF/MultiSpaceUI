package org.tastytrash.spatialGUI.util;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.compat.VulkanModCompat;
import org.tastytrash.spatialGUI.mixin.render.GameRendererInvoker;

public final class RenderUtil {
    private RenderUtil() {}

    public static void addQuadVertex(VertexConsumer buffer, Matrix4f pose,
            float x, float y, float z, float u, float v) {
        //? if >1.20.1 {
        buffer.addVertex(pose, x, y, z).setUv(u, v).setColor(255, 255, 255, SpatialGUI.config.screenAlpha);
        //?} else {
        /*buffer.vertex(pose, x, y, z).uv(u, v).color(255, 255, 255, SpatialGUI.config.screenAlpha).endVertex();
        *///?}
    }

    public static void applyScreenTransform(PoseStack matrices, boolean isFirstPerson,
            float yawRadians, float pitchRadians, ScreenTransformConfig config,
            double lookX, double lookY, double lookZ) {
        float yawOffsetRad = (float) Math.toRadians(config.yawOffset);
        float pitchOffsetRad = (float) Math.toRadians(config.pitchOffset);
        
        if (isFirstPerson) {
            matrices.translate(
                    lookX * config.distance + Math.cos(yawRadians) * config.sideOffset,
                    lookY * config.distance + config.heightOffset,
                    lookZ * config.distance + Math.sin(yawRadians) * config.sideOffset
            );
            matrices.mulPose(new Quaternionf()
                    .rotateY(-yawRadians + yawOffsetRad)
                    .rotateX(-pitchRadians + pitchOffsetRad)
                    //? if >1.20.1 {
                    .get(new Matrix4f())
                    //?}
            );
        } else {
            matrices.translate(
                    -Mth.sin(yawRadians) * config.distance + Math.cos(yawRadians) * config.sideOffset,
                    config.heightOffset,
                    Mth.cos(yawRadians) * config.distance + Math.sin(yawRadians) * config.sideOffset
            );
            matrices.mulPose(new Quaternionf()
                    .rotateY(-yawRadians + yawOffsetRad)
                    .rotateX(pitchOffsetRad)
                    //? if >1.20.1 {
                    .get(new Matrix4f())
                    //?}
            );
        }
    }

    public record ScreenTransformConfig(float distance, float sideOffset, float heightOffset, float yawOffset, float pitchOffset, float scale) { }

    private static float calculateFovScaleMultiplier(boolean autoScaleByFov) {
        if (!autoScaleByFov) return 1.0f;

        float currentFov;
        //? if >=26.1.2 {
        currentFov = Minecraft.getInstance().gameRenderer.mainCamera().getFov();
        //?} else {
        /*if (SpatialGUI.config.overrideFov) {
            currentFov = (float) SpatialGUI.config.targetFov;
        } else {
            currentFov = (float) ((GameRendererInvoker) Minecraft.getInstance().gameRenderer)
                    .spatialGUI$getFov(Minecraft.getInstance().gameRenderer.mainCamera(),
                            //? if >1.20.1 {
                            Minecraft.getInstance().gameRenderer.mainCamera().getPartialTickTime(), true);
                            //?} else {
                            /^Minecraft.getInstance().getFrameTime(), true);
                            ^///?}
        }
        *///?}
        float baselineFov = (float) SpatialGUI.config.autoFovTuning.autoScaleBaselineFov;
        float power = (float) SpatialGUI.config.autoFovTuning.autoScaleScreenPower;

        float ratio = currentFov / baselineFov;
        return (float) Math.pow(ratio, power);
    }

    public static ScreenTransformConfig getScreenTransformConfig(boolean isFirstPerson) {
        float fovMultiplier = calculateFovScaleMultiplier(SpatialGUI.config.autoScaleByFov);

        if (isFirstPerson) {
            return new ScreenTransformConfig(
                    (float) SpatialGUI.config.firstPersonScreenDistance,
                    (float) SpatialGUI.config.firstPersonScreenSideOffset,
                    (float) SpatialGUI.config.firstPersonScreenHeightOffset,
                    (float) SpatialGUI.config.firstPersonScreenYawOffset,
                    (float) SpatialGUI.config.firstPersonScreenPitchOffset,
                    (float) SpatialGUI.config.firstPersonScreenScale * fovMultiplier
            );
        }

        float thirdPersonFovMultiplier = 1.0f + (fovMultiplier - 1.0f) * (float) SpatialGUI.config.autoFovTuning.autoScaleThirdPersonScreenMultiplier;
        float sideOffset = (float) SpatialGUI.config.screenSideOffset;
        float yawOffset = (float) SpatialGUI.config.screenYawOffset;

        if (SpatialGUI.config.mirrorThirdPerson) {
            sideOffset = -sideOffset;
            yawOffset = -yawOffset;
        }

        return new ScreenTransformConfig(
                (float) SpatialGUI.config.screenDistance,
                sideOffset,
                (float) SpatialGUI.config.screenHeightOffset,
                yawOffset,
                (float) SpatialGUI.config.screenPitchOffset,
                (float) SpatialGUI.config.screenScale * thirdPersonFovMultiplier
        );
    }

    public static void addScreenQuad(VertexConsumer buffer, Matrix4f pose, float aspect) {
        float halfWidth = aspect * 0.5F;
        float halfHeight = 0.5F;
        if (VulkanModCompat.isVulkanModLoaded()) {
            addQuadVertex(buffer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F);
            addQuadVertex(buffer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F);
            addQuadVertex(buffer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F);
            addQuadVertex(buffer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F);
        } else {
            addQuadVertex(buffer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 0.0F);
            addQuadVertex(buffer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 0.0F);
            addQuadVertex(buffer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 1.0F);
            addQuadVertex(buffer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 1.0F);
        }
    }

    public record QuadBasis(Vector3f centerOffset, Vector3f right, Vector3f up, Vector3f normal, float halfWidth, float halfHeight) {}

    public static QuadBasis computeQuadBasis(Matrix4f worldPose, float aspect, float scale) {
        Vector3f centerOffset = worldPose.transformPosition(new Vector3f());
        Vector3f right = worldPose.transformDirection(1f, 0f, 0f, new Vector3f()).normalize();
        Vector3f up = worldPose.transformDirection(0f, 1f, 0f, new Vector3f()).normalize();
        Vector3f normal = new Vector3f(right).cross(up).normalize();
        float halfWidth = aspect * 0.5f * scale;
        float halfHeight = 0.5f * scale;
        return new QuadBasis(centerOffset, right, up, normal, halfWidth, halfHeight);
    }

    private static float[] buildCameraRayDirection(double screenX, double screenY) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();

        double ndcX = (screenX / (double) screenWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - (screenY / (double) screenHeight) * 2.0;

        //? if >=26.2 {
        var camera = mc.gameRenderer.mainCamera();
        float yawRadians = (float) Math.toRadians(camera.yRot());
        float pitchRadians = (float) Math.toRadians(camera.xRot());
        //?} else {
        /*var camera = mc.gameRenderer.mainCamera();
        float yawRadians = (float) Math.toRadians(camera.yRot());
        float pitchRadians = (float) Math.toRadians(camera.xRot());
        *///?}

        float fx = (float) (-Math.sin(yawRadians) * Math.cos(pitchRadians));
        float fy = (float) (-Math.sin(pitchRadians));
        float fz = (float) (Math.cos(yawRadians) * Math.cos(pitchRadians));
        float fInv = 1.0f / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= fInv;
        fy *= fInv;
        fz *= fInv;

        float rx = -fz;
        float ry = 0f;
        float rz = fx;
        float rInv = 1.0f / (float) Math.sqrt(rx * rx + rz * rz);
        rx *= rInv;
        rz *= rInv;

        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;
        float uInv = 1.0f / (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux *= uInv;
        uy *= uInv;
        uz *= uInv;

        //? if >=26.1.2 {
        float fovDegrees = camera.getFov();
         //?} else {
        /*float fovDegrees = (float) ((GameRendererInvoker) mc.gameRenderer)
                .spatialGUI$getFov(camera,
                        //? if >1.20.1 {
                        camera.getPartialTickTime(), true);
                        //?} else {
                        /^Minecraft.getInstance().getFrameTime(), true);
                        ^///?}
        *///?}
        float aspect = (float) screenWidth / (float) screenHeight;
        float tanHalfFovY = (float) Math.tan(Math.toRadians(fovDegrees / 2.0));
        float tanHalfFovX = tanHalfFovY * aspect;

        float xCoeff = (float) ndcX * tanHalfFovX;
        float yCoeff = (float) ndcY * tanHalfFovY;

        float dx = fx + rx * xCoeff + ux * yCoeff;
        float dy = fy + ry * xCoeff + uy * yCoeff;
        float dz = fz + rz * xCoeff + uz * yCoeff;
        float dInv = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= dInv;
        dy *= dInv;
        dz *= dInv;
        return new float[]{dx, dy, dz};
    }

    public static Vector2d getInventoryMousePositionRay(double screenX, double screenY, QuadBasis basis, TextureTarget inventoryTarget) {
        Minecraft mc = Minecraft.getInstance();
        float[] ray = buildCameraRayDirection(screenX, screenY);
        if (ray == null) {
            return null;
        }
        float dx = ray[0];
        float dy = ray[1];
        float dz = ray[2];


        Vector3f norm = basis.normal();
        float denom = dx * norm.x() + dy * norm.y() + dz * norm.z();
        if (Math.abs(denom) < 1e-6f) {
            return null;
        }

        Vector3f center = basis.centerOffset();
        float t = (center.x() * norm.x() + center.y() * norm.y() + center.z() * norm.z()) / denom;
        if (t <= 0f || Float.isNaN(t) || Float.isInfinite(t)) {
            return null;
        }

        if (basis.halfWidth() <= 0f || basis.halfHeight() <= 0f) {
            return null;
        }

        float hx = dx * t - center.x();
        float hy = dy * t - center.y();
        float hz = dz * t - center.z();

        Vector3f bRight = basis.right();
        Vector3f bUp = basis.up();
        float localX = hx * bRight.x() + hy * bRight.y() + hz * bRight.z();
        float localY = hx * bUp.x() + hy * bUp.y() + hz * bUp.z();

        float u = (localX / basis.halfWidth() + 1f) / 2f;
        float v = (localY / basis.halfHeight() + 1f) / 2f;

        if (Float.isNaN(u) || Float.isNaN(v)) {
            return null;
        }

        float clampedU = Math.max(0.0f, Math.min(1.0f, u));
        float clampedV = Math.max(0.0f, Math.min(1.0f, v));

        int targetWidth = mc.getWindow().getWidth();
        int targetHeight = mc.getWindow().getHeight();

        return new Vector2d(clampedU * targetWidth, (1.0 - clampedV) * targetHeight);
    }

    public record CylinderBasis(Vector3f axisPos, Vector3f axisDir, Vector3f refDir, Vector3f center, float radius, float arcRadians, float halfHeight) {}

    public static CylinderBasis computeCylinderBasis(Matrix4f worldPose, float aspect, float scale, float arcRadians) {
        float radiusLocal = aspect / arcRadians;
        Vector3f axisPos = worldPose.transformPosition(0.0f, 0.0f, radiusLocal, new Vector3f());
        Vector3f center = worldPose.transformPosition(new Vector3f());
        Vector3f axisDir = worldPose.transformDirection(0.0f, 1.0f, 0.0f, new Vector3f()).normalize();
        Vector3f refDir = worldPose.transformDirection(0.0f, 0.0f, -1.0f, new Vector3f()).normalize();
        return new CylinderBasis(axisPos, axisDir, refDir, center, radiusLocal * scale, arcRadians, 0.5f * scale);
    }

    public static void addCurvedScreenQuad(VertexConsumer buffer, Matrix4f pose, float aspect, float arcRadians, int segments) {
        float radius = aspect / arcRadians;
        float halfArc = arcRadians * 0.5F;
        float prevX = radius * (float) Math.sin(-halfArc);
        float prevZ = radius * (1.0F - (float) Math.cos(-halfArc));
        float prevU = 0.0F;
        for (int i = 1; i <= segments; i++) {
            float angle = -halfArc + arcRadians * (i / (float) segments);
            float x = radius * (float) Math.sin(angle);
            float z = radius * (1.0F - (float) Math.cos(angle));
            float u = i / (float) segments;

            if (VulkanModCompat.isVulkanModLoaded()) {
                addQuadVertex(buffer, pose, prevX, -0.5F, prevZ, prevU, 1.0F);
                addQuadVertex(buffer, pose, x, -0.5F, z, u, 1.0F);
                addQuadVertex(buffer, pose, x, 0.5F, z, u, 0.0F);
                addQuadVertex(buffer, pose, prevX, 0.5F, prevZ, prevU, 0.0F);
            } else {
                addQuadVertex(buffer, pose, prevX, -0.5F, prevZ, prevU, 0.0F);
                addQuadVertex(buffer, pose, x, -0.5F, z, u, 0.0F);
                addQuadVertex(buffer, pose, x, 0.5F, z, u, 1.0F);
                addQuadVertex(buffer, pose, prevX, 0.5F, prevZ, prevU, 1.0F);
            }

            prevX = x;
            prevZ = z;
            prevU = u;
        }
    }

    public static Vector2d getInventoryMousePositionRayCurved(double screenX, double screenY, CylinderBasis basis) {
        Minecraft mc = Minecraft.getInstance();
        float[] ray = buildCameraRayDirection(screenX, screenY);
        if (ray == null) {
            return null;
        }
        float dx = ray[0];
        float dy = ray[1];
        float dz = ray[2];

        Vector3f axis = basis.axisPos();
        Vector3f up = basis.axisDir();
        float radius = basis.radius();

        float ox = -axis.x();
        float oy = -axis.y();
        float oz = -axis.z();

        float dParallel = dx * up.x() + dy * up.y() + dz * up.z();
        float oParallel = ox * up.x() + oy * up.y() + oz * up.z();

        float dpx = dx - up.x() * dParallel;
        float dpy = dy - up.y() * dParallel;
        float dpz = dz - up.z() * dParallel;
        float opx = ox - up.x() * oParallel;
        float opy = oy - up.y() * oParallel;
        float opz = oz - up.z() * oParallel;

        float a = dpx * dpx + dpy * dpy + dpz * dpz;
        if (a < 1.0e-10f) {
            return null;
        }
        float b = 2.0f * (opx * dpx + opy * dpy + opz * dpz);
        float c = opx * opx + opy * opy + opz * opz - radius * radius;
        float discriminant = b * b - 4.0f * a * c;
        if (discriminant < 0.0f) {
            return null;
        }
        float sqrtDisc = (float) Math.sqrt(discriminant);
        float t = (-b - sqrtDisc) / (2.0f * a);
        if (t <= 0.0f) {
            t = (-b + sqrtDisc) / (2.0f * a);
        }
        if (t <= 0.0f || Float.isNaN(t) || Float.isInfinite(t)) {
            return null;
        }

        float hx = dx * t;
        float hy = dy * t;
        float hz = dz * t;

        float rx = hx - axis.x();
        float ry = hy - axis.y();
        float rz = hz - axis.z();
        float rParallel = rx * up.x() + ry * up.y() + rz * up.z();
        rx -= up.x() * rParallel;
        ry -= up.y() * rParallel;
        rz -= up.z() * rParallel;
        float rLength = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLength < 1.0e-6f) {
            return null;
        }
        rx /= rLength;
        ry /= rLength;
        rz /= rLength;

        Vector3f ref = basis.refDir();
        float cosPhi = ref.x() * rx + ref.y() * ry + ref.z() * rz;
        float sinPhi = (up.y() * rz - up.z() * ry) * ref.x()
                + (up.z() * rx - up.x() * rz) * ref.y()
                + (up.x() * ry - up.y() * rx) * ref.z();
        float phi = (float) Math.atan2(sinPhi, cosPhi);

        float arc = basis.arcRadians();
        float u = 0.5f + phi / arc;

        Vector3f center = basis.center();
        float vertical = (hx - center.x()) * up.x() + (hy - center.y()) * up.y() + (hz - center.z()) * up.z();
        float v = 0.5f + vertical / (2.0f * basis.halfHeight());

        if (Float.isNaN(u) || Float.isNaN(v)) {
            return null;
        }

        float clampedU = Math.max(0.0f, Math.min(1.0f, u));
        float clampedV = Math.max(0.0f, Math.min(1.0f, v));

        int targetWidth = mc.getWindow().getWidth();
        int targetHeight = mc.getWindow().getHeight();

        return new Vector2d(clampedU * targetWidth, (1.0 - clampedV) * targetHeight);
    }
}
