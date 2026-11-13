package com.macatmovie.livingwind.client.windmill;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindmillBlockEntity;
import com.macatmovie.livingwind.block.WindmillBlock;
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
import org.joml.Matrix4f;

/** Animated head turns with the wind travel direction, while its 12 pitched wooden boards spin around the axle.
 * The crossed-plane stand is rendered by Minecraft's ordinary static block model instead. */
public final class WindmillRenderer implements BlockEntityRenderer<WindmillBlockEntity> {
    private static final ResourceLocation TAIL = LivingWind.id("textures/block/windmill_tail.png");
    private static final ResourceLocation WHEEL = LivingWind.id("textures/block/windmill_wheel.png");
    private static final ResourceLocation[] GLOW_STAGES = new ResourceLocation[] {
        null,
        LivingWind.id("textures/block/windmill_redstone_glow_1.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_2.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_3.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_4.png")
    };
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "textures/block/oak_planks.png");

    public WindmillRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override
    public void render(WindmillBlockEntity mill, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light, int overlay) {
        if (mill.getLevel() == null) return;
        // Base plate is fixed; render a single full-bright overlay before rotating the head.
        if (mill.getBlockState().getBlock() == ModBlocks.REDSTONE_WINDMILL.get()) {
            renderRedstoneGlow(mill, stack, buffers, overlay);
        }
        WindmillMotion.update(mill, mill.getLevel().getGameTime() + (double) partialTick);
        renderHead(stack, buffers, light, overlay, mill.getYaw(), mill.getRotorAngle());
    }

    /** Shared geometry for the world renderer and the two custom-rendered windmill items.
     * Includes ALL 12 blades with their authentic two-axis pitched geometry. */
    public static void renderHead(PoseStack stack, MultiBufferSource buffers,
                                  int light, int overlay, float yaw, float rotorAngle) {
        stack.pushPose();
        stack.translate(.5, 0, .5);
        stack.mulPose(Axis.YP.rotationDegrees(yaw));
        stack.translate(-.5, 0, -.5);
        draw(stack, buffers.getBuffer(RenderType.entityCutout(TAIL)), WindmillMesh.HEAD, light, overlay);
        stack.pushPose();
        stack.translate(4f/16f, 19.5f/16f, 8f/16f);
        stack.mulPose(Axis.XP.rotationDegrees(rotorAngle));
        stack.translate(-4f/16f, -19.5f/16f, -8f/16f);
        draw(stack, buffers.getBuffer(RenderType.entityCutout(TAIL)), WindmillMesh.ROTOR_HUB, light, overlay);
        draw(stack, buffers.getBuffer(RenderType.entityCutout(WHEEL)), WindmillMesh.ROTOR_WHEEL, light, overlay);
        draw(stack, buffers.getBuffer(RenderType.entityCutout(WOOD)), WindmillMesh.ROTOR_BLADES, light, overlay);
        stack.popPose();
        stack.popPose();
    }

    /** Only the selected redstone marks are emissive. The stone base remains normally lit. */
    private static void renderRedstoneGlow(WindmillBlockEntity mill, PoseStack stack,
                                          MultiBufferSource buffers, int overlay) {
        int stage = mill.getBlockState().getValue(WindmillBlock.STAGE);
        if (stage == 0) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(GLOW_STAGES[stage]));
        Matrix4f pose = stack.last().pose();
        // Top of smooth-stone plate is Y=2 model pixels. Tiny offset prevents z-fighting.
        float y = 2.02f / 16.0f;
        put(consumer, pose, 0f, y, 1f, 0f, 1f, LightTexture.FULL_BRIGHT, overlay, 0f, 1f, 0f);
        put(consumer, pose, 1f, y, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay, 0f, 1f, 0f);
        put(consumer, pose, 1f, y, 0f, 1f, 0f, LightTexture.FULL_BRIGHT, overlay, 0f, 1f, 0f);
        put(consumer, pose, 0f, y, 0f, 0f, 0f, LightTexture.FULL_BRIGHT, overlay, 0f, 1f, 0f);
    }

    @Override
    public boolean shouldRenderOffScreen(WindmillBlockEntity ignored) {
        // The head rises to model Y=28, above the default one-block BE frustum bounds.
        return true;
    }

    private static void draw(PoseStack stack, VertexConsumer consumer, float[][] quads, int light, int overlay) {
        Matrix4f pose = stack.last().pose();
        // Mesh data is baked once when classes load. No allocations are needed per rendered quad.
        for (float[] quad : quads) {
            for (int i=0; i<32; i+=8) {
                put(consumer, pose, quad[i]/16f, quad[i+1]/16f, quad[i+2]/16f,
                    quad[i+3], quad[i+4], light, overlay, quad[i+5], quad[i+6], quad[i+7]);
            }
        }
    }

    private static void put(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                            float u, float v, int light, int overlay, float nx, float ny, float nz) {
        consumer.vertex(pose, x, y, z).color(255, 255, 255, 255)
            .uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
    }
}
