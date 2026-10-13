package com.macatmovie.livingwind.client.particle;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.ParticleWaterProtection;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A deliberately tiny, lightweight wind mote. It is independent code designed around the visual
 * goal of an airborne pixel: low gravity, long-ish lifetime, gentle vertical motion and continuous
 * steering by Living Wind's current vector.
 */
public final class AmbientPixelParticle extends TextureSheetParticle {
    private final float phaseA;
    private final float phaseB;
    private final float baseAlpha;
    private final float swayBias;
    private final float liftBias;

    private AmbientPixelParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double red,
        double green,
        double blue,
        SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 0.99f;
        int minLifetime = ClientWindConfig.AMBIENT_LIFETIME_MIN_TICKS.get();
        int maxLifetime = Math.max(minLifetime, ClientWindConfig.AMBIENT_LIFETIME_MAX_TICKS.get());
        this.lifetime = minLifetime + this.random.nextInt(maxLifetime - minLifetime + 1);
        this.quadSize = 0.026f + this.random.nextFloat() * 0.010f;
        this.phaseA = this.random.nextFloat() * Mth.TWO_PI;
        this.phaseB = this.random.nextFloat() * Mth.TWO_PI;
        this.baseAlpha = 0.58f + this.random.nextFloat() * 0.20f;
        this.swayBias = 0.78f + this.random.nextFloat() * 0.44f;
        this.liftBias = 0.78f + this.random.nextFloat() * 0.44f;
        this.setAlpha(0.0f);
        this.setColor(
            Mth.clamp((float) red, 0.0f, 1.0f),
            Mth.clamp((float) green, 0.0f, 1.0f),
            Mth.clamp((float) blue, 0.0f, 1.0f)
        );

        float wind = ParticleWaterProtection.isSubmerged(this.x, this.y, this.z)
            ? 0.0f : ClientWindState.altitudeStrength() * WindShelter.exposureAt(this.x, this.y, this.z);
        double responseMultiplier = ParticleWindFilter.ambientWindResponseMultiplier();
        double initialScale = (0.025 + Math.min(wind, 1.6f) * 0.020) * responseMultiplier;
        this.xd = wind > 0.0f ? ClientWindState.altitudeWindX() * WindShelter.exposureAt(this.x, this.y, this.z) * initialScale * WindMath.visualDriftBoost(wind) : 0.0;
        this.zd = wind > 0.0f ? ClientWindState.altitudeWindZ() * WindShelter.exposureAt(this.x, this.y, this.z) * initialScale * WindMath.visualDriftBoost(wind) : 0.0;
        // A tiny initial lift helps the mote look picked up by air instead of launched like block debris.
        this.yd = 0.0045 + (this.random.nextDouble() - 0.5) * 0.006;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        boolean underwater = ParticleWaterProtection.isSubmerged(this.x, this.y, this.z);
        float wind = underwater ? 0.0f : ClientWindState.altitudeStrength() * WindShelter.exposureAt(this.x, this.y, this.z);
        float turbulence = underwater ? 0.0f : ClientWindState.turbulence();
        float cappedWind = Math.min(wind, 1.6f);

        // Continuously follow the wind rather than receiving one launch impulse. This makes a mote
        // hang in the air and bend with gusts/direction changes instead of behaving like block debris.
        double responseMultiplier = ParticleWindFilter.ambientWindResponseMultiplier();
        double targetSpeed = (0.026 + cappedWind * 0.042) * responseMultiplier;

        // Main motion follows the synchronized wind, but tiny debris should not travel on ruler-straight
        // trajectories. Add a low-frequency cross-wind component that varies per mote and becomes a
        // little more visible as turbulence/instability rise. This is smooth meandering, not random jitter.
        double radians = Math.toRadians(ClientWindState.directionDegrees());
        double unitX = -Math.sin(radians);
        double unitZ = Math.cos(radians);
        double perpX = -unitZ;
        double perpZ = unitX;
        double swayMultiplier = ParticleWindFilter.ambientSwayMultiplier();
        double swayWave = Math.sin(this.age * 0.055 + this.phaseA) * 0.72
            + Math.sin(this.age * 0.021 + this.phaseB) * 0.28;
        double swayFraction = swayWave * this.swayBias * swayMultiplier
            * (0.035 + turbulence * 0.105 + ClientWindState.directionInstability() * 0.055);
        double alongPulse = 1.0 + Math.sin(this.age * 0.033 + this.phaseB) * turbulence * 0.055;
        double mainMagnitude = underwater ? 0.0 : cappedWind * targetSpeed * alongPulse * WindMath.visualDriftBoost(wind);
        double targetX = unitX * mainMagnitude + perpX * mainMagnitude * swayFraction;
        double targetZ = unitZ * mainMagnitude + perpZ * mainMagnitude * swayFraction;
        double steering = 0.055 + cappedWind * 0.025 + turbulence * 0.018;
        this.xd += (targetX - this.xd) * steering;
        this.zd += (targetZ - this.zd) * steering;
        if (underwater) { this.xd *= 0.82; this.zd *= 0.82; }

        // Slight positive lift plus two slow waves makes the mote feel suspended/picked up by moving
        // air. Strong wind lifts a little more, but the effect remains much smaller than horizontal drift.
        double liftMultiplier = ParticleWindFilter.ambientLiftMultiplier();
        double bob = Math.sin(this.age * 0.17 + this.phaseA) * 0.0028
            + Math.sin(this.age * 0.061 + this.phaseB) * 0.0016;
        double windLift = (0.0038 + cappedWind * 0.0036 + turbulence * 0.0015)
            * this.liftBias * liftMultiplier;
        double targetY = underwater ? 0.0 : windLift + bob;
        this.yd += (targetY - this.yd) * 0.105;

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.993;
        this.zd *= 0.993;
        this.yd *= 0.965;

        float fadeIn = Math.min(1.0f, this.age / 8.0f);
        float fadeOut = Math.min(1.0f, (this.lifetime - this.age) / 14.0f);
        this.setAlpha(this.baseAlpha * Math.max(0.0f, Math.min(fadeIn, fadeOut)));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
            SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
        ) {
            // For this purely client-generated particle the three speed channels carry RGB.
            return new AmbientPixelParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
