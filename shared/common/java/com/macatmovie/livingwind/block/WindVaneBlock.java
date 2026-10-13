package com.macatmovie.livingwind.block;

import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative vane: the fixed compass base is a normal block model; the pointer rotates in its BER. */
public final class WindVaneBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

    public WindVaneBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindVaneBlockEntity(pos, state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
