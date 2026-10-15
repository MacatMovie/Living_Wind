package com.macatmovie.livingwind.client.particle;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.client.ParticleShelterContinuity;
import com.macatmovie.livingwind.client.ParticleWaterProtection;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A native Minecraft sheet particle, with continuously steered drifting and a 3-axis tumble. */
public final class WindLeafParticle extends TextureSheetParticle {
    private final boolean grassFragment;
    private final float phase;
    private final float phase2;
    private final float wobble;
    private final float windAffinity;
    private final float spinX;
    private final float spinY;
    private final float spinZ;
    private float pitchAngle;
    private float prevPitchAngle;
    private float yawAngle;
    private float prevYawAngle;
    private float rollAngle;
    private float prevRollAngle;
    private int groundTicks;

    // Primitive-only per-particle continuity: native particles bypass the generic
    // wind hook, so they need its confirmed-shelter grace rule themselves.
    private int lastShelteredTick = -1;
    private int lastShelteredX, lastShelteredY, lastShelteredZ;

    private float nativeExposure() {
        int now = WindShelter.shelterTick();
        int bx = Mth.floor(this.x), by = Mth.floor(this.y), bz = Mth.floor(this.z);
        float measured = WindShelter.trackedParticleExposureAt(this.x, this.y, this.z);
        if (measured >= 0.0f && measured < 0.20f) {
            lastShelteredTick = now;
            lastShelteredX = bx; lastShelteredY = by; lastShelteredZ = bz;
        }
        // Missing position data must not create a player-centered windless boundary.
        // The emitter already validates shelter before spawning; during flight, use
        // the same short indoor grace and outdoor fallback as ordinary particles.
        return ParticleShelterContinuity.resolve(measured, now, lastShelteredTick,
            bx, by, bz, lastShelteredX, lastShelteredY, lastShelteredZ);
    }

    private WindLeafParticle(ClientLevel level, double x, double y, double z,
                             double red, double green, double blue, SpriteSet spriteSet, boolean grassFragment) {
        super(level, x, y, z);
        this.grassFragment = grassFragment;
        this.pickSprite(spriteSet);
        this.quadSize = grassFragment ? 0.065f + random.nextFloat() * 0.045f : 0.105f + random.nextFloat() * 0.055f;
        this.lifetime = grassFragment ? 65 + random.nextInt(85) : 95 + random.nextInt(100);
        this.hasPhysics = true;
        this.gravity = 0.0f; // Custom gravity varies while floating and after landing.
        this.friction = 0.98f;
        this.setColor(Mth.clamp((float)red,0,1), Mth.clamp((float)green,0,1), Mth.clamp((float)blue,0,1));
        this.alpha = 0.0f;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.phase2 = random.nextFloat() * Mth.TWO_PI;
        this.wobble = 0.7f + random.nextFloat() * 0.65f;
        this.windAffinity = 0.72f + random.nextFloat() * 0.60f;
        this.spinX = (random.nextBoolean() ? 1 : -1) * (0.046f + random.nextFloat() * 0.065f);
        this.spinY = (random.nextBoolean() ? 1 : -1) * (0.032f + random.nextFloat() * 0.06f);
        this.spinZ = (random.nextBoolean() ? 1 : -1) * (0.05f + random.nextFloat() * 0.09f);
        this.pitchAngle = random.nextFloat() * Mth.TWO_PI;
        this.yawAngle = random.nextFloat() * Mth.TWO_PI;
        this.rollAngle = random.nextFloat() * Mth.TWO_PI;
        float exposure = nativeExposure();
        float w = ParticleWaterProtection.isSubmerged(this.x, this.y, this.z)
            ? 0.0f : ClientWindState.altitudeStrength() * exposure;
        // Dedicated foliage fragments travel 30% farther with the wind; grass fragments keep their old feel.
        // Apply the boost here as well as to the steering target so newly emitted leaves do not start slower.
        double foliageWindBoost = grassFragment ? 1.0 : 1.30;
        double initialDriftBoost = WindMath.visualDriftBoost(w) * foliageWindBoost;
        this.xd = w > 0.0f ? ClientWindState.altitudeWindX() * exposure * (0.085 + w * 0.025) * initialDriftBoost : 0.0;
        this.zd = w > 0.0f ? ClientWindState.altitudeWindZ() * exposure * (0.085 + w * 0.025) * initialDriftBoost : 0.0;
        this.yd = (random.nextDouble() - 0.5) * 0.025;
    }

