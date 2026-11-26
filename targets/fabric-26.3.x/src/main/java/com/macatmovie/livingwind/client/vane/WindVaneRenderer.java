package com.macatmovie.livingwind.client.vane;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class WindVaneRenderer implements BlockEntityRenderer<WindVaneBlockEntity, WindVaneRenderer.State> {
    private static final Identifier BIRD_TEXTURE = LivingWind.id("textures/block/bird_wind_vane_arrow.png");
    private static final Identifier[] REDSTONE_INDICATORS = new Identifier[] {
        LivingWind.id("textures/block/redstone_vane_indicator_s.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_sw.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_w.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_nw.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_n.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_ne.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_e.png"),
        LivingWind.id("textures/block/redstone_vane_indicator_se.png")
    };
    private static final float EPS = 0.0005f;

    public WindVaneRenderer(BlockEntityRendererProvider.Context context) {}

    public static final class State extends BlockEntityRenderState {
        float degrees;
        int indicator = -1;
        boolean valid;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(WindVaneBlockEntity vane, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(vane, state, partialTick, cameraPosition, breakProgress);
        state.valid = vane.getLevel() != null;
        if (!state.valid) return;
        state.indicator = vane.getBlockState().getBlock() == ModBlocks.REDSTONE_WIND_VANE.get()
            ? vane.getBlockState().getValue(com.macatmovie.livingwind.block.RedstoneWindVaneBlock.DIRECTION) : -1;
        state.degrees = WindVaneMotion.angle(vane, vane.getLevel().getGameTime() + (double)partialTick);
    }
    @Override public void submit(State state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) return;
        if (state.indicator >= 0) renderRedstoneIndicator(state.indicator, stack, collector, OverlayTexture.NO_OVERLAY);
        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        stack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(-state.degrees)));
        int light = state.lightCoords;
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(BIRD_TEXTURE),
            (pose, buffer) -> renderBird(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        stack.popPose();
    }

    private static void renderRedstoneIndicator(int idx, PoseStack poseStack, SubmitNodeCollector collector, int overlay) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(REDSTONE_INDICATORS[idx]), (pose, c) -> {
        PoseStack.Pose matrix = pose;
        float y = 2.02f / 16f;
        vertex(c, matrix, 0f, y, 1f, 0f, 1f, 0xF000F0, overlay, 0, 1, 0);
        vertex(c, matrix, 1f, y, 1f, 1f, 1f, 0xF000F0, overlay, 0, 1, 0);
        vertex(c, matrix, 1f, y, 0f, 1f, 0f, 0xF000F0, overlay, 0, 1, 0);
        vertex(c, matrix, 0f, y, 0f, 0f, 0f, 0xF000F0, overlay, 0, 1, 0);
        });
    }

    /** The 22x15-model-pixel bird artwork uses a *cropped* part of its 32x16 source PNG.
     * Matches Blockbench UV [0,1,11,16] on the front, and a mirrored back side.
     * Keep the existing local orientation: the wind vane pivots around its center. */
    private static void renderBird(PoseStack.Pose m, VertexConsumer c, int light, int overlay) {
        float z0 = -11f / 16f, z1 = 11f / 16f;
        float y0 = 13f / 16f, y1 = 28f / 16f;
        float u0 = 0f, u1 = 11f / 16f;
        float v0 = 1f / 16f, v1 = 1f;
        // Front side: viewed from +X, counterclockwise vertex order.
        vertex(c, m,  EPS, y0, z0, u0, v1, light, overlay,  1, 0, 0);
        vertex(c, m,  EPS, y1, z0, u0, v0, light, overlay,  1, 0, 0);
        vertex(c, m,  EPS, y1, z1, u1, v0, light, overlay,  1, 0, 0);
        vertex(c, m,  EPS, y0, z1, u1, v1, light, overlay,  1, 0, 0);
        // Back side faces -X. UVs are reversed in vertex order, NOT reversed on the vane:
        // the bird's head stays at the same physical end when viewed from behind.
        vertex(c, m, -EPS, y0, z1, u1, v1, light, overlay, -1, 0, 0);
        vertex(c, m, -EPS, y1, z1, u1, v0, light, overlay, -1, 0, 0);
        vertex(c, m, -EPS, y1, z0, u0, v0, light, overlay, -1, 0, 0);
        vertex(c, m, -EPS, y0, z0, u0, v1, light, overlay, -1, 0, 0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x,float y,float z,float u,float v,
                               int light,int overlay,float nx,float ny,float nz) {
        consumer.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255)
            .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
