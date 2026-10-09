package org.tastytrash.spatialGUI.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

public class CameraUtil {
    public static void checkBlockCollision(Entity entity) {
        if (Minecraft.getInstance().level == null || !SpatialGUI.config.fallbackToFirstPersonOnBlockCollision) {
            SpatialGUIClient.setSwitchedToFirstPersonDueToBlock(false);
            return;
        }

        float yawRadians = (float) Math.toRadians(entity.getYRot());
        double sideOffset = SpatialGUI.config.cameraSideOffset;
        if (SpatialGUI.config.mirrorThirdPerson) {
            sideOffset = -sideOffset;
        }
        double camX = entity.getX() + Math.sin(yawRadians) * SpatialGUI.config.cameraDistance + Math.cos(yawRadians) * sideOffset;
        double camY = entity.getY() + SpatialGUI.config.cameraHeightOffset;
        double camZ = entity.getZ() - Math.cos(yawRadians) * SpatialGUI.config.cameraDistance + Math.sin(yawRadians) * sideOffset;

        Vec3 playerEyePos = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
        Vec3 targetCamPos = new Vec3(camX, camY, camZ);
        BlockPos blockPos = BlockPos.containing(camX, camY, camZ);

        var blockState = Minecraft.getInstance().level.getBlockState(blockPos);
        var collisionShape = blockState.getCollisionShape(Minecraft.getInstance().level, blockPos);
        boolean isInsideBlock = !blockState.isAir() && !collisionShape.isEmpty() && collisionShape.bounds().move(blockPos).inflate(0.001).contains(targetCamPos);

        var clipContext = new ClipContext(playerEyePos, targetCamPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        boolean pathBlocked = Minecraft.getInstance().level.clip(clipContext).getType() != HitResult.Type.MISS;

        SpatialGUIClient.setSwitchedToFirstPersonDueToBlock(isInsideBlock || pathBlocked);
    }

    public static Vec3 adjustCameraPositionForCollision(Entity entity, Vec3 playerEyePos, Vec3 targetCamPos) {
        if (Minecraft.getInstance().level == null) return targetCamPos;

        var clipContext = new ClipContext(playerEyePos, targetCamPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        HitResult hit = Minecraft.getInstance().level.clip(clipContext);
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hitVec = hit.getLocation();
            Vec3 dir = playerEyePos.subtract(targetCamPos);
            double len = dir.length();
            if (len > 0.0001) {
                dir = dir.normalize();
                return hitVec.add(dir.scale(0.1));
            }
        }
        return targetCamPos;
    }

    public static float getCurrentFov() {
        //? if >26.2 {
        return Minecraft.getInstance().gameRenderer.mainCamera().getFov();
        //?} else {
        /*return SpatialGUI.config.overrideFov
                ? (float) SpatialGUI.config.targetFov
                : (float) (int) Minecraft.getInstance().options.fov().get();
        *///?}
    }

    public static float calculateFovMultiplier() {
        if (!SpatialGUI.config.autoScaleByFov) return 1.0f;

        float currentFov = getCurrentFov();
        float baselineFov = (float) SpatialGUI.config.autoFovTuning.autoScaleBaselineFov;

        return currentFov / baselineFov;
    }

    public static float calculateAutoFovDistance(float baseDistance, boolean isFirstPerson) {
        if (isFirstPerson) return 0.0f;
        
        float fovMultiplier = calculateFovMultiplier();
        float distanceMultiplier = (float) SpatialGUI.config.autoFovTuning.autoScaleDistanceMultiplier;
        float fovAdjustment = (1.0f / fovMultiplier) - 1.0f;
        float distanceFovAdjustment = fovAdjustment * distanceMultiplier;
        
        return Math.max(-4, Math.min(baseDistance * (1.0f + distanceFovAdjustment), 4));
    }

    public static float calculateAutoFovSideOffset(float baseSideOffset, boolean isFirstPerson) {
        if (isFirstPerson) return 0.0f;
        
        float fovMultiplier = calculateFovMultiplier();
        float sideOffsetMultiplier = (float) SpatialGUI.config.autoFovTuning.autoScaleSideOffsetMultiplier;
        float fovAdjustment = (1.0f / fovMultiplier) - 1.0f;
        float sideOffsetFovAdjustment = fovAdjustment * sideOffsetMultiplier;
        
        return Math.max(-4, Math.min(baseSideOffset * (1.0f + sideOffsetFovAdjustment), 4));
    }

    public static double calculateMouseSensitivity() {
        double ss = Minecraft.getInstance().options.sensitivity().get() * 0.6 + 0.2;
        return ss * ss * ss;
    }
}
