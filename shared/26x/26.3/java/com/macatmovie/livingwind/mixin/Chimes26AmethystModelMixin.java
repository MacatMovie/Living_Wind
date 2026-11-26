package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.model.AmethystModel", remap=false)
public abstract class Chimes26AmethystModelMixin {
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
    @Unique private float[] livingwind$x, livingwind$z;
    @Inject(method="setupAnim(Lcom/cicada/chimes/client/renderer/state/WindChimeRenderState;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$animate(@Coerce Object state, CallbackInfo ci) {
        if (!(state instanceof ChimesWindState access)) return;
        if (livingwind$parts == null) { livingwind$parts = new ModelPart[] {SmallStringA, MedStringA, BigStringA, LarStringA, SmallStringB, MedStringB, BigStringB, LarStringB}; livingwind$x=new float[livingwind$parts.length]; livingwind$z=new float[livingwind$parts.length]; }
        ChimesWindMotion.applySample(access.livingwind$getWindSample(), livingwind$parts, livingwind$x, livingwind$z, UpperTopperBone.yRot, ChimesWindMotion.Type.AMETHYST);
    }
}
