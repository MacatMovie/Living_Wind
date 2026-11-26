package com.macatmovie.livingwind.block.entity;

import com.macatmovie.livingwind.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Renderer-only client animation state; redstone strength is stored in the synchronized blockstate. */
public final class WindmillBlockEntity extends BlockEntity {
    private float yaw;
    private float yawVelocity;
    private float rotorAngle;
    private float rotorSpeed;
    private double lastRenderTick = Double.NaN;
    private boolean initialized;
    private float lastSubLevelReference;
    private boolean subLevelReferenceValid;

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDMILL.get(), pos, state);
    }
    public float getYaw() { return yaw; }
    public void setYaw(float v) { yaw = v; }
    public float getYawVelocity() { return yawVelocity; }
    public void setYawVelocity(float v) { yawVelocity = v; }
    public float getRotorAngle() { return rotorAngle; }
    public void setRotorAngle(float v) { rotorAngle = v; }
    public float getRotorSpeed() { return rotorSpeed; }
    public void setRotorSpeed(float v) { rotorSpeed = v; }
    public double getLastRenderTick() { return lastRenderTick; }
    public void setLastRenderTick(double v) { lastRenderTick = v; }
    /** Last local heading of a fixed world compass reference (0=S).
     * Helps the head counter-rotate instantly when the craft itself turns. */
    public float getLastSubLevelReference() { return lastSubLevelReference; }
    public boolean hasSubLevelReference() { return subLevelReferenceValid; }
    public void recordSubLevelReference(float reference) {
        lastSubLevelReference = reference;
        subLevelReferenceValid = true;
    }
    public void clearSubLevelReference() { subLevelReferenceValid = false; }
    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean v) { initialized = v; }
}
