package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.compat.CreateWindmillScaling;
import com.macatmovie.livingwind.config.WindConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional Create windmill compatibility; safe when Create is not installed.
 *
 * The earlier prototype relied on the *third bytecode RETURN* in Create's
 * getGeneratedSpeed(). That was too fragile: minor Create changes can move
 * the return sites without breaking the method's public contract, leaving the
 * integration silently inactive (the former require=0 also concealed it).
 *
 * This implementation intercepts every return but only changes the returned
 * speed when our server windmill ticker has computed a valid applied RPM.
 * While collecting the VANILLA live-sail speed, the return override is bypassed;
 * consequently the calculation can never compound its own previous output.
 *
 * Server kinetic refresh uses Create's updateGeneratedRotation() on bounded,
 * staggered intervals. Create owns network/stress recalculation and sync.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity", remap = false)
public abstract class CreateWindmillBearingMixin {
    // Server tick runs on the logical server thread. Primitive flags avoid
    // static initializers in a Mixin target class.
    @Unique private static boolean livingwind$inspectionWarned;

    @Shadow(remap = false) protected float lastGeneratedSpeed;
    @Shadow(remap = false) public abstract float getGeneratedSpeed();
    @Shadow(remap = false) public abstract void updateGeneratedRotation();

    @Unique private boolean livingwind$bypassWindMultiplier;
    @Unique private float livingwind$appliedRpm;
    @Unique private long livingwind$lastCheck = Long.MIN_VALUE;

    /**
     * Matches all return paths on all supported Create versions. `raw` is the
     * original Create result: 0 when stopped, cached RPM if sails aren't loaded,
     * or the unmodified sail calculation when assembled.
     */
    @Inject(method = "getGeneratedSpeed()F", at = @At("RETURN"),
            cancellable = true, remap = false, require = 1)
    private void livingwind$windDrivenSpeed(CallbackInfoReturnable<Float> cir) {
        if (livingwind$bypassWindMultiplier || !WindConfig.CREATE_WINDMILLS_ENABLED.get()) return;
        float raw = cir.getReturnValue();
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        Level level = blockEntity.getLevel();
        if (Math.abs(raw) < 0.0001f) {
            if (level != null && !level.isClientSide()) livingwind$appliedRpm = 0f;
            return;
        }
        if (level == null) return;
        if (level.isClientSide()) {
            // The client does NOT simulate its own gust-driven generator. Create's
            // LastGenerated field is synchronized from the authoritative server.
            if (Math.abs(lastGeneratedSpeed) > 0.0001f)
                cir.setReturnValue(lastGeneratedSpeed);
            return;
        }
        if (livingwind$appliedRpm > 0f)
            cir.setReturnValue(Math.copySign(livingwind$appliedRpm, raw));
    }

    // Create 6.0.10 tick() has early returns on ordinary ticks when there is no
    // queued reassembly. TAIL only hits its final return (the reassembly path),
    // leaving running windmills unmodified. RETURN covers all normal and early
    // exits, once per tick, after Create has executed its own tick logic.
    @Inject(method = "tick()V", at = @At("RETURN"), remap = false, require = 1)
    private void livingwind$refreshWindmill(CallbackInfo ci) {
        BlockEntity tile = (BlockEntity) (Object) this;
        if (!(tile.getLevel() instanceof ServerLevel server)) return;

        long tick = server.getGameTime();
        int interval = Math.max(20, WindConfig.CREATE_WINDMILL_UPDATE_TICKS.get());
        BlockPos pos = tile.getBlockPos();
        if (Math.floorMod(tick + pos.asLong(), interval) != 0 || livingwind$lastCheck == tick) return;
        livingwind$lastCheck = tick;

        // If disabled after running, restore Create's vanilla kinetic RPM once.
        if (!WindConfig.CREATE_WINDMILLS_ENABLED.get()) {
            if (livingwind$appliedRpm != 0f) {
                livingwind$appliedRpm = 0f;
                updateGeneratedRotation();
            }
            return;
        }

        // This is a public Create bearing method, inherited from its parent.
        // Checking for an assembled contraption prevents multiplying Create's
        // previously cached LastGenerated speed when its sails are unloaded.
        if (!livingwind$hasAssembledContraption()) {
            livingwind$appliedRpm = 0f;
            return;
        }

        // Temporarily bypass our own return injection. This always reads the
        // ORIGINAL Create sail-count based RPM, never our previously scaled RPM.
        float vanilla;
        livingwind$bypassWindMultiplier = true;
        try {
            vanilla = getGeneratedSpeed();
        } finally {
            livingwind$bypassWindMultiplier = false;
        }
        float magnitude = Math.abs(vanilla);
        if (magnitude < 0.0001f) {
            livingwind$appliedRpm = 0f;
            return;
        }

        float wind = WindApi.getAt(server, pos).strength();
        float wanted = CreateWindmillScaling.targetRpm(magnitude, wind,
            WindConfig.CREATE_WINDMILL_MIN_MULTIPLIER.get().floatValue(),
            WindConfig.CREATE_WINDMILL_MAX_MULTIPLIER.get().floatValue(),
            WindConfig.CREATE_WINDMILL_RPM_STEP.get().floatValue());
        float current = livingwind$appliedRpm > 0f ? livingwind$appliedRpm : magnitude;
        float next = CreateWindmillScaling.approachRpm(current, wanted,
            WindConfig.CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE.get().floatValue(),
            WindConfig.CREATE_WINDMILL_MIN_RPM_CHANGE.get().floatValue());
        if (Math.abs(next - current) < 0.001f) return;

        livingwind$appliedRpm = next;
        updateGeneratedRotation(); // Actual kinetic RPM + stress + client packet.

    }

    @Unique
    private boolean livingwind$hasAssembledContraption() {
        try {
            // Public method on Create's MechanicalBearingBlockEntity superclass.
            // Reflection is used only once per configured interval, and avoids
            // linking Create classes when the compatibility mod isn't installed.
            Method getContraption = this.getClass().getMethod("getMovedContraption");
            return getContraption.invoke(this) != null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            if (!livingwind$inspectionWarned) {
                livingwind$inspectionWarned = true;
                LoggerFactory.getLogger("Living Wind / Create").warn("Could not inspect Create windmill assembly; wind integration skipped", e);
            }
            return false;
        }
    }
}
