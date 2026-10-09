package org.tastytrash.spatialGUI.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2d;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.mixin.gui.GuiRendererAccessor;
import org.tastytrash.spatialGUI.mixin.render.GameRendererAccessor;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;
import org.tastytrash.spatialGUI.util.RenderUtil;
import org.tastytrash.spatialGUI.util.RenderUtil.QuadBasis;
import org.tastytrash.spatialGUI.util.RenderUtil.CylinderBasis;

//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//?} else if >1.21.1 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.gui.render.state.GuiRenderState;
import com.mojang.blaze3d.platform.Window;
*///?} else {
/*import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
*///?}

//? if neoforge && >=26.1.2 {
/*import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import net.minecraft.client.renderer.state.gui.pip.*;
*///?} else if neoforge && >1.21.1 {
/*import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import net.minecraft.client.gui.render.state.pip.*;
*///?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenExtractor {
    //? if >1.21.1 {
    private GuiRenderState screenRenderState;
    private GuiRenderer screenGuiRenderer;
    //? if fabric {
    private static List<PictureInPictureRenderer<?>> vanillaPipRenderers(Minecraft mc) {
        GuiRenderer main = ((GameRendererAccessor) mc.gameRenderer).spatialGUI$getGuiRenderer();
        List<PictureInPictureRenderer<?>> out = new ArrayList<>();
        for (PictureInPictureRenderer<?> r : ((GuiRendererAccessor) main).spatialGUI$getPictureInPictureRenderers().values()) {
            if (r.getClass().getName().startsWith("net.minecraft.")) out.add(r);
        }
        return out;
    }
    //?}
    //? if neoforge {
    /*public static List<PictureInPictureRendererRegistration<?>> capturedPipRegistrations;
    *///?}
    //?}

    public void ensureScreenGuiRenderer() {
        //? if >1.21.1 {

        if (screenGuiRenderer == null) {
            Minecraft mc = Minecraft.getInstance();
            screenRenderState = new GuiRenderState();
            //? if fabric && >=26.2 {
            screenGuiRenderer = new GuiRenderer(
                    screenRenderState,
                    mc.gameRenderer.featureRenderDispatcher(),
                    vanillaPipRenderers(mc)
            );
            //?} else if neoforge && >=26.2 {
            /*screenGuiRenderer = new GuiRenderer(
                    screenRenderState,
                    mc.gameRenderer.featureRenderDispatcher(),
                    capturedPipRegistrations == null ? List.of() : capturedPipRegistrations
            );
            *///?} else if fabric {
            /*screenGuiRenderer = new GuiRenderer(
                    screenRenderState,
                    mc.renderBuffers().bufferSource(),
                    mc.gameRenderer.getSubmitNodeStorage(),
                    mc.gameRenderer.getFeatureRenderDispatcher(),
                    List.of()
            );

            GuiRenderer main = ((GameRendererAccessor) mc.gameRenderer).spatialGUI$getGuiRenderer();

            ((GuiRendererAccessor) screenGuiRenderer)
                    .spatialGUI$setPictureInPictureRenderers(((GuiRendererAccessor) main)
                    .spatialGUI$getPictureInPictureRenderers());

            *///?} else if neoforge {
            /*screenGuiRenderer = new GuiRenderer(
                    screenRenderState,
                    mc.renderBuffers().bufferSource(),
                    mc.gameRenderer.getSubmitNodeStorage(),
                    mc.gameRenderer.getFeatureRenderDispatcher(),
                    capturedPipRegistrations == null ? List.of() : capturedPipRegistrations
            );
            *///?}
        }
        //?}
    }

    //? if >1.21.1 {
    public GuiRenderer getScreenGuiRenderer() {
        ensureScreenGuiRenderer();
        return screenGuiRenderer;
    }
    //?}

    //? if >=26.1.2 {
    public GuiGraphicsExtractor createIsolatedGraphics() {
        ensureScreenGuiRenderer();
        int mouseX = MouseHandlerUtil.getHoverX();
        int mouseY = MouseHandlerUtil.getHoverY();
        return new GuiGraphicsExtractor(Minecraft.getInstance(), screenRenderState, mouseX, mouseY);
    }
    //?} else if >1.21.1 {
    /*public GuiGraphics createIsolatedGraphics() {
        ensureScreenGuiRenderer();
        int mouseX = MouseHandlerUtil.getHoverX();
        int mouseY = MouseHandlerUtil.getHoverY();
        return new GuiGraphics(Minecraft.getInstance(), screenRenderState, mouseX, mouseY);
    }
    *///?}

    public void extractIsolatedScreen(Screen screen, float partialTick, QuadBasis quadBasis, CylinderBasis cylinderBasis, TextureTargetManager targetManager) {
        extractIsolatedScreen(screen, partialTick, quadBasis, cylinderBasis, targetManager, null);
    }

    public void extractIsolatedScreen(Screen screen, float partialTick, QuadBasis quadBasis, CylinderBasis cylinderBasis, TextureTargetManager targetManager, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> renderOperation) {
        ensureScreenGuiRenderer();
        Minecraft mc = Minecraft.getInstance();
        double guiScale = SpatialGUI.config.getEffectiveGuiScale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        if (quadBasis != null) {
            MouseHandlerUtil.mapMousePosition(MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY(),
                    quadBasis, cylinderBasis, guiScale, targetManager.getInventoryTarget());
        }

        int mouseX = MouseHandlerUtil.getHoverX();
        int mouseY = MouseHandlerUtil.getHoverY();

        SpatialGUIRenderer.isExtractingScreen = true;
        //? if >=26.1.2 {
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, screenRenderState, mouseX, mouseY);
        screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        //?} else if >1.21.1 {
        /*GuiGraphics graphics = new GuiGraphics(mc, screenRenderState, mouseX, mouseY);
        screen.renderWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        /*var target = targetManager.getTarget();
        if (target != null) {
            Window window = mc.getWindow();
            float guiWidth = (float) (window.getWidth() / window.getGuiScale());
            float guiHeight = (float) (window.getHeight() / window.getGuiScale());
            Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
            VertexSorting oldSorting = RenderSystem.getVertexSorting();
            var modelView = RenderSystem.getModelViewStack();

            try {
                target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);

                RenderSystem.setProjectionMatrix(
                        new Matrix4f().setOrtho(0.0F, guiWidth, guiHeight, 0.0F, 1000.0F, 21000.0F),
                        VertexSorting.ORTHOGRAPHIC_Z
                );
                modelView.pushMatrix();
                //? if >1.20.1 {
                modelView.translation(0.0F, 0.0F, -11000.0F);
                //?} else {
                /^modelView.setIdentity();
                modelView.translate(0.0F, 0.0F, -11000.0F);
                ^///?}
                RenderSystem.applyModelViewMatrix();

                GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                if (renderOperation != null) {
                    renderOperation.call(screen, graphics, mouseX, mouseY, partialTick);
                } else {
                    screen.renderWithTooltip(graphics, mouseX, mouseY, partialTick);
                }
                graphics.flush();
            } finally {
                modelView.popMatrix();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
                mc.getMainRenderTarget().bindWrite(true);
            }
        }
        *///?}
        SpatialGUIRenderer.isExtractingScreen = false;
    }
}
