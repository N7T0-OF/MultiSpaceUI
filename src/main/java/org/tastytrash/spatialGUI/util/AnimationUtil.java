package org.tastytrash.spatialGUI.util;

import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIConfig;

public final class AnimationUtil {
    private AnimationUtil() {}

    public static float easeOutCubic(float t) {
        float t1 = t - 1.0f;
        return t1 * t1 * t1 + 1.0f;
    }

    public static float easeOutElastic(float t) {
        float c4 = (float) (2 * Math.PI) / 3.0f;
        return t == 0.0f ? 0.0f : t == 1.0f ? 1.0f : (float) Math.pow(2.0, -10.0 * t) * (float) Math.sin((t * 10.0f - 0.75f) * c4) + 1.0f;
    }

    public static float easeOutBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1.0f / d1) {
            return n1 * t * t;
        } else if (t < 2.0f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float t1 = t - 1.0f;
        return 1.0f + c3 * t1 * t1 * t1 + c1 * t1 * t1;
    }

    public static float easeOutExpo(float t) {
        return t == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    public static float easeOutQuart(float t) {
        float t1 = t - 1.0f;
        return 1.0f - t1 * t1 * t1 * t1;
    }

    public static float easeInOutSine(float t) {
        return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
    }

    public static float applyEasing(SpatialGUIConfig.EasingType easingType, float t) {
        return switch (easingType) {
            case Elastic -> easeOutElastic(t);
            case Bounce -> easeOutBounce(t);
            case Back -> easeOutBack(t);
            case Exponential -> easeOutExpo(t);
            case Quadratic -> easeOutQuad(t);
            case Quartic -> easeOutQuart(t);
            default -> easeOutCubic(t);
        };
    }

    public static float calculateAnimatedScale(float baseScale, long screenOpenTime, boolean isRecipeBookOpen, int recipeBookCloseDelay) {
        int updatedDelay = isRecipeBookOpen ? -2 : Math.min(0, recipeBookCloseDelay + 1);
        boolean shouldShrink = isRecipeBookOpen || updatedDelay < 0;
        float scale = shouldShrink ? baseScale / (float) SpatialGUI.config.recipeBookShrinkFactor : baseScale;

        if (SpatialGUI.config.enableScaleAnimation) {
            long elapsed = System.currentTimeMillis() - screenOpenTime;
            float animationProgress = Math.min(1.0F, (float) elapsed / (float) SpatialGUI.config.openAnimationDurationMs);
            float easedProgress = applyEasing(SpatialGUI.config.animationEasing, animationProgress);
            float startScalePercent = SpatialGUI.config.animationStartScalePercent / 100.0f;
            float startScale = scale * startScalePercent;
            scale = org.tastytrash.spatialGUI.util.MathUtil.lerp(startScale, scale, easedProgress);
        }
        return scale;
    }
}
