package com.macatmovie.livingwind.block;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.block.entity.WindmillBlockEntity;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Small farm-style windmill: decorative or analog-redstone wind-speed meter (0..15). */
public final class WindmillBlock extends Block implements EntityBlock {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    /** 0=dark, 1=center, 2=near four, 3=middle four, 4=all markings. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    private static final VoxelShape SHAPE = Shapes.or(
        box(3, 0, 3, 13, 6, 13),
        box(5, 6, 5, 11, 21, 11)
    );
    private static final VoxelShape REDSTONE_SHAPE = Shapes.or(
        box(0, 0, 0, 16, 2, 16),
        box(3, 2, 3, 13, 6, 13),
        box(5, 6, 5, 11, 21, 11)
    );
    private final boolean redstone;

    public WindmillBlock(BlockBehaviour.Properties properties, boolean redstone) {
        super(properties);
        this.redstone = redstone;
        registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(STAGE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Both variants share the property so the same class can serve either registry entry.
        builder.add(POWER, STAGE);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WindmillBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return redstone ? REDSTONE_SHAPE : SHAPE;
    }
    @Override public boolean isSignalSource(BlockState state) { return redstone; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return redstone && direction.getAxis().isHorizontal() ? state.getValue(POWER) : 0;
    }
    @Override public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return redstone && direction != null && direction.getAxis().isHorizontal();
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!redstone || level.isClientSide || type != ModBlockEntities.WINDMILL.get()) return null;
        return (world, blockPos, blockState, be) -> {
            if (be instanceof WindmillBlockEntity) tickRedstone(world, blockPos, blockState);
        };
    }

    private static void tickRedstone(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel level)) return;
        // Individual positions are staggered to avoid thousands of simultaneous checks.
        if (Math.floorMod(level.getGameTime() + (long)pos.hashCode(), 10L) != 0L) return;
        float strength = WindApi.getAt(level, pos).strength() * shelter(level, pos);
        float raw = Mth.clamp(strength / 2.0f, 0.0f, 1.0f) * 15.0f;
        int oldPower = state.getValue(POWER);
        int newPower = Mth.clamp(Math.round(raw), 0, 15);
        // Keep the existing small signal hysteresis; visual tiers are independent of POWER.
        if (newPower != oldPower && Math.abs(raw - oldPower) < 0.62f) newPower = oldPower;
        int newStage = stageForStrength(strength);
        if (newPower == oldPower && newStage == state.getValue(STAGE)) return;

        BlockState nextState = state.setValue(POWER, newPower).setValue(STAGE, newStage);
        level.setBlock(pos, nextState, Block.UPDATE_ALL); // sends both to observing clients
        if (newPower != oldPower) {
            // Avoid triggering extra redstone physics when only a glowing mark changes.
            for (Direction dir : Direction.Plane.HORIZONTAL) level.updateNeighborsAt(pos.relative(dir), state.getBlock());
        }
    }

    /** Four cumulative illumination bands, based on effective local wind (0..2). */
    public static int stageForStrength(float strength) {
        if (strength < 0.1f) return 0;
        if (strength < 0.5f) return 1;
        if (strength < 1.0f) return 2;
        if (strength < 1.5f) return 3;
        return 4;
    }

    /** Bounded server-side exposure estimate. Client propellers use the full cached shelter survey. */
    private static float shelter(ServerLevel level, BlockPos pos) {
        BlockPos center = pos.above(1);
        if (level.canSeeSky(center)) return 1.0f;
        int exposed = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= 3; distance++) {
                BlockPos candidate = center.relative(dir, distance);
                if (!level.hasChunkAt(candidate)) break;
                if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) break;
                if (level.canSeeSky(candidate)) { exposed++; break; }
            }
        }
        return exposed >= 3 ? 0.60f : exposed > 0 ? 0.25f : 0.0f;
    }
}
