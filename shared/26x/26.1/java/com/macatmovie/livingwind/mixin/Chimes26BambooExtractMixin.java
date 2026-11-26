package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.cicada.chimes.client.renderer.BambooBERenderer", remap=false)
public abstract class Chimes26BambooExtractMixin {
    @Inject(method="extractRenderState(Lcom/cicada/chimes/block/entity/WindChimeBE;Lcom/cicada/chimes/client/renderer/state/WindChimeRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at=@At("RETURN"), require=0, remap=false)
    private void livingwind$capture(@Coerce Object be, @Coerce Object state, float partialTick,
            net.minecraft.world.phys.Vec3 camera, @Coerce Object overlay, CallbackInfo ci) {
        if (state instanceof ChimesWindState access) access.livingwind$setWindSample(ChimesWindMotion.sample((BlockEntity)be));
    }
}
