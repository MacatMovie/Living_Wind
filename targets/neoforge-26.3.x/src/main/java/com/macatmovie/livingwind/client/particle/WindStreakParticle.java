package com.macatmovie.livingwind.client.particle;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One tapered, gently animated ribbon = ONE translucent particle with four connected
 * double-sided segments. No entity, trail history or world/block checks during tick/render.
 */
public final class WindStreakParticle extends SingleQuadParticle {
    private static final AtomicInteger ACTIVE = new AtomicInteger();
    // Four ribbon segments, five joints. Keep the endpoints pinned and bend only
    // the interior; this preserves the full source texture's fade/taper.
    private static final int RIBBON_SEGMENTS = 4;
    private static final float QUARTER_SINE = 0.70710677f;

    private final float length;
    private final float width;
    private final float baseAlpha;
    private final float phase;
    private float dirX, dirZ;
    private boolean released;

    private WindStreakParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.setSprite(sprites.get(this.random));
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = 36 + random.nextInt(24);
        this.length = 2.75f + random.nextFloat() * 3.0f;
        this.width = 0.22f + random.nextFloat() * 0.11f;
        this.baseAlpha = 0.42f + random.nextFloat() * 0.16f;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = 0.1f;
        this.setColor(0.88f, 0.94f, 1.0f);
        this.setAlpha(0.0f);
        float radians = (float) Math.toRadians(ClientWindState.directionDegrees());
        this.dirX = -Mth.sin(radians);
        this.dirZ = Mth.cos(radians);
        ACTIVE.incrementAndGet();
    }

    public static int activeCount() { return ACTIVE.get(); }
    public static void resetActiveCount() { ACTIVE.set(0); }

    @Override
    public void remove() {
        if (!released) {
            released = true;
            ACTIVE.updateAndGet(value -> Math.max(0, value - 1));
        }
        super.remove();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (!ClientWindConfig.WIND_STREAKS_ENABLED.get() || age++ >= lifetime) {
            remove();
            return;
        }
        float radians = (float) Math.toRadians(ClientWindState.directionDegrees());
        float targetX = -Mth.sin(radians);
        float targetZ = Mth.cos(radians);
        this.dirX += (targetX - this.dirX) * 0.13f;
        this.dirZ += (targetZ - this.dirZ) * 0.13f;
        float norm = Mth.sqrt(this.dirX * this.dirX + this.dirZ * this.dirZ);
        if (norm > 0.01f) {
            this.dirX /= norm;
            this.dirZ /= norm;
        }
        // Very gentle low-frequency movement, not jittery point-by-point trails.
        float strength = Math.min(2.5f, ClientWindState.altitudeStrength());
        double speed = 0.12 + 0.065 * strength;
        float turbulence = ClientWindState.turbulence();
        double cross = Math.sin(age * 0.12f + phase) * (0.0015 + 0.0030 * turbulence);
        this.move(dirX * speed - dirZ * cross,
            0.0012 * Math.sin(age * 0.09f + phase),
            dirZ * speed + dirX * cross);

        float fadeIn = Mth.clamp(age / 9.0f, 0.0f, 1.0f);
        float fadeOut = Mth.clamp((lifetime - age) / 12.0f, 0.0f, 1.0f);
        setAlpha(baseAlpha * Math.min(fadeIn, fadeOut));
    }

    @Override protected SingleQuadParticle.Layer getLayer() { return SingleQuadParticle.Layer.TRANSLUCENT; }

    // 26.x quad extraction accepts square quads rather than arbitrary vertices.
    // Divide the ribbon texture across adjacent small quads, retaining its wind alignment and bend.
    @Override public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        Vec3 cam = camera.position();
        float px = (float)(Mth.lerp(partialTick, xo, x) - cam.x);
        float py = (float)(Mth.lerp(partialTick, yo, y) - cam.y);
        float pz = (float)(Mth.lerp(partialTick, zo, z) - cam.z);
        Vector3f along = new Vector3f(dirX, 0, dirZ).normalize();
        Vector3f across = new Vector3f(along).cross(-px, -py, -pz);
        if (across.lengthSquared() < 0.0001f) across.set(0, 1, 0);
        else across.normalize();
        Vector3f normal = new Vector3f(along).cross(across).normalize();
        Quaternionf rotation = new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, along).setColumn(1, across).setColumn(2, normal));
        int segments = Math.max(1, (int)Math.ceil(length / width));
        float step = length / segments;
        int color = net.minecraft.util.ARGB.colorFromFloat(alpha, rCol, gCol, bCol);
        int light = getLightCoords(partialTick);
        float waveTime = (age + partialTick) * 0.105f + phase;
        float bend = 0.19f + width * 0.40f;
        for (int i = 0; i < segments; i++) {
            float t = (i + 0.5f) / segments;
            float side = (float)Math.sin(t * Math.PI) * bend * (float)Math.sin(waveTime + t * 3.6f);
            float dist = (t - 0.5f) * length;
            // Slightly overlap tiles to avoid cracks at the joints.
            state.add(getLayer(), px + along.x * dist + across.x * side,
                py + along.y * dist + across.y * side, pz + along.z * dist + across.z * side,
                rotation.x, rotation.y, rotation.z, rotation.w, step * 0.52f,
                Mth.lerp((float)i / segments, getU0(), getU1()),
                Mth.lerp((float)(i+1) / segments, getU0(), getU1()), getV0(), getV1(), color, light);
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xs, double ys, double zs, net.minecraft.util.RandomSource providerRandom) {
            return new WindStreakParticle(level, x, y, z, sprites);
        }
    }
}
