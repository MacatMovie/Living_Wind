package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.InteractiveFoliageWindCompat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Fabric Interactive Foliage 2.2.1 registers its core foliage shader through
 * Fabric API, not ForgeFoliageHooks. That route loads the legacy vertex shader
 * directly through ShaderInstance, bypassing IF's readSway() injection points.
 *
 * Only when Interactive Foliage itself is installed, wrap the resource provider
 * for shader creation. The wrapper preserves every unrelated resource and only
 * patches IF's own foliage_legacy / foliage_legacy_1_20 vertex shader.
 * The same shader replacement is already used by the stable Forge integration.
 * All other renderers (including Iris) retain their original resource providers.
 */
@Mixin(ShaderInstance.class)
public abstract class FabricInteractiveFoliageLegacyShaderMixin {
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static ResourceProvider livingwind$wrapInteractiveFoliageLegacyShader(ResourceProvider provider) {
        return InteractiveFoliageWindCompat.wrapLegacyProvider(provider);
    }
}
