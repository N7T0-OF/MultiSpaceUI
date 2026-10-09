package org.tastytrash.spatialGUI.mixin.gui;

import org.spongepowered.asm.mixin.Mixin;

//? if neoforge && >1.21.1 {
/*//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}
//? if <26.1.2 {
/^import net.minecraft.client.gui.render.state.GuiRenderState;
^///?} else {
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//?}
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
*///?}

//? if >1.21.1 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.ScreenExtractor;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import java.util.List;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @ModifyArg(
            method = "draw", at = @At(value = "INVOKE",
                    //? if > 26.2 {
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V"
                    //? } else if 26.2 {
                    /*target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V"
                    *///? } else {
                    /*target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"
                    *///?}

            ), index = 1)
    private RenderTarget spatialGUI$redirectRenderTarget(RenderTarget original) {
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();

        if (!SpatialGUIClient.isEnabled()) {
            return original;
        }

        if ((Object) this == renderer.getScreenGuiRenderer()) {
            return renderer.getTargetManager().getTarget();
        }

        return original;
    }

    //? if neoforge {
    /*@Inject(method = "<init>", at = @At("RETURN"))
    private void spatialGUI$capturePip(
            GuiRenderState renderState,
            //? if <26.2 {
            MultiBufferSource.BufferSource bufferSource,
            SubmitNodeCollector submitNodeCollector,
            //?}
            FeatureRenderDispatcher featureRenderDispatcher,
            List pipRendererFactories,
            CallbackInfo ci) {
        if (ScreenExtractor.capturedPipRegistrations == null) {
            ScreenExtractor.capturedPipRegistrations = List.copyOf(pipRendererFactories);
        }
    }
    *///?}
}
//?} else {
/*import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public class GuiRendererMixin {
}
*///?}