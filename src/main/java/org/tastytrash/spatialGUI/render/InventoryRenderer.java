package org.tastytrash.spatialGUI.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.tastytrash.spatialGUI.util.RenderUtil.QuadBasis;
import org.tastytrash.spatialGUI.util.RenderUtil.CylinderBasis;
//? if >1.21.1 {
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
//? if >26.2 {
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.StagedVertexBuffer;
//?} else if 26.2 {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.StagedVertexBuffer;
*///?} else if >1.21.1 {
/*import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
*///?} else {
/*import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//? if 1.21.1 {
/^import com.mojang.blaze3d.vertex.MeshData;
^///?}
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
*///?}
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.util.AnimationUtil;
import org.tastytrash.spatialGUI.util.RenderUtil;

public class InventoryRenderer {
    private static final PoseStack WORLD_POSE_STACK = new PoseStack();
    //? if >1.21.1 {
    private static final RenderPipeline INVENTORY_PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static final org.joml.Vector3f ZERO_VECTOR = new org.joml.Vector3f();
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
    private static final org.joml.Vector4f COLOR_MODULATOR = new org.joml.Vector4f();
     //?}
    //? if >1.21.1 && <26.2 {
    /*private static final ByteBufferBuilder INVENTORY_BYTE_BUFFER = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    *///?}
    //? if >=26.2 {
    private static final StagedVertexBuffer INVENTORY_BUFFER = new StagedVertexBuffer(
            () -> "Spatial GUI Inventory Buffer",
            RenderType.SMALL_BUFFER_SIZE
    );
    //?}

    private final TextureTargetManager targetManager;
    private QuadBasis quadBasis;
    private CylinderBasis cylinderBasis;
    private long basisFrame = -1;
    private int meshQuadCount = 1;
    private static final int CURVED_SEGMENTS = 32;
    private long screenOpenTime = 0;
    private static boolean isRecipeBookOpen = false;
    private static int recipeBookCloseDelay = 0;

    //? if >1.21.1 {
    private GpuBufferSlice capturedProjectionBuffer;
    private com.mojang.blaze3d.ProjectionType capturedProjectionType;
    //?} else {
    /*private final Matrix4f capturedProjectionMatrix = new Matrix4f();
    private VertexSorting capturedVertexSorting;
    *///?}
    private final PoseStack capturedPoseStack = new PoseStack();
    private boolean hasCapturedPerspective = false;
    private boolean hasDrawnThisFrame = false;

    public InventoryRenderer(TextureTargetManager targetManager) {
        this.targetManager = targetManager;
    }

    private record ScreenPose(float scale, float aspect) {}

    public void updateQuadBasis() {
        if (Minecraft.getInstance().player == null) return;
        boolean isFirstPerson = SpatialGUIClient.getEffectiveFirstPersonMode();
        updateScreenBases(computeScreenPose(null, isFirstPerson), isFirstPerson);
    }

    private ScreenPose computeScreenPose(PoseStack matrices, boolean isFirstPerson) {
        var player = Minecraft.getInstance().player;
        float yaw = player.getYRot();
        float pitch = isFirstPerson ? player.getXRot() : 0;
        float pitchClamp = (float) SpatialGUI.config.firstPersonPitchClamp;
        //? if >=26.2 {
        pitch = Math.clamp(pitch, -pitchClamp, pitchClamp);
        //?} else {
        /*pitch = Math.max(-pitchClamp, Math.min(pitch, pitchClamp));
        *///?}
        float yawRadians = (float) Math.toRadians(yaw);
        float pitchRadians = (float) Math.toRadians(pitch);

        RenderUtil.ScreenTransformConfig config = RenderUtil.getScreenTransformConfig(isFirstPerson);

        double lookX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double lookY = -Math.sin(pitchRadians);
        double lookZ = Math.cos(yawRadians) * Math.cos(pitchRadians);

        float scale = AnimationUtil.calculateAnimatedScale(config.scale(), screenOpenTime, isRecipeBookOpen, recipeBookCloseDelay);

        if (matrices != null) {
            RenderUtil.applyScreenTransform(matrices, isFirstPerson, yawRadians, pitchRadians, config, lookX, lookY, lookZ);
            matrices.scale(scale, scale, scale);
        }

        WORLD_POSE_STACK.setIdentity();
        RenderUtil.applyScreenTransform(WORLD_POSE_STACK, isFirstPerson, yawRadians, pitchRadians, config, lookX, lookY, lookZ);
        WORLD_POSE_STACK.scale(scale, scale, scale);

        var target = targetManager.getInventoryTarget();
        float aspect = target != null && target.height > 0
                ? (float) target.width / (float) target.height
                : 1.0f;
        return new ScreenPose(scale, aspect);
    }

    private void updateScreenBases(ScreenPose pose, boolean isFirstPerson) {
        Matrix4f worldPose = WORLD_POSE_STACK.last().pose();
        quadBasis = RenderUtil.computeQuadBasis(worldPose, pose.aspect(), pose.scale());
        cylinderBasis = isCurvedScreenActive(isFirstPerson)
                ? RenderUtil.computeCylinderBasis(worldPose, pose.aspect(), pose.scale(), (float) Math.toRadians(SpatialGUI.config.curvedScreenArcDegrees))
                : null;
    }

    public QuadBasis getQuadBasis() {
        refreshBasisIfStale();
        return quadBasis;
    }

    public CylinderBasis getCylinderBasis() {
        refreshBasisIfStale();
        return cylinderBasis;
    }

    private void refreshBasisIfStale() {
        long frame = SpatialGUIRenderer.frameCounter();
        if (basisFrame != frame) {
            updateQuadBasis();
            basisFrame = frame;
        }
    }

    private static boolean isCurvedScreenActive(boolean isFirstPerson) {
        return SpatialGUI.config.curvedScreenEnabled && isFirstPerson
                && SpatialGUI.config.curvedScreenArcDegrees >= 5
                && SpatialGUI.config.curvedScreenArcDegrees <= 150;
    }

    private void addScreenQuadMesh(VertexConsumer buffer, Matrix4f pose, float aspect, boolean isFirstPerson) {
        if (isCurvedScreenActive(isFirstPerson)) {
            float arcRadians = (float) Math.toRadians(SpatialGUI.config.curvedScreenArcDegrees);
            RenderUtil.addCurvedScreenQuad(buffer, pose, aspect, arcRadians, CURVED_SEGMENTS);
            meshQuadCount = CURVED_SEGMENTS;
        } else {
            RenderUtil.addScreenQuad(buffer, pose, aspect);
            meshQuadCount = 1;
        }
    }

    //? if >1.21.1 {
    public void capturePerspectiveState(GpuBufferSlice buffer, com.mojang.blaze3d.ProjectionType type, PoseStack poseStack) {
        this.capturedProjectionBuffer = buffer;
        this.capturedProjectionType = type;
        this.capturedPoseStack.setIdentity();
        if (poseStack != null) {
            this.capturedPoseStack.last().pose().set(poseStack.last().pose());
            this.capturedPoseStack.last().normal().set(poseStack.last().normal());
        }
        this.hasCapturedPerspective = true;
    }

    public GpuBufferSlice getCapturedProjectionBuffer() {
        return capturedProjectionBuffer;
    }

    public com.mojang.blaze3d.ProjectionType getCapturedProjectionType() {
        return capturedProjectionType;
    }
    //?} else {
    /*public void capturePerspectiveState(Matrix4f projectionMatrix, VertexSorting vertexSorting, PoseStack poseStack) {
        if (projectionMatrix != null) {
            this.capturedProjectionMatrix.set(projectionMatrix);
        }
        this.capturedVertexSorting = vertexSorting;
        this.capturedPoseStack.setIdentity();
        if (poseStack != null) {
            this.capturedPoseStack.last().pose().set(poseStack.last().pose());
            this.capturedPoseStack.last().normal().set(poseStack.last().normal());
        }
        this.hasCapturedPerspective = true;
    }
    *///?}

    public void resetPerspectiveState() {
        this.hasCapturedPerspective = false;
        this.hasDrawnThisFrame = false;
    }

    public void onFrameStart() {
        this.hasDrawnThisFrame = false;
    }

    public void setScreenOpenTime(long time) {
        this.screenOpenTime = time;
    }

    public void setRecipeBookOpen(boolean open) {
        if (isRecipeBookOpen && !open) {
            recipeBookCloseDelay = -2;
        }
        isRecipeBookOpen = open;
    }

    public void resetRecipeBookState() {
        isRecipeBookOpen = false;
        recipeBookCloseDelay = 0;
    }

    //? if >=26.2 {
    public void renderInWorld(PoseStack matrices) {
        Minecraft client = Minecraft.getInstance();

        if (targetManager.getInventoryTarget() == null || !SpatialGUIClient.renderer().shouldCapture() || client.player == null) {
            return;
        }

        var texture = targetManager.getInventoryTarget().getColorTextureView();
        VertexFormat format = INVENTORY_PIPELINE.getVertexFormatBinding(0);

        if (texture == null || format == null) {
            return;
        }

        PrimitiveTopology primitive = INVENTORY_PIPELINE.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = INVENTORY_BUFFER.appendDraw(
                format,
                primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null
        );

        matrices.pushPose();

        boolean isFirstPerson = SpatialGUIClient.getEffectiveFirstPersonMode();
        ScreenPose screenPose = computeScreenPose(matrices, isFirstPerson);
        recipeBookCloseDelay = isRecipeBookOpen ? -2 : Math.min(0, recipeBookCloseDelay + 1);

        Matrix4f pose = matrices.last().pose();
        VertexConsumer buffer = INVENTORY_BUFFER.getVertexBuilder(draw);

        addScreenQuadMesh(buffer, pose, screenPose.aspect(), isFirstPerson);

        updateScreenBases(screenPose, isFirstPerson);

        matrices.popPose();

        INVENTORY_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = INVENTORY_BUFFER.getExecuteInfo(draw);
        if (info == null) {
            INVENTORY_BUFFER.endFrame();
            return;
        }

        drawInventory(info, texture);
        INVENTORY_BUFFER.endFrame();
    }

    private void drawInventory(StagedVertexBuffer.ExecuteInfo info, GpuTextureView texture) {
        Minecraft client = Minecraft.getInstance();
        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        var output = mainTarget.getColorTextureView();
        if (output == null) {
            return;
        }

        float fadeAlpha = SpatialGUI.config.enableFadeAnimation
                ? Math.min(1.0F, (System.currentTimeMillis() - screenOpenTime) / (float) SpatialGUI.config.fadeDurationMs)
                : 1.0F;
        COLOR_MODULATOR.set(fadeAlpha, fadeAlpha, fadeAlpha, fadeAlpha);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                IDENTITY_MATRIX,
                COLOR_MODULATOR,
                ZERO_VECTOR,
                IDENTITY_MATRIX
        );

        FilterMode filterMode = SpatialGUI.config.filtering.useLinearFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;

        try (var renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Spatial GUI",
                output,
                java.util.Optional.empty(),
                null,
                java.util.OptionalDouble.empty()
        )) {
            //? if >26.2 {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(INVENTORY_PIPELINE));
             //?} else {
            /*renderPass.setPipeline(InventoryRenderer.INVENTORY_PIPELINE);
            *///?}
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            //? if >26.2 {
            renderPass.setUniform
             //?} else {
            /*renderPass.bindTexture
                    *///?}
                            ("Sampler0", texture, RenderSystem.getSamplerCache().getSampler(
                                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                                    filterMode, filterMode, SpatialGUI.config.filtering.useAnisotropicFiltering
                            ));
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public void renderInWorldPost() {
        Minecraft client = Minecraft.getInstance();
        if (!hasCapturedPerspective || hasDrawnThisFrame || targetManager.getInventoryTarget() == null || client.player == null) {
            return;
        }

        var texture = targetManager.getInventoryTarget().getColorTextureView();
        if (texture == null) {
            return;
        }

        hasDrawnThisFrame = true;

        var prevBuffer = RenderSystem.getProjectionMatrixBuffer();
        var prevType = RenderSystem.getProjectionType();

        try {
            if (capturedProjectionBuffer != null && capturedProjectionType != null) {
                RenderSystem.setProjectionMatrix(capturedProjectionBuffer, capturedProjectionType);
            }
            renderInWorld(capturedPoseStack);
        } finally {
            if (prevBuffer != null && prevType != null) {
                RenderSystem.setProjectionMatrix(prevBuffer, prevType);
            }
        }
    }
    //?} else if >1.21.1 {
    /*public void renderInWorld(PoseStack matrices) {
        Minecraft client = Minecraft.getInstance();

        if (targetManager.getInventoryTarget() == null || !SpatialGUIClient.renderer().shouldCapture() || client.player == null) {
            return;
        }

        var texture = targetManager.getInventoryTarget().getColorTextureView();
        if (texture == null) {
            return;
        }

        matrices.pushPose();

        boolean isFirstPerson = SpatialGUIClient.getEffectiveFirstPersonMode();
        ScreenPose screenPose = computeScreenPose(matrices, isFirstPerson);
        recipeBookCloseDelay = isRecipeBookOpen ? -2 : Math.min(0, recipeBookCloseDelay + 1);

        Matrix4f pose = matrices.last().pose();

        BufferBuilder buffer = new BufferBuilder(INVENTORY_BYTE_BUFFER, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        addScreenQuadMesh(buffer, pose, screenPose.aspect(), isFirstPerson);

        MeshData meshData = buffer.build();

        updateScreenBases(screenPose, isFirstPerson);

        matrices.popPose();

        if (meshData != null) {
            try {
                drawInventory(meshData, texture);
            } finally {
                meshData.close();
            }
        }
    }

    private void drawInventory(MeshData meshData, GpuTextureView texture) {
        Minecraft client = Minecraft.getInstance();
        RenderTarget mainTarget = client.getMainRenderTarget();
        var output = mainTarget.getColorTextureView();
        if (output == null) {
            return;
        }

        float fadeAlpha = SpatialGUI.config.enableFadeAnimation
                ? Math.min(1.0F, (System.currentTimeMillis() - screenOpenTime) / (float) SpatialGUI.config.fadeDurationMs)
                : 1.0F;
        COLOR_MODULATOR.set(fadeAlpha, fadeAlpha, fadeAlpha, fadeAlpha);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                IDENTITY_MATRIX,
                COLOR_MODULATOR,
                ZERO_VECTOR,
                IDENTITY_MATRIX
        );

        FilterMode filterMode = SpatialGUI.config.filtering.useLinearFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;

        var sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = sequentialBuffer.getBuffer(meshQuadCount * 6);
        VertexFormat.IndexType indexType = sequentialBuffer.type();

        try (GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Spatial GUI Quad",
                GpuBuffer.USAGE_VERTEX,
                meshData.vertexBuffer()
        )) {
            try (var renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Spatial GUI",
                    output,
                    java.util.OptionalInt.empty(),
                    mainTarget.getDepthTextureView(),
                    java.util.OptionalDouble.empty()
            )) {
                renderPass.setPipeline(InventoryRenderer.INVENTORY_PIPELINE);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.bindTexture("Sampler0", texture, RenderSystem.getSamplerCache().getSampler(
                        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                        filterMode, filterMode, SpatialGUI.config.filtering.useAnisotropicFiltering
                ));
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshQuadCount * 6, 1);
            }
        }
    }

    public void renderInWorldPost() {
        Minecraft client = Minecraft.getInstance();
        if (!hasCapturedPerspective || hasDrawnThisFrame || targetManager.getInventoryTarget() == null || client.player == null) {
            return;
        }

        var texture = targetManager.getInventoryTarget().getColorTextureView();
        if (texture == null) {
            return;
        }

        hasDrawnThisFrame = true;

        var prevBuffer = RenderSystem.getProjectionMatrixBuffer();
        var prevType = RenderSystem.getProjectionType();

        try {
            if (capturedProjectionBuffer != null && capturedProjectionType != null) {
                RenderSystem.setProjectionMatrix(capturedProjectionBuffer, capturedProjectionType);
            }
            renderInWorld(capturedPoseStack);
        } finally {
            if (prevBuffer != null && prevType != null) {
                RenderSystem.setProjectionMatrix(prevBuffer, prevType);
            }
        }
    }
    *///?} else {
    /*public void renderInWorld(PoseStack matrices) {
        Minecraft client = Minecraft.getInstance();
        RenderTarget target = targetManager.getInventoryTarget();

        if (target == null || !SpatialGUIClient.renderer().shouldCapture() || client.player == null) {
            return;
        }

        int textureId = target.getColorTextureId();
        if (textureId <= 0) {
            return;
        }

        matrices.pushPose();

        boolean isFirstPerson = SpatialGUIClient.getEffectiveFirstPersonMode();
        ScreenPose screenPose = computeScreenPose(matrices, isFirstPerson);
        recipeBookCloseDelay = isRecipeBookOpen ? -2 : Math.min(0, recipeBookCloseDelay + 1);

        Matrix4f pose = matrices.last().pose();

        //? if 1.21.1 {
        /^BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        ^///?} else {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        //?}

        addScreenQuadMesh(buffer, pose, screenPose.aspect(), isFirstPerson);

        updateScreenBases(screenPose, isFirstPerson);

        matrices.popPose();

        //? if 1.21.1 {
        /^drawInventory(buffer.buildOrThrow(), textureId);
        ^///?} else {
        drawInventory(buffer.end(), textureId);
        //?}
    }

    //? if 1.21.1 {
    /^private void drawInventory(MeshData meshData, int textureId) {
    ^///?} else {
    private void drawInventory(BufferBuilder.RenderedBuffer renderedBuffer, int textureId) {
    //?}
        Minecraft client = Minecraft.getInstance();

        float fadeAlpha = SpatialGUI.config.enableFadeAnimation
                ? Math.min(1.0F, (System.currentTimeMillis() - screenOpenTime) / (float) SpatialGUI.config.fadeDurationMs)
                : 1.0F;

        int filter = SpatialGUI.config.filtering.useLinearFiltering ? GL11.GL_LINEAR : GL11.GL_NEAREST;

        client.getMainRenderTarget().bindWrite(true);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.bindTexture(textureId);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);

        RenderSystem.setShaderColor(fadeAlpha, fadeAlpha, fadeAlpha, fadeAlpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        try {
            //? if 1.21.1 {
            /^BufferUploader.drawWithShader(meshData);
            ^///?} else {
            BufferUploader.drawWithShader(renderedBuffer);
            //?}
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void renderInWorldPost() {
        Minecraft client = Minecraft.getInstance();
        if (!hasCapturedPerspective || hasDrawnThisFrame || targetManager.getInventoryTarget() == null || client.player == null) {
            return;
        }

        RenderTarget target = targetManager.getInventoryTarget();
        int textureId = target.getColorTextureId();
        if (textureId <= 0) {
            return;
        }

        hasDrawnThisFrame = true;

        Matrix4f prevProjection = RenderSystem.getProjectionMatrix();
        VertexSorting prevSorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();

        try {
            RenderSystem.setProjectionMatrix(capturedProjectionMatrix, capturedVertexSorting);
            modelView.pushMatrix();
            //? if >1.20.1 {
            modelView.identity();
            //?} else {
            /^modelView.setIdentity();
            ^///?}
            RenderSystem.applyModelViewMatrix();

            renderInWorld(capturedPoseStack);
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(prevProjection, prevSorting);
        }
    }
    *///?}
}