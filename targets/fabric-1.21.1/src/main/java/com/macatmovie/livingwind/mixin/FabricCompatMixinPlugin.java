package com.macatmovie.livingwind.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Only attach third-party client mixins when their owning mod is installed. */
public final class FabricCompatMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String mod = null;
        if (mixinClassName.endsWith("WindblownFrameMixin") || mixinClassName.endsWith("WindblownShaderMixin"))
            mod = "windblown";
        else if (mixinClassName.contains("SwingingLanterns")) mod = "swinginglanterns";
        else if (mixinClassName.startsWith("com.macatmovie.livingwind.mixin.Chimes")) mod = "chimes";
        else if (mixinClassName.endsWith("SwingingSignsSwayMixin")) mod = "swingingsigns";
        else if (mixinClassName.contains("OfficialParticleRain")) mod = "particlerain";
        else if (mixinClassName.contains("InteractiveFoliage")) mod = "mc2_interactivefoliage";
        else if (mixinClassName.endsWith("AmbientSoundsWindCategoryMixin")) mod = "ambientsounds";
        else if (mixinClassName.endsWith("AmbiWindSoundMixin")) mod = "ambimod";
        else if (mixinClassName.endsWith("DynamicSurroundingsWindMixin") || mixinClassName.endsWith("DynamicSurroundingsParticleWindMixin")) mod = "dsurround";
        else if (mixinClassName.endsWith("SupplementariesWindVaneMixin")) mod = "supplementaries";
        return mod == null || FabricLoader.getInstance().isModLoaded(mod);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
