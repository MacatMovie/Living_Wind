package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.SwingingLanternsWindCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.rilith.swinginglanterns.client.render.SwingingLanternRenderer", remap=false)
public abstract class SwingingLanternsWindMixin {
    @Inject(method="updateWindExposure(Ldev/rilith/swinginglanterns/client/render/SwingingLanternRenderer$ExposureState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ldev/rilith/swinginglanterns/client/render/SwingingLanternRenderState;DFF)V", at=@At(value="INVOKE", target="Ldev/rilith/swinginglanterns/client/render/SwingingLanternRenderer;updateSpeedMultiplier(Ldev/rilith/swinginglanterns/client/render/SwingingLanternRenderer$ExposureState;DF)V", shift=At.Shift.AFTER), require=0, remap=false)
    private static void livingwind$replaceLanternWind(@Coerce Object exposure, Level level, BlockPos pos,
            @Coerce Object state, double time, float rain, float thunder, CallbackInfo ci) {
        SwingingLanternsWindCompat.apply(level, pos, exposure);
    }
}
