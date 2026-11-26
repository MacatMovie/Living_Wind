package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.ChimesWindMotion;
import com.macatmovie.livingwind.client.compat.ChimesWindState;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets={"com.cicada.chimes.client.renderer.state.WindChimeRenderState", "com.cicada.chimes.client.renderer.state.WindBellRenderState"}, remap=false)
public abstract class Chimes26StateMixin implements ChimesWindState {
    @Unique private ChimesWindMotion.Sample livingwind$sample;
    public void livingwind$setWindSample(ChimesWindMotion.Sample sample) { livingwind$sample=sample; }
    public ChimesWindMotion.Sample livingwind$getWindSample() { return livingwind$sample; }
}
