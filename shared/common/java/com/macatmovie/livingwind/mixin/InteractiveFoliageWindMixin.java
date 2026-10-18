package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import org.joml.Vector4f;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional IF 2.2.0 weather wind; avoids all hard IF type references. */
@Pseudo
@Mixin(targets = "net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageRenderer", remap = false)
public abstract class InteractiveFoliageWindMixin {
    @Inject(method = "weather()Lorg/joml/Vector4f;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void livingwind$weather(CallbackInfoReturnable<Vector4f> ci) {
        ci.setReturnValue(InteractiveFoliageWindCompat.adjustWeather(ci.getReturnValue()));
    }
    // IF 2.2.0 only calculates wind shelter for rainy, fixed-westward wind.
    // Disable that outdated shelter mask when using our variable wind direction;
    // otherwise grass can appear sheltered on the wrong side of walls.
    @Redirect(method = "followRain(Lnet/minecraft/client/Minecraft;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F", remap = false),
            require = 0, remap = false)
    private static float livingwind$foliageRainShelter(ClientLevel level, float partialTick) {
        return InteractiveFoliageWindCompat.shelterRainLevel(level.getRainLevel(partialTick));
    }
    @Inject(method = "calmSway()F", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void livingwind$calm(CallbackInfoReturnable<Float> ci) {
        ci.setReturnValue(InteractiveFoliageWindCompat.adjustCalmSway(ci.getReturnValue()));
    }
}
