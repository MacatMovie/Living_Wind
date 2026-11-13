package com.macatmovie.livingwind.client.vane;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;

public final class WindVaneRenderer implements BlockEntityRenderer<WindVaneBlockEntity> {
    private static final ResourceLocation BIRD_TEXTURE = LivingWind.id("textures/block/bird_wind_vane_arrow.png");
    private static final ResourceLocation[] REDSTONE_INDICATORS = new ResourceLocation[] {
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

    @Override
    public void render(WindVaneBlockEntity vane, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        if (vane.getLevel() == null) return;
        Block block = vane.getBlockState().getBlock();
        boolean redstone = block == ModBlocks.REDSTONE_WIND_VANE.get();
        if (redstone) renderRedstoneIndicator(vane, poseStack, buffers, overlay);
        double tick = vane.getLevel().getGameTime() + (double) partialTick;
        float degrees = WindVaneMotion.angle(vane, tick);
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-degrees));
        renderBird(poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    private static void renderRedstoneIndicator(WindVaneBlockEntity vane, PoseStack poseStack, MultiBufferSource buffers, int overlay) {
        int idx = vane.getBlockState().getValue(com.macatmovie.livingwind.block.RedstoneWindVaneBlock.DIRECTION);
        VertexConsumer c = buffers.getBuffer(RenderType.entityCutout(REDSTONE_INDICATORS[idx]));
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        float y = 2.02f / 16f;
        vertex(c, matrix, 0f, y, 1f, 0f, 1f, LightTexture.FULL_BRIGHT, overlay, 0, 1, 0);
        vertex(c, matrix, 1f, y, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay, 0, 1, 0);
        vertex(c, matrix, 1f, y, 0f, 1f, 0f, LightTexture.FULL_BRIGHT, overlay, 0, 1, 0);
        vertex(c, matrix, 0f, y, 0f, 0f, 0f, LightTexture.FULL_BRIGHT, overlay, 0, 1, 0);
        poseStack.popPose();
    }

    /** The 22x15-model-pixel bird artwork uses a *cropped* part of its 32x16 source PNG.
     * Matches Blockbench UV [0,1,11,16] on the front, and a mirrored back side.
     * Keep the existing local orientation: the wind vane pivots around its center. */
    private static void renderBird(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer c = buffers.getBuffer(RenderType.entityCutout(BIRD_TEXTURE));
        Matrix4f m = poseStack.last().pose();
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

    private static void vertex(VertexConsumer consumer, Matrix4f pose,
                               float x,float y,float z,float u,float v,
                               int light,int overlay,float nx,float ny,float nz) {
        consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, 255)
            .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }
}
