package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.WindblownWindCompat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

/** Optional: override only Windblown's already-computed per-frame GPU wind inputs. */
@Pseudo
@Mixin(targets = "dev.windblown.client.WindFrame", remap = false)
public abstract class WindblownFrameMixin {
    @Shadow(remap = false) @Final private static float[] FRAME;
    @Shadow(remap = false) @Final private static ByteBuffer BYTES_BUFFER;

    @Inject(method = "update(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"), require = 0, remap = false)
    private static void livingwind$frame(Vec3 camera, CallbackInfo ci) {
        WindblownWindCompat.adjustFrame(FRAME, BYTES_BUFFER, camera);
    }
}
