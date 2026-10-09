package org.tastytrash.spatialGUI.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.tastytrash.spatialGUI.SpatialGUI;
//? if <1.21.11 {
//?} else if >26.2 {
import com.mojang.renderpearl.api.GpuFormat;
import org.joml.Vector4f;
//?} else if 26.2 {
/*import org.joml.Vector4f;
import com.mojang.blaze3d.GpuFormat;
*///?}

public class TextureTargetManager {
    private TextureTarget inventoryTarget;

    public void clearTarget() {
        if (inventoryTarget == null) {
            return;
        }

        //? if <1.21.11 {
        /*// Transparent black, depth handled by RenderTarget.clear
        inventoryTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        inventoryTarget.clear(Minecraft.ON_OSX);
        *///?} else {
        var colorTexture = inventoryTarget.getColorTexture();
        var depthTexture = inventoryTarget.getDepthTexture();

        if (colorTexture == null) {
            return;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        //? if >=26.2 {
        encoder.clearColorTexture(colorTexture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        if (depthTexture != null) {
            encoder.clearDepthTexture(depthTexture, 1.0);
        }
        //?} else {
        /*if (depthTexture != null) {
            encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);
        } else {
            encoder.clearColorTexture(colorTexture, 0);
        }
        *///?}
        //?}
    }

    public void prepareTarget() {
        Minecraft client = Minecraft.getInstance();
        int windowWidth = client.getWindow().getWidth();
        int windowHeight = client.getWindow().getHeight();

        if (windowWidth <= 0 || windowHeight <= 0) {
            return;
        }

        double scale = Math.max(25, Math.min(SpatialGUI.config.renderScalePercent, 100)) / 100.0;
        int width = Math.max(1, (int) Math.round(windowWidth * scale));
        int height = Math.max(1, (int) Math.round(windowHeight * scale));

        if (inventoryTarget == null) {
            //? if <1.21.11 {
            /*inventoryTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            *///?} else {
            inventoryTarget = new TextureTarget(
                    "Spatial GUI Inventory",
                    width,
                    height,
                    //? if >26.2 {
                    GpuFormat.RGBA8_UNORM,
                    GpuFormat.D16_UNORM
                    //?} else if 26.2 {
                    /*true,
                    GpuFormat.RGBA8_UNORM
                    *///?} else {
                    /*true
                    *///?}
            );
            //?}
            return;
        }

        if (inventoryTarget.width != width || inventoryTarget.height != height) {
            //? if <1.21.11 {
            /*inventoryTarget.resize(width, height, Minecraft.ON_OSX);
            *///?} else {
            inventoryTarget.resize(width, height);
             //?}
        }
    }

    public TextureTarget getTarget() {
        prepareTarget();
        return inventoryTarget;
    }

    public TextureTarget getInventoryTarget() {
        return inventoryTarget;
    }
}