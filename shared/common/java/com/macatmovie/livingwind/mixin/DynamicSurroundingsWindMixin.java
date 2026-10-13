package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.config.ClientWindConfig;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Dynamic Surroundings integration. Remove only its biome.wind sound
 * factories from the current ambient mix before DS reconciles its playing loops.
 * Existing loops fade out normally; other biome sounds are not touched.
 *
 * No DS classes are linked at compile time. This also leaves the user's own DS
 * sound settings and all other DS audio untouched, including waterfall sounds.
 */
@Pseudo
@Mixin(targets = "org.orecruncher.dsurround.processing.BiomeSoundHandler", remap = false)
public abstract class DynamicSurroundingsWindMixin {
    @Shadow(remap = false)
    private Object2FloatOpenHashMap<Object> workMap;

    // Resolve once per concrete DS factory class, not once per emitter or audio tick.
    private static final ClassValue<Method> LIVINGWIND$FACTORY_LOCATION = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) {
            try {
                return type.getMethod("getLocation");
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    };

    @Inject(method = "queueAmbientSounds", at = @At("HEAD"), require = 0, remap = false)
    private void livingwind$filterDynamicSurroundingsWind(CallbackInfo ci) {
        if (ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get()
                != ClientWindConfig.DynamicSurroundingsWindMode.LIVING_WIND) return;
        if (workMap == null || workMap.isEmpty()) return;
        workMap.keySet().removeIf(DynamicSurroundingsWindMixin::livingwind$isWindFactory);
    }

    private static boolean livingwind$isWindFactory(Object factory) {
        if (factory == null) return false;
        Method method = LIVINGWIND$FACTORY_LOCATION.get(factory.getClass());
        if (method == null) return false;
        try {
            Object value = method.invoke(factory);
            if (value instanceof ResourceLocation id) {
                return "dsurround".equals(id.getNamespace())
                    && ("biome.wind".equals(id.getPath()) || id.getPath().startsWith("biome.wind."));
            }
        } catch (ReflectiveOperationException ignored) {
            // Dynamic Surroundings may alter its factory implementation in a future release.
        }
        return false;
    }
}
