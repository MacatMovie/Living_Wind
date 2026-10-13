package com.macatmovie.livingwind.client.windmill;

import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** One shared BEWLR for both windmill items. Unlike JSON previews, this uses exactly
 * the placed windmill's pitched 12-blade quad mesh, without modifying world geometry. */
public final class WindmillItemRenderer extends BlockEntityWithoutLevelRenderer {
    public WindmillItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack item, ItemDisplayContext context, PoseStack stack,
                             MultiBufferSource buffers, int light, int overlay) {
        boolean redstone = item.is(ModItems.REDSTONE_WINDMILL.get());
        stack.pushPose();
        // ItemRenderer places custom block-items at the block origin. Normalize the tall
        // 2.2-block windmill around its visual center for GUI, hand and dropped-item views.
        // In first person the whole model is a little smaller and shifted up, so the base
        // (including the redstone plate) can be seen below the moving head.
        float scale;
        float centerY;
        switch (context) {
            case GUI -> { scale = 0.48f; centerY = 0.56f; }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> { scale = 0.56f; centerY = 0.65f; }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> { scale = 0.53f; centerY = 0.56f; }
            case GROUND -> { scale = 0.40f; centerY = 0.48f; }
            case FIXED -> { scale = 0.55f; centerY = 0.53f; }
            default -> { scale = 0.53f; centerY = 0.54f; }
        }
        stack.translate(0.5f, centerY, 0.5f);
        stack.scale(scale, scale, scale);
        stack.translate(-0.5f, -1.10f, -0.5f);

        // The stand is still Minecraft's original block model with the real redstone plate.
        // The head and the blades use the SAME 2-axis geometry as the in-world block.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            redstone ? ModBlocks.REDSTONE_WINDMILL.get().defaultBlockState()
                     : ModBlocks.WINDMILL.get().defaultBlockState(),
            stack, buffers, light, overlay);
        WindmillRenderer.renderHead(stack, buffers, light, overlay, 25f, 0f);
        stack.popPose();
    }
}
