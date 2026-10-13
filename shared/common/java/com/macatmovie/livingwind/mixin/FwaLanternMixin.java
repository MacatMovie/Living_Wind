package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.FwaAnimationAccess;
import com.macatmovie.livingwind.client.compat.FwaWindMotion;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps FWA's renderer and chain-length constraints, altering only its computed angles. */
@Pseudo
@Mixin(targets = "fr.madu59.fwa.anims.LanternAnimation", remap = false)
public abstract class FwaLanternMixin {
    @Shadow(remap = false) private float tiltX;
    @Shadow(remap = false) private float tiltZ;
    @Shadow(remap = false) private float spin;

    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$applyWind(CallbackInfo ci) {
        BlockPos pos = ((FwaAnimationAccess)(Object)this).livingwind$getPosition();
        FwaWindMotion.Smoother smoother = FwaWindMotion.smootherFor(pos, FwaWindMotion.ObjectType.LANTERN);
        smoother.begin(new FwaWindMotion.Angles(tiltX, tiltZ, spin));
        FwaWindMotion.Angles angles = smoother.update(
            FwaWindMotion.swinging(pos, tiltX, tiltZ, spin, FwaWindMotion.ObjectType.LANTERN, smoother));
        tiltX = angles.x();
        tiltZ = angles.z();
        spin = angles.spin();
    }
}
