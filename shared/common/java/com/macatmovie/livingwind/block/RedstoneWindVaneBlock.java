package com.macatmovie.livingwind.block;

import com.macatmovie.livingwind.api.WindApi;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.wind.WindMath;
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

/** An eight-direction, server-synced redstone wind vane. 0=S,1=SW,2=W,3=NW,4=N,5=NE,6=E,7=SE. */
public final class RedstoneWindVaneBlock extends Block implements EntityBlock {
    public static final IntegerProperty DIRECTION = IntegerProperty.create("direction", 0, 7);
    private static final VoxelShape SHAPE = Shapes.or(
        box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
        box(7.0, 2.0, 7.0, 9.0, 16.0, 9.0)
    );

    public RedstoneWindVaneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DIRECTION, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRECTION);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WindVaneBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public boolean isSignalSource(BlockState state) { return true; }

    /** The redstone outputs and the client-side indicator deliberately read ONE authoritative blockstate. */
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        int mask = switch (state.getValue(DIRECTION)) {
            case 0 -> 4;       // S
            case 1 -> 4 | 8;   // SW
            case 2 -> 8;       // W
            case 3 -> 1 | 8;   // NW
            case 4 -> 1;       // N
            case 5 -> 1 | 2;   // NE
            case 6 -> 2;       // E
            default -> 4 | 2;  // SE
        };
        // Minecraft queries getSignal with the opposite of the physical output face.
        // Mirror the query, not the blockstate, so emissive indicator and actual output agree.
        int bit = switch (dir.getOpposite()) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 8;
            default -> 0;
        };
        return (mask & bit) != 0 ? 15 : 0;
    }

    @Override public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos,
                                                @Nullable Direction direction) {
        return direction != null && direction.getAxis().isHorizontal();
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.WIND_VANE.get()) return null;
        return (world, pos, blockState, be) -> {
            if (be instanceof WindVaneBlockEntity vane) serverTick(world, pos, blockState, vane);
        };
    }

    private static void serverTick(Level world, BlockPos pos, BlockState state, WindVaneBlockEntity vane) {
        if (!(world instanceof ServerLevel level) || !(state.getBlock() instanceof RedstoneWindVaneBlock)) return;
        // Check direction 2x per second, not every tick or frame; no extra networking needed.
        if (level.getGameTime() % 10L != 0L) return;
        var wind = WindApi.getAt(level, pos);
        if (wind.strength() <= 0.025f || shelterExposure(level, pos) <= 0.05f) return;

        // Living Wind's direction means wind TO; indicator points the same direction as its flow.
        float bearing = WindMath.normalizeDegrees(wind.directionDegrees());
        int old = state.getValue(DIRECTION);
        int next = (Mth.floor((bearing + 22.5f) / 45.0f) & 7);
        if (next == old) return;
        // 4 degree hysteresis around the 45 degree sector boundaries to prevent flicker.
        float oldBearing = old * 45.0f;
        if (Math.abs(Mth.wrapDegrees(bearing - oldBearing)) <= 26.5f) return;

        // A blockstate update is automatically saved, sent to clients, and causes model updates.
        world.setBlock(pos, state.setValue(DIRECTION, next), Block.UPDATE_ALL);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            world.updateNeighborsAt(pos.relative(side), state.getBlock());
        }
    }

    /** Small, server-side version of the existing shelter heuristic, checked only 2x per second. */
    private static float shelterExposure(ServerLevel level, BlockPos pos) {
        BlockPos center = pos.above();
        if (level.canSeeSky(center)) return 1.0f;
        int exposed = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            boolean clear = true;
            for (int step = 1; step <= 3; step++) {
                BlockPos check = center.relative(direction, step);
                if (!level.hasChunkAt(check)) { clear = false; break; }
                var block = level.getBlockState(check);
                if (!block.getCollisionShape(level, check).isEmpty()) { clear = false; break; }
                if (level.canSeeSky(check)) { exposed++; break; }
            }
        }
        return exposed >= 3 ? 0.55f : exposed >= 1 ? 0.25f : 0.0f;
    }
}
