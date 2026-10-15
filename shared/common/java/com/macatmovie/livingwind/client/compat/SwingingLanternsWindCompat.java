package com.macatmovie.livingwind.client.compat;

import com.macatmovie.livingwind.client.ClientWindState;
import com.macatmovie.livingwind.client.WindShelter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.wind.WindMath;
import com.macatmovie.livingwind.mixin.SwingingLanternsExposureAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Wind-only bridge for Swinging Lanterns 2.0.4 (both Forge and NeoForge).
 *
 * The Swinging Lanterns renderer computes a persistent ExposureState for each lantern
 * before feeding it into both procedural and XPBD physics modes. Adjust only the wind
 * input of that state, never physics velocity, collision impulses or Create frame data.
 * This class has no direct references to optional Swinging Lanterns classes.
 */
public final class SwingingLanternsWindCompat {
    private SwingingLanternsWindCompat() {}

    public static void apply(Level level, BlockPos pos, Object exposureState) {
        if (!ClientWindConfig.SWINGING_LANTERNS_COMPAT.get()
                || !ClientWindState.hasServerState() || exposureState == null) return;

        // VirtualRenderWorld/SubLevel on Create moving contraptions uses local coordinates
        // rather than real world coordinates. Leave its established inertial/Create forces
        // and native wind behavior untouched until a world-coordinate bridge is available.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || level != minecraft.level) return;

        SwingingLanternsExposureAccess state = (SwingingLanternsExposureAccess) exposureState;
        float strength = Math.max(0.0f, ClientWindState.altitudeStrength());
        float multiplier = ClientWindConfig.SWINGING_LANTERNS_WIND_MULTIPLIER.get().floatValue();

        // Swinging Lanterns' own weather/open-space sampler may report an exposure
        // far below one even in a reasonably open area. Taking min(native, ours)
        // kept clamping Living Wind to almost nothing outdoors. Let Living Wind's
        // block-aware shelter survey determine indoor shielding; keep a SMALL
        // amount of native exposure as secondary outdoor variation.
        float ownExposure = clamp(state.livingwind$getWindExposure(), 0.0f, 1.0f);
        float livingWindExposure = WindShelter.exposureForStaticAnimation(pos);
        float exposure = livingWindExposure * (0.88f + 0.12f * ownExposure);

        // Respond progressively to actual wind, not just the binary weather state.
        // Avoid overdriving the existing physics solver: the increased native XPBD
        // wind target remains capped to a safe value below its 38-degree envelope.
        float gain = 0.72f + 0.40f * Math.min(strength, 2.0f);
        float windFactor = clamp(strength / 1.25f * gain * multiplier * exposure, 0.0f, 3.0f);
        state.livingwind$setWindExposure(windFactor);

        // Drive a stronger steady directional target. This is not a separate
        // artificial render rotation: chain and body still follow their native
        // physics response, collision impulses and motion constraints.
        float leanProgress = clamp((windFactor - 0.12f) / 1.50f, 0.0f, 1.0f);
        float directionalLean = 9.0f * leanProgress;
        state.livingwind$setLeanFactor(directionalLean);

        // Weather/gust contributions are already represented in Living Wind's strength.
        // The other mod's own weatherBoost would otherwise double-count storms.
        state.livingwind$setWeatherBoost(1.0f);

        // Swinging Lanterns maps its windDirX/Z fields to an X/Z *rotation*
        // with reversed signs: a positive Z input produces positive X rotation,
        // which moves a hanging lantern's lower tip toward -Z; positive X input
        // produces negative Z rotation, moving the tip toward -X. Thus the
        // wind-vector inputs need negating to yield a real DOWNWIND displacement.
        // This applies to both its procedural animation and physics wind target.
        float degrees = ClientWindState.directionDegrees();
        state.livingwind$setWindDirX(-WindMath.windX(1.0f, degrees));
        state.livingwind$setWindDirZ(-WindMath.windZ(1.0f, degrees));

    }

    /** Keep extra solver range strictly scoped to enabled Living Wind integration. */
    public static boolean extendsPhysicsWindRange() {
        return ClientWindConfig.SWINGING_LANTERNS_COMPAT.get() && ClientWindState.hasServerState();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
