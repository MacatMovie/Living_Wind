package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.model.CopperModel", remap=false)
public abstract class Chimes26CopperModelMixin {
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
    @Unique private float[] livingwind$x, livingwind$z;
    @Inject(method="setupAnim(Lcom/cicada/chimes/client/renderer/state/WindChimeRenderState;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$animate(@Coerce Object state, CallbackInfo ci) {
        if (!(state instanceof ChimesWindState access)) return;
        if (livingwind$parts == null) { livingwind$parts = new ModelPart[] {String1BoneUp, String2BoneUp, String3BoneUp, String4BoneUp, String1BoneLo, String2BoneLo, String3BoneLo, String4BoneLo}; livingwind$x=new float[livingwind$parts.length]; livingwind$z=new float[livingwind$parts.length]; }
        ChimesWindMotion.applySample(access.livingwind$getWindSample(), livingwind$parts, livingwind$x, livingwind$z, CopperChimeBoneUp.yRot, ChimesWindMotion.Type.COPPER);
    }
}
