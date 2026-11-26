package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.model.GlassModel", remap=false)
public abstract class Chimes26GlassModelMixin {
    @Shadow(remap = false) @Final private ModelPart GlassBowlBone;
    @Shadow(remap = false) @Final private ModelPart ChimeStringBone;
    @Unique private ModelPart[] livingwind$parts;
    @Unique private float[] livingwind$x, livingwind$z;
    @Inject(method="setupAnim(Lcom/cicada/chimes/client/renderer/state/WindBellRenderState;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$animate(@Coerce Object state, CallbackInfo ci) {
        if (!(state instanceof ChimesWindState access)) return;
        if (livingwind$parts == null) { livingwind$parts = new ModelPart[] {GlassBowlBone, ChimeStringBone}; livingwind$x=new float[livingwind$parts.length]; livingwind$z=new float[livingwind$parts.length]; }
        ChimesWindMotion.applySample(access.livingwind$getWindSample(), livingwind$parts, livingwind$x, livingwind$z, 0f, ChimesWindMotion.Type.GLASS);
    }
}
