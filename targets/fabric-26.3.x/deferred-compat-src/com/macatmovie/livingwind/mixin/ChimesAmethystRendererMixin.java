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
@Mixin(targets = "com.cicada.chimes.client.renderer.AmethystBERenderer", remap = false)
public abstract class ChimesAmethystRendererMixin {
    @Shadow(remap = false) @Final private ModelPart SmallStringA;
    @Shadow(remap = false) @Final private ModelPart MedStringA;
    @Shadow(remap = false) @Final private ModelPart BigStringA;
    @Shadow(remap = false) @Final private ModelPart LarStringA;
    @Shadow(remap = false) @Final private ModelPart SmallStringB;
    @Shadow(remap = false) @Final private ModelPart MedStringB;
    @Shadow(remap = false) @Final private ModelPart BigStringB;
    @Shadow(remap = false) @Final private ModelPart LarStringB;
    @Shadow(remap = false) @Final private ModelPart UpperTopperBone;
    @Unique private ModelPart[] livingwind$parts;
    @Unique private float[] livingwind$x;
    @Unique private float[] livingwind$z;
    @Unique private boolean livingwind$applied;
    @Unique private boolean livingwind$handled;

    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void livingwind$begin(@Coerce Object be, float partialTick,
                                 com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffers,
                                 int light, int overlay, CallbackInfo ci) {
        livingwind$applied = false;
        livingwind$handled = false;
    }

    // Match every possible ModelPart.render call site. Only the first actually
    // executed call for this frame applies offsets, including alternate branches.
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/class_630;method_22698(Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;II)V",
            remap = false), remap = false, require = 0)
    private void livingwind$beforeDraw(@Coerce Object be, float partialTick,
                                      com.mojang.blaze3d.vertex.PoseStack pose,
                                      net.minecraft.client.renderer.MultiBufferSource buffers,
                                      int light, int overlay, CallbackInfo ci) {
        if (livingwind$handled) return;
        livingwind$handled = true;
        if (livingwind$parts == null) {
            livingwind$parts = new ModelPart[] {SmallStringA, MedStringA, BigStringA, LarStringA, SmallStringB, MedStringB, BigStringB, LarStringB};
            livingwind$x = new float[livingwind$parts.length];
            livingwind$z = new float[livingwind$parts.length];
        }
        livingwind$applied = ChimesWindMotion.apply((BlockEntity) be, livingwind$parts, livingwind$x, livingwind$z, UpperTopperBone.yRot, ChimesWindMotion.Type.AMETHYST);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$afterDraw(@Coerce Object be, float partialTick,
                                     com.mojang.blaze3d.vertex.PoseStack pose,
                                     net.minecraft.client.renderer.MultiBufferSource buffers,
                                     int light, int overlay, CallbackInfo ci) {
        if (livingwind$applied) ChimesWindMotion.restore(livingwind$parts, livingwind$x, livingwind$z);
        livingwind$applied = false;
    }
}
