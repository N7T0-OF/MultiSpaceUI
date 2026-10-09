package org.tastytrash.spatialGUI.render;

//? if >=26.1.2 {
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class WorldBlurRenderer {
    private static GlobalSettingsUniform spatialGUI$blurGlobals;
    private static EntityRenderState deferredPlayerRenderState;
    private static boolean applyingBlur = false;

    public static void applyBlur(int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (spatialGUI$blurGlobals == null) {
            spatialGUI$blurGlobals = new GlobalSettingsUniform();
        }

        var originalGlobals = RenderSystem.getGlobalSettingsUniform();
        applyingBlur = true;
        try {
            //? if >=26.3 {
            spatialGUI$blurGlobals.update(0, 0, 0.0, 0L, 0.0F, radius, Vec3.ZERO, false);
             //?} else {
            /*spatialGUI$blurGlobals.update(0, 0, 0.0, 0L, mc.getDeltaTracker(), radius, Vec3.ZERO, false);
            *///?}
            mc.gameRenderer.processBlurEffect();
        } finally {
            RenderSystem.setGlobalSettingsUniform(originalGlobals);
            applyingBlur = false;
        }
    }

    public static boolean isApplyingBlur() {
        return applyingBlur;
    }

    public static EntityRenderState getDeferredPlayerRenderState() {
        return deferredPlayerRenderState;
    }

    public static void setDeferredPlayerRenderState(EntityRenderState state) {
        deferredPlayerRenderState = state;
    }
}
//?} else {
/*public class WorldBlurRenderer {
}
*///?}
