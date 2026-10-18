package com.macatmovie.livingwind.mixin;
import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Wraps only IF's own legacy foliage shader provider, without affecting other shaders. */
@Pseudo
@Mixin(targets = "net.karto.mc2.mc2_interactivefoliage.platform.neoforge.NeoforgeFoliageHooks", remap = false)
public abstract class InteractiveFoliageLegacyShaderMixin {
    @ModifyArg(method = "registerShaders", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/renderer/ShaderInstance;<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"),
        index = 0, require = 0, remap = false)
    private static ResourceProvider livingwind$legacyFoliageShader(ResourceProvider original) {
        return InteractiveFoliageWindCompat.wrapLegacyProvider(original);
    }
}
