package com.macatmovie.livingwind.client.windmill;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindmillBlockEntity;
import com.macatmovie.livingwind.block.WindmillBlock;
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

/** Animated head turns the rotor into the incoming wind (tail downwind), while
 * its 12 pitched wooden boards spin around the axle. The crossed-plane stand
 * is rendered by Minecraft's ordinary static block model instead. */
public final class WindmillRenderer implements BlockEntityRenderer<WindmillBlockEntity, WindmillRenderer.State> {
    private static final Identifier TAIL = LivingWind.id("textures/block/windmill_tail.png");
    private static final Identifier WHEEL = LivingWind.id("textures/block/windmill_wheel.png");
    private static final Identifier[] GLOW_STAGES = new Identifier[] {
        null,
        LivingWind.id("textures/block/windmill_redstone_glow_1.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_2.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_3.png"),
        LivingWind.id("textures/block/windmill_redstone_glow_4.png")
    };
    private static final Identifier WOOD = Identifier.parse("minecraft:textures/block/oak_planks.png");

    public WindmillRenderer(BlockEntityRendererProvider.Context ignored) {}

    public static final class State extends BlockEntityRenderState {
        float yaw, rotor;
        int stage;
        boolean valid;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(WindmillBlockEntity mill, State state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mill, state, partialTick, cameraPosition, breakProgress);
        state.valid = mill.getLevel() != null;
        if (!state.valid) return;
        state.stage = mill.getBlockState().getBlock() == ModBlocks.REDSTONE_WINDMILL.get()
            ? mill.getBlockState().getValue(WindmillBlock.STAGE) : 0;
        WindmillMotion.update(mill, mill.getLevel().getGameTime() + (double)partialTick);
        state.yaw = mill.getYaw(); state.rotor = mill.getRotorAngle();
    }
    @Override public void submit(State state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) return;
        if (state.stage != 0) renderRedstoneGlow(state.stage, stack, collector, OverlayTexture.NO_OVERLAY);
        renderHead(stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.yaw, state.rotor);
    }

    /** Shared geometry for the world renderer and the two custom-rendered windmill items.
     * Includes ALL 12 blades with their authentic two-axis pitched geometry. */
    public static void renderHead(PoseStack stack, SubmitNodeCollector collector,
                                  int light, int overlay, float yaw, float rotorAngle) {
        stack.pushPose();
        stack.translate(.5, 0, .5);
        stack.mulPose(Axis.YP.rotationDegrees(yaw));
        stack.translate(-.5, 0, -.5);
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(TAIL), (pose, buffer) -> draw(pose, buffer, WindmillMesh.HEAD, light, overlay));
        stack.pushPose();
        stack.translate(4f/16f, 19.5f/16f, 8f/16f);
        stack.mulPose(Axis.XP.rotationDegrees(rotorAngle));
        stack.translate(-4f/16f, -19.5f/16f, -8f/16f);
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(TAIL), (pose, buffer) -> draw(pose, buffer, WindmillMesh.ROTOR_HUB, light, overlay));
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(WHEEL), (pose, buffer) -> draw(pose, buffer, WindmillMesh.ROTOR_WHEEL, light, overlay));
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(WOOD), (pose, buffer) -> draw(pose, buffer, WindmillMesh.ROTOR_BLADES, light, overlay));
        stack.popPose();
        stack.popPose();
    }

    /** Only the selected redstone marks are emissive. The stone base remains normally lit. */
    private static void renderRedstoneGlow(int stage, PoseStack stack,
                                          SubmitNodeCollector collector, int overlay) {
        if (stage == 0) return;
        collector.submitCustomGeometry(stack, RenderTypes.entityCutout(GLOW_STAGES[stage]), (entry, consumer) -> {
        PoseStack.Pose pose = entry;
        // Top of smooth-stone plate is Y=2 model pixels. Tiny offset prevents z-fighting.
        float y = 2.02f / 16.0f;
        put(consumer, pose, 0f, y, 1f, 0f, 1f, 0xF000F0, overlay, 0f, 1f, 0f);
        put(consumer, pose, 1f, y, 1f, 1f, 1f, 0xF000F0, overlay, 0f, 1f, 0f);
        put(consumer, pose, 1f, y, 0f, 1f, 0f, 0xF000F0, overlay, 0f, 1f, 0f);
        put(consumer, pose, 0f, y, 0f, 0f, 0f, 0xF000F0, overlay, 0f, 1f, 0f);
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The head rises to model Y=28, above the default one-block BE frustum bounds.
        return true;
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer consumer, float[][] quads, int light, int overlay) {
        // Mesh data is baked once when classes load. No allocations are needed per rendered quad.
        for (float[] quad : quads) {
            for (int i=0; i<32; i+=8) {
                put(consumer, pose, quad[i]/16f, quad[i+1]/16f, quad[i+2]/16f,
                    quad[i+3], quad[i+4], light, overlay, quad[i+5], quad[i+6], quad[i+7]);
            }
        }
    }

    private static void put(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                            float u, float v, int light, int overlay, float nx, float ny, float nz) {
        consumer.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255)
            .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
