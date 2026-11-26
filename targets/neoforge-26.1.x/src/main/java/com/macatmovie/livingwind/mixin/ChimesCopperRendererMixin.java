package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.ChimesWindMotion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chimes owns native animation; temporarily overlay Living Wind before drawing. */
@Pseudo
@Mixin(targets = "com.cicada.chimes.client.renderer.CopperBERenderer", remap = false)
public abstract class ChimesCopperRendererMixin {
    @Shadow(remap = false) @Final private ModelPart String1BoneUp;
    @Shadow(remap = false) @Final private ModelPart String2BoneUp;
    @Shadow(remap = false) @Final private ModelPart String3BoneUp;
    @Shadow(remap = false) @Final private ModelPart String4BoneUp;
    @Shadow(remap = false) @Final private ModelPart String1BoneLo;
    @Shadow(remap = false) @Final private ModelPart String2BoneLo;
    @Shadow(remap = false) @Final private ModelPart String3BoneLo;
    @Shadow(remap = false) @Final private ModelPart String4BoneLo;
    @Shadow(remap = false) @Final private ModelPart CopperChimeBoneUp;
    @Unique private ModelPart[] livingwind$parts;
    @Unique private float[] livingwind$x;
    @Unique private float[] livingwind$z;
    @Unique private boolean livingwind$applied;
    @Unique private boolean livingwind$handled;

    @Inject(method = "render(Lcom/cicada/chimes/block/entity/WindChimeBE$CopperBE;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), remap = false, require = 0)
    private void livingwind$begin(@Coerce Object be, float partialTick,
                                 com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffers,
                                 int light, int overlay, CallbackInfo ci) {
        livingwind$applied = false;
        livingwind$handled = false;
    }

    // Match every possible ModelPart.render call site. Only the first actually
    // executed call for this frame applies offsets, including alternate branches.
    @Inject(method = "render(Lcom/cicada/chimes/block/entity/WindChimeBE$CopperBE;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            remap = false), remap = false, require = 0)
    private void livingwind$beforeDraw(@Coerce Object be, float partialTick,
                                      com.mojang.blaze3d.vertex.PoseStack pose,
                                      net.minecraft.client.renderer.MultiBufferSource buffers,
                                      int light, int overlay, CallbackInfo ci) {
        if (livingwind$handled) return;
        livingwind$handled = true;
        if (livingwind$parts == null) {
            livingwind$parts = new ModelPart[] {String1BoneUp, String2BoneUp, String3BoneUp, String4BoneUp, String1BoneLo, String2BoneLo, String3BoneLo, String4BoneLo};
            livingwind$x = new float[livingwind$parts.length];
            livingwind$z = new float[livingwind$parts.length];
        }
        livingwind$applied = ChimesWindMotion.apply((BlockEntity) be, livingwind$parts, livingwind$x, livingwind$z, CopperChimeBoneUp.yRot, ChimesWindMotion.Type.COPPER);
    }

    @Inject(method = "render(Lcom/cicada/chimes/block/entity/WindChimeBE$CopperBE;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$afterDraw(@Coerce Object be, float partialTick,
                                     com.mojang.blaze3d.vertex.PoseStack pose,
                                     net.minecraft.client.renderer.MultiBufferSource buffers,
                                     int light, int overlay, CallbackInfo ci) {
        if (livingwind$applied) ChimesWindMotion.restore(livingwind$parts, livingwind$x, livingwind$z);
        livingwind$applied = false;
    }
}
