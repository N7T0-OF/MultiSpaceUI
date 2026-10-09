package org.tastytrash.spatialGUI.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MathUtil {
    private MathUtil() {}

    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    public static float rotLerp(float start, float end, float t) {
        float diff = ((end - start) % 360f + 540f) % 360f - 180f;
        return start + diff * t;
    }

    public static double lerp(double start, double end, float t) {
        return start + (end - start) * t;
    }

    public static Vec3 lerpEntityPosition(Entity entity, float partialTicks) {
        double x = lerp(entity.xOld, entity.getX(), partialTicks);
        double y = lerp(entity.yOld, entity.getY(), partialTicks);
        double z = lerp(entity.zOld, entity.getZ(), partialTicks);
        return new Vec3(x, y, z);
    }
}
