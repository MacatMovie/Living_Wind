package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 2.2.1 ships the followRain hook in intermediary mappings, even though
 * Living Wind's shared Forge/NeoForge mixin uses official Minecraft mappings.
 * The uploaded 1.20.1 and 1.21.1 JARs both call class_638.method_8430(F)F.
 *
 * Do not let IF's fixed-west rain shelter mask suppress grass on the wrong side
 * of a wall when Living Wind is steering the grass in a different direction.
 */
@Pseudo
@Mixin(targets = "net.karto.mc2.mc2_interactivefoliage.gpu.GpuFoliageRenderer", remap = false)
public abstract class FabricInteractiveFoliageShelterMixin {
    @Redirect(method = "followRain(Lnet/minecraft/class_310;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_638;method_8430(F)F", remap = false),
            remap = false, require = 0)
    private static float livingwind$correctFoliageShelterDirection(ClientLevel level, float partialTick) {
        return InteractiveFoliageWindCompat.shelterRainLevel(level.getRainLevel(partialTick));
    }
}
