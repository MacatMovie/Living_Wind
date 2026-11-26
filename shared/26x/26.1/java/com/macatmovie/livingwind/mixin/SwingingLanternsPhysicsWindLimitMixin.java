package com.macatmovie.livingwind.mixin;

import com.macatmovie.livingwind.client.compat.SwingingLanternsWindCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Swinging Lanterns 2.0.4 physically clips its wind target at +/-24 degrees
 * in two places: when storing the target and when converting it to XPBD gravity.
 * Both limits must change together or increasing Living Wind's lean factor will
 * stop having an effect. 34 degrees stays inside the solver's existing 38-degree
 * horizontal-acceleration/pendulum envelope. Only the WIND target is affected;
 * collision and projectile impulses, the physics solver, and Create handling
 * remain owned by Swinging Lanterns.
 *
 * Pseudo + require=0: safe if the optional mod or method is absent.
 */
@Pseudo
@Mixin(targets = "dev.rilith.swinginglanterns.client.render.LanternCollisionPhysics", remap = false)
public abstract class SwingingLanternsPhysicsWindLimitMixin {
    @ModifyConstant(method = {"setWindTarget", "beginXpbdStep"},
        constant = @Constant(floatValue = 24.0f), require = 0, remap = false)
    private static float livingwind$windTiltMax(float original) {
        return SwingingLanternsWindCompat.extendsPhysicsWindRange() ? 34.0f : original;
    }

    @ModifyConstant(method = {"setWindTarget", "beginXpbdStep"},
        constant = @Constant(floatValue = -24.0f), require = 0, remap = false)
    private static float livingwind$windTiltMin(float original) {
        return SwingingLanternsWindCompat.extendsPhysicsWindRange() ? -34.0f : original;
    }
}
