package com.macatmovie.livingwind.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Uses the same interpolated pose that Sable renders, avoiding frame-by-frame
 * orientation mismatch when a contraption turns. */
public final class SableClientInstrumentResolver {
    private SableClientInstrumentResolver() {}

    public static void install() { InstrumentSpatialContext.setClientResolver(SableClientInstrumentResolver::at); }

    private static InstrumentSpatialContext.Frame at(Level world, BlockPos pos, float partialTick) {
        if (!(world instanceof ClientLevel level)) return InstrumentSpatialContext.Frame.stationary(pos);
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return InstrumentSpatialContext.Frame.stationary(pos);
        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        if (plot == null || !(plot.getSubLevel() instanceof ClientSubLevel subLevel)) {
            return InstrumentSpatialContext.Frame.stationary(pos);
        }
        return SableInstrumentFrame.from(subLevel.renderPose(partialTick), pos);
    }
}