    @Override public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }

        this.prevPitchAngle = pitchAngle;
        this.prevYawAngle = yawAngle;
        this.prevRollAngle = rollAngle;
        boolean underwater = ParticleWaterProtection.isSubmerged(this.x, this.y, this.z);
        float observedWind = underwater ? 0.0f
            : Mth.clamp(ClientWindState.altitudeStrength() * nativeExposure(), 0f, 2.5f);
        float wind = Math.min(observedWind, 1.8f);
        float turbulence = underwater || wind <= 0.001f ? 0.0f : ClientWindState.turbulence();
        // This runtime multiplier also applies to existing player configs saved with windResponse=1.0.
        double power = ClientWindConfig.LEAF_PARTICLE_WIND_RESPONSE.get() * windAffinity * (grassFragment ? 1.0 : 1.30);
        // Stronger high-end drift without changing the server weather or gust distributions.
        // 2.0 wind -> 1.60x motion; calm wind stays almost unchanged.
        double highWindBoost = WindMath.visualDriftBoost(observedWind);
        // The nonzero .052 baseline is only for moving air. At zero local wind
        // (indoors, caves, underwater), the steering target must be exactly zero.
        double targetSpeed = wind <= 0.001f ? 0.0 : (0.052 + 0.11 * wind) * power * highWindBoost;
        double angle = Math.toRadians(ClientWindState.directionDegrees());
        double dx = -Math.sin(angle), dz = Math.cos(angle);
        double perpendicularX = -dz, perpendicularZ = dx;
        double meander = Math.sin(age * 0.075 + phase) * 0.13 + Math.sin(age * 0.031 + phase2) * 0.065;
        meander *= wobble * (0.45 + turbulence * 0.60);
        double speedPulse = 0.93 + 0.12 * Math.sin(age * 0.05 + phase2);
        double tx = (dx + perpendicularX * meander) * targetSpeed * speedPulse;
        double tz = (dz + perpendicularZ * meander) * targetSpeed * speedPulse;
        this.xd += (tx - xd) * (0.045 + wind * 0.026);
        this.zd += (tz - zd) * (0.045 + wind * 0.026);
        if (underwater || wind <= 0.001f) { this.xd *= 0.65; this.zd *= 0.65; }

        // Gravity plus drifting lift: calm leaves settle, gusts briefly suspend and loft them.
        double eddy = Math.sin(age * 0.088 + phase) * 0.008 + Math.sin(age * 0.037 + phase2) * 0.006;
        double lift = underwater ? 0.0 : Math.max(0, wind - 0.37) * 0.012 * (0.5 + turbulence);
        this.yd += ((grassFragment ? -0.0100 : -0.0071) + lift + eddy - yd * 0.018) * 0.10;
        this.yd = Mth.clamp(this.yd, -0.125, 0.10);
        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            groundTicks++;
            this.xd *= 0.71;
            this.zd *= 0.71;
            if (wind > 0.24f && random.nextFloat() < (0.025f + wind * 0.045f)) {
                this.yd = 0.027 + random.nextDouble() * 0.045 + wind * 0.014;
                this.xd += dx * 0.025 * wind;
                this.zd += dz * 0.025 * wind;
                groundTicks = 0;
            }
            if (groundTicks > 28 + random.nextInt(15)) this.remove();
        } else groundTicks = 0;

        // Three independently changing axes give a turning, tumbling silhouette rather than a
        // perfectly camera-facing speck. Angular speed gently increases with wind speed.
        float motionSpin = (0.68f + wind * 0.58f) * (float)(0.7 + 0.3 * turbulence);
        pitchAngle += spinX * motionSpin + (float)Math.sin(age * 0.06 + phase) * 0.017;
        yawAngle += spinY * motionSpin;
        rollAngle += spinZ * motionSpin;

        float fadeIn = Mth.clamp(age / 7f, 0, 1);
        float fadeOut = Mth.clamp((lifetime - age) / 18f, 0, 1);
        this.alpha = 0.93f * Math.min(fadeIn, fadeOut);
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float px = (float)(Mth.lerp(partialTick, this.xo, this.x) - cam.x);
        float py = (float)(Mth.lerp(partialTick, this.yo, this.y) - cam.y);
        float pz = (float)(Mth.lerp(partialTick, this.zo, this.z) - cam.z);
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.rotateY(Mth.lerp(partialTick, prevYawAngle, yawAngle));
        rotation.rotateX(Mth.lerp(partialTick, prevPitchAngle, pitchAngle));
        rotation.rotateZ(Mth.lerp(partialTick, prevRollAngle, rollAngle));
        float size = this.getQuadSize(partialTick);
        float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTick);
        Vector3f[] corners = {
            new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0),
            new Vector3f( 1, 1, 0), new Vector3f( 1,-1, 0)
        };
        for (Vector3f c : corners) { rotation.transform(c); c.mul(size).add(px, py, pz); }
        // Both windward and leeward faces remain visible as the leaf flips in 3D.
        vertex(buffer, corners[0], u1, v1, light);
        vertex(buffer, corners[1], u1, v0, light);
        vertex(buffer, corners[2], u0, v0, light);
        vertex(buffer, corners[3], u0, v1, light);
        vertex(buffer, corners[3], u0, v1, light);
        vertex(buffer, corners[2], u0, v0, light);
        vertex(buffer, corners[1], u1, v0, light);
        vertex(buffer, corners[0], u1, v1, light);
    }

    private void vertex(VertexConsumer buffer, Vector3f p, float u, float v, int light) {
        buffer.vertex(p.x(), p.y(), p.z()).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    /** Native grass-blade sprites share wind steering/tumbling but use shorter lifetimes. */
    public static final class GrassProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public GrassProvider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xs, double ys, double zs) {
            return new WindLeafParticle(level, x, y, z, xs, ys, zs, sprites, true);
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xs, double ys, double zs) {
            return new WindLeafParticle(level, x, y, z, xs, ys, zs, sprites, false);
        }
    }
}
