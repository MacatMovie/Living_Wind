package com.macatmovie.livingwind.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Server-only Sable plot lookup for redstone wind instruments. */
public final class SableServerInstrumentResolver {
    private SableServerInstrumentResolver() {}

    public static void install() { InstrumentSpatialContext.setServerResolver(SableServerInstrumentResolver::at); }

    private static InstrumentSpatialContext.Frame at(Level world, BlockPos pos, float unusedPartial) {
        if (!(world instanceof ServerLevel level)) return InstrumentSpatialContext.Frame.stationary(pos);
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return InstrumentSpatialContext.Frame.stationary(pos);
        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        if (plot == null || plot.getSubLevel() == null) return InstrumentSpatialContext.Frame.stationary(pos);
        return SableInstrumentFrame.from(plot.getSubLevel().logicalPose(), pos);
    }
}
