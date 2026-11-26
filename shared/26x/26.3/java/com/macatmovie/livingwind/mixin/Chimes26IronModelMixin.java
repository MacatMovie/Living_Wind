package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.model.IronModel", remap=false)
public abstract class Chimes26IronModelMixin {
    @Shadow(remap = false) @Final private ModelPart BigStrA;
    @Shadow(remap = false) @Final private ModelPart SmaStrA;
    @Shadow(remap = false) @Final private ModelPart MedStrA;
    @Shadow(remap = false) @Final private ModelPart LarStrA;
    @Shadow(remap = false) @Final private ModelPart BigStrB;
    @Shadow(remap = false) @Final private ModelPart SmaStrB;
    @Shadow(remap = false) @Final private ModelPart MedStrB;
    @Shadow(remap = false) @Final private ModelPart LarStrB;
    @Shadow(remap = false) @Final private ModelPart TopperBoneA;
    @Unique private ModelPart[] livingwind$parts;
    @Unique private float[] livingwind$x, livingwind$z;
    @Inject(method="setupAnim(Lcom/cicada/chimes/client/renderer/state/WindChimeRenderState;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$animate(@Coerce Object state, CallbackInfo ci) {
        if (!(state instanceof ChimesWindState access)) return;
        if (livingwind$parts == null) { livingwind$parts = new ModelPart[] {BigStrA, SmaStrA, MedStrA, LarStrA, BigStrB, SmaStrB, MedStrB, LarStrB}; livingwind$x=new float[livingwind$parts.length]; livingwind$z=new float[livingwind$parts.length]; }
        ChimesWindMotion.applySample(access.livingwind$getWindSample(), livingwind$parts, livingwind$x, livingwind$z, TopperBoneA.yRot, ChimesWindMotion.Type.IRON);
    }
}
