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

    public WindVaneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_VANE.get(), pos, state);
    }

    public float getRotationDegrees() { return rotationDegrees; }
    public float getRotationVelocity() { return rotationVelocity; }
    public void setRotationVelocity(float value) { rotationVelocity = value; }
    public void setRotationDegrees(float angle) { rotationDegrees = angle; }
    public double getLastRenderTick() { return lastRenderTick; }
    public void setLastRenderTick(double tick) { lastRenderTick = tick; }
    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean value) { initialized = value; }
}
