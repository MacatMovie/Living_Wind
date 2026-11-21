package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.compat.TumbleweedWindBridge;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Tumbleweed 0.5.5 (1.20.1 Forge/Fabric) integration. Tumbleweed's
 * original tick consumes synchronized custom-wind fields BEFORE applying its
 * bounce, friction, rotation, water and collision handling. We simply feed
 * those fields using Living Wind's server wind; no duplicate motion code.
 *
 * All custom wind written here is transient. Explicit user/other-mod custom
 * winds remain authoritative, and our values aren't written to entity NBT.
 */
@Pseudo
@Mixin(targets = "net.konwboy.tumbleweed.common.EntityTumbleweed", remap = false)
public abstract class TumbleweedWindMixin {
    @Shadow(remap = false) @Final private static EntityDataAccessor<Boolean> CUSTOM_WIND_ENABLED;
    @Shadow(remap = false) @Final private static EntityDataAccessor<Float> CUSTOM_WIND_X;
    @Shadow(remap = false) @Final private static EntityDataAccessor<Float> CUSTOM_WIND_Z;
    @Shadow(remap = false) private double windMod;

    @Unique private boolean livingwind$ownsWind;
    @Unique private static final int livingwind$UPDATE_INTERVAL = 10;

    // Forge's production name is m_8119_; Fabric's is method_5773.
    @Inject(method = {"tick", "m_8119_", "method_5773"}, at = @At("HEAD"), remap = false, require = 0)
    private void livingwind$setTumbleweedWind(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide) return; // SynchedEntityData drives client-side prediction.

        boolean enabled = WindConfig.TUMBLEWEED_WIND_ENABLED.get();
        double multiplier = WindConfig.TUMBLEWEED_WIND_MULTIPLIER.get();
        if (!enabled || multiplier <= 0.0 || !TumbleweedWindBridge.allowed(self)) {
            livingwind$restoreOriginal(self);
            return;
        }

        var data = self.getEntityData();
        // /summon or another mod may supply custom wind. Don't replace it.
        if (!livingwind$ownsWind && data.get(CUSTOM_WIND_ENABLED)) return;
        if (livingwind$ownsWind && self.tickCount % livingwind$UPDATE_INTERVAL != 0) return;

        Vec3 wind = TumbleweedWindBridge.velocity(self, windMod, multiplier);
        if (!Double.isFinite(wind.x) || !Double.isFinite(wind.z)) return;
        float x = (float) wind.x;
        float z = (float) wind.z;
        livingwind$ownsWind = true;
        if (!data.get(CUSTOM_WIND_ENABLED)) data.set(CUSTOM_WIND_ENABLED, true);
        if (Math.abs(data.get(CUSTOM_WIND_X) - x) > 0.001f) data.set(CUSTOM_WIND_X, x);
        if (Math.abs(data.get(CUSTOM_WIND_Z) - z) > 0.001f) data.set(CUSTOM_WIND_Z, z);
    }

    @Unique
    private void livingwind$restoreOriginal(Entity self) {
        if (!livingwind$ownsWind) return;
        var data = self.getEntityData();
        data.set(CUSTOM_WIND_ENABLED, false);
        livingwind$ownsWind = false;
    }

    @Inject(method = {"addAdditionalSaveData", "m_7378_", "method_5652"}, at = @At("TAIL"), remap = false, require = 0)
    private void livingwind$avoidPersistingOverride(CompoundTag nbt, CallbackInfo ci) {
        if (!livingwind$ownsWind) return;
        nbt.putBoolean("CustomWindEnabled", false);
        nbt.putDouble("CustomWindX", 0.0);
        nbt.putDouble("CustomWindZ", 0.0);
    }
}
