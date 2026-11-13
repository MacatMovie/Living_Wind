package com.macatmovie.livingwind.client.particle;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * One tapered, gently animated ribbon = ONE translucent particle with four connected
 * double-sided segments. No entity, trail history or world/block checks during tick/render.
 */
public final class WindStreakParticle extends TextureSheetParticle {
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
        super(level, x, y, z);
        this.pickSprite(sprites);
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

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float px = (float)(Mth.lerp(partialTick, this.xo, this.x) - cam.x);
        float py = (float)(Mth.lerp(partialTick, this.yo, this.y) - cam.y);
        float pz = (float)(Mth.lerp(partialTick, this.zo, this.z) - cam.z);

        // Align the long edge to wind in WORLD space, but rotate the thin edge to
        // face the camera. This avoids billboard streaks pointing randomly on screen.
        // No temporary vectors or geometry arrays are allocated per frame.
        float cx = -px, cy = -py, cz = -pz;
        float acrossX = -dirZ * cy;
        float acrossY = dirZ * cx - dirX * cz;
        float acrossZ = dirX * cy;
        float acrossLen = Mth.sqrt(acrossX * acrossX + acrossY * acrossY + acrossZ * acrossZ);
        if (acrossLen < 0.01f) {
            acrossX = 0.0f; acrossY = 1.0f; acrossZ = 0.0f;
        } else {
            acrossX /= acrossLen; acrossY /= acrossLen; acrossZ /= acrossLen;
        }
        float wx = acrossX * width * 0.5f;
        float wy = acrossY * width * 0.5f;
        float wz = acrossZ * width * 0.5f;
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = getLightColor(partialTick);

        // Gently undulate the *shape* of the streak, rather than only wobbling its
        // particle position. The displacement is in the camera-facing thin-edge
        // direction, so the curve stays visible from different view angles.
        // A shared time phase keeps neighbouring joints moving smoothly. Texture
        // coordinates advance over the entire ribbon, with no repeated sprites.
        // All geometry uses locals: no per-frame arrays, vectors or allocations.
        float waveTime = (this.age + partialTick) * 0.105f + phase;
        float bend = 0.19f + width * 0.40f;
        float startX = px - dirX * length * 0.5f;
        float startY = py;
        float startZ = pz - dirZ * length * 0.5f;
        float startU = u0;
        for (int joint = 1; joint <= RIBBON_SEGMENTS; joint++) {
            float t = joint * (1.0f / RIBBON_SEGMENTS);
            // Endpoints remain fixed. Interior samples smoothly follow a
            // travelling sine wave whose amplitude tapers toward both ends.
            float envelope = joint == RIBBON_SEGMENTS ? 0.0f
                    : joint == 2 ? 1.0f : QUARTER_SINE;
            float sideways = envelope * bend * (float) Math.sin(waveTime + t * 3.6f);
            float along = (t - 0.5f) * length;
            float endX = px + dirX * along + acrossX * sideways;
            float endY = py + acrossY * sideways;
            float endZ = pz + dirZ * along + acrossZ * sideways;
            float endU = u0 + (u1 - u0) * t;

            // Four vertices per side; both sides render with and without shaders.
            vertex(buffer, startX-wx, startY-wy, startZ-wz, startU, v1, light);
            vertex(buffer, startX+wx, startY+wy, startZ+wz, startU, v0, light);
            vertex(buffer, endX+wx, endY+wy, endZ+wz, endU, v0, light);
            vertex(buffer, endX-wx, endY-wy, endZ-wz, endU, v1, light);
            vertex(buffer, endX-wx, endY-wy, endZ-wz, endU, v1, light);
            vertex(buffer, endX+wx, endY+wy, endZ+wz, endU, v0, light);
            vertex(buffer, startX+wx, startY+wy, startZ+wz, startU, v0, light);
            vertex(buffer, startX-wx, startY-wy, startZ-wz, startU, v1, light);

            startX = endX;
            startY = endY;
            startZ = endZ;
            startU = endU;
        }
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xs, double ys, double zs) {
            return new WindStreakParticle(level, x, y, z, sprites);
        }
    }
}
