package com.macatmovie.livingwind.compat;

import net.minecraft.server.level.ServerLevel;

/** Implemented only on Small Ships' optional WindManager target via Mixin. */
public interface SmallShipsLevelAccess {
    void livingwind$setServerLevel(ServerLevel level);
}
