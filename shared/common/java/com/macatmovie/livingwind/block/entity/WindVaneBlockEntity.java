package com.macatmovie.livingwind.block.entity;

import com.macatmovie.livingwind.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Transient visual rotation state; redstone direction lives in the synced blockstate. */
public final class WindVaneBlockEntity extends BlockEntity {
    private float rotationDegrees;
    private float rotationVelocity;
    private double lastRenderTick = Double.NaN;
    private boolean initialized;
    private float lastSubLevelReference;
    private boolean subLevelReferenceValid;

    public WindVaneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_VANE.get(), pos, state);
    }

    public float getRotationDegrees() { return rotationDegrees; }
    public float getRotationVelocity() { return rotationVelocity; }
    public void setRotationVelocity(float value) { rotationVelocity = value; }
    public void setRotationDegrees(float angle) { rotationDegrees = angle; }
    public double getLastRenderTick() { return lastRenderTick; }
    public void setLastRenderTick(double tick) { lastRenderTick = tick; }
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
    public void setInitialized(boolean value) { initialized = value; }
}
