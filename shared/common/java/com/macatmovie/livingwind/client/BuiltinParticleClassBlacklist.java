package com.macatmovie.livingwind.client;

import java.util.Set;
import net.minecraft.client.particle.Particle;
import org.jetbrains.annotations.Nullable;

/**
 * Fallback for effects that are instantiated directly or registered through non-vanilla
 * particle registries (notably Visuality). No hard dependency on any optional mod.
 * Per-class decisions are cached by the JVM, safe for asynchronous particle workers.
 */
public final class BuiltinParticleClassBlacklist {
    private static final Set<String> IMMUNE_CLASSES = Set.of(
        // Dynamic Surroundings creates these directly, often using vanilla particle types.
        // Never blacklist vanilla END_ROD globally: only DS fireflies are exempt.
        // Water foam, surface ripples and waterfall mist have their own fluid motion.
        "org.orecruncher.dsurround.effects.particles.FireflyParticle",
        "org.orecruncher.dsurround.effects.particles.WaterFoam",
        "org.orecruncher.dsurround.effects.particles.WaterRippleParticle",
        "org.orecruncher.dsurround.effects.particles.WaterfallMist",
        // Coastal Waves' directly-added particle handler has no normal particle ID.
        "waves.client.particle.WaveParticleHandler",
        // Visuality 3.x uses a custom particle registry, so ordinary IDs are insufficient.
        "plus.dragons.visuality.particle.SparkleParticle",
        "plus.dragons.visuality.particle.SlimeParticle",
        "plus.dragons.visuality.particle.SolidFallingParticle",
        "plus.dragons.visuality.particle.FeatherParticle",
        "plus.dragons.visuality.particle.ChargeParticle",
        "plus.dragons.visuality.particle.WaterCircleParticle",
        // Footprint Particle can create particles directly, without ParticleEngine ID capture.
        "com.rimo.footprintparticle.particle.FootprintParticle",
        "com.rimo.footprintparticle.particle.WatermarkParticle",
        "com.rimo.footprintparticle.particle.SnowDustParticle",
        "com.rimo.footprintparticle.particle.WaterSplashParticle",
        // Explosion sequences must not pick up additional ambient wind when directly added.
        "dev.xylonity.explosiveenhancement.particle.custom.BlastWaveParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.FireballParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.ShockwaveParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.SmokeParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.SparksParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.BubbleParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.UnderwaterBlastwaveParticle",
        "dev.xylonity.explosiveenhancement.particle.custom.UnderwaterSparksParticle"
    );

    private static final ClassValue<Boolean> IMMUNE_BY_CLASS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            // What Are They Up To? (WATUT) constructs its chat/GUI, idle, and item
            // display particles directly and uses its own CustomParticleEngine rather
            // than a registry ParticleType. Their positions are anchored to players
            // each tick, so a generic wind move() offset makes the displays jitter.
            // Match only WATUT's dedicated particle package; no optional-mod API,
            // dependency, or per-tick reflection is necessary.
            String name = type.getName();
            return name.startsWith("com.corosus.watut.particle.") || IMMUNE_CLASSES.contains(name);
        }
    };

    private BuiltinParticleClassBlacklist() {}

    public static boolean contains(@Nullable Particle particle) {
        return particle != null && IMMUNE_BY_CLASS.get(particle.getClass());
    }
}
