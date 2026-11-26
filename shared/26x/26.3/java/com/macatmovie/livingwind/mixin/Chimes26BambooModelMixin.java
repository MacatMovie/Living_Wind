package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.model.BambooModel", remap=false)
public abstract class Chimes26BambooModelMixin {
    @Shadow(remap = false) @Final private ModelPart BamStringLarBoneUp;
    @Shadow(remap = false) @Final private ModelPart BamStringBigBoneUp;
    @Shadow(remap = false) @Final private ModelPart BamStringMedBoneUp;
    @Shadow(remap = false) @Final private ModelPart BamStringSmaBoneUp;
    @Shadow(remap = false) @Final private ModelPart BamStringLarBoneLo;
    @Shadow(remap = false) @Final private ModelPart BamStringBigBoneLo;
    @Shadow(remap = false) @Final private ModelPart BamStringMedBoneLo;
    @Shadow(remap = false) @Final private ModelPart BamStringSmaBoneLo;
    @Shadow(remap = false) @Final private ModelPart CarvStringLarBoneUp;
    @Shadow(remap = false) @Final private ModelPart CarvStringBigBoneUp;
    @Shadow(remap = false) @Final private ModelPart CarvStringMedBoneUp;
    @Shadow(remap = false) @Final private ModelPart CarvStringSmaBoneUp;
    @Shadow(remap = false) @Final private ModelPart CarvStringLarBoneLo;
    @Shadow(remap = false) @Final private ModelPart CarvStringBigBoneLo;
    @Shadow(remap = false) @Final private ModelPart CarvStringMedBoneLo;
    @Shadow(remap = false) @Final private ModelPart CarvStringSmaBoneLo;
    @Shadow(remap = false) @Final private ModelPart BambooUpper;
    @Unique private ModelPart[] livingwind$parts;
    @Unique private float[] livingwind$x, livingwind$z;
    @Inject(method="setupAnim(Lcom/cicada/chimes/client/renderer/state/WindChimeRenderState;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$animate(@Coerce Object state, CallbackInfo ci) {
        if (!(state instanceof ChimesWindState access)) return;
        if (livingwind$parts == null) { livingwind$parts = new ModelPart[] {BamStringLarBoneUp, BamStringBigBoneUp, BamStringMedBoneUp, BamStringSmaBoneUp, BamStringLarBoneLo, BamStringBigBoneLo, BamStringMedBoneLo, BamStringSmaBoneLo, CarvStringLarBoneUp, CarvStringBigBoneUp, CarvStringMedBoneUp, CarvStringSmaBoneUp, CarvStringLarBoneLo, CarvStringBigBoneLo, CarvStringMedBoneLo, CarvStringSmaBoneLo}; livingwind$x=new float[livingwind$parts.length]; livingwind$z=new float[livingwind$parts.length]; }
        ChimesWindMotion.applySample(access.livingwind$getWindSample(), livingwind$parts, livingwind$x, livingwind$z, BambooUpper.yRot, ChimesWindMotion.Type.BAMBOO);
    }
}
