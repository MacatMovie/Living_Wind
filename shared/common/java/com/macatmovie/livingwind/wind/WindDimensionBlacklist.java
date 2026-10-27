package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Server-side dimension wind exclusion. No world scans or per-particle checks. */
public final class WindDimensionBlacklist {
    private WindDimensionBlacklist() {}

    public static boolean contains(ServerLevel level) {
        return WindDimensionSettings.mode(level) == WindDimensionSettings.Mode.DISABLED;
    }

    /** Return a fully calm state without changing the saved atmospheric simulation. */
    public static WindSnapshot calm(long gameTime) {
        return new WindSnapshot(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, false, gameTime);
    }

    public static boolean validId(String id) {
        if (id == null || id.isBlank()) return false;
        ResourceLocation parsed = ResourceLocation.tryParse(id.trim().toLowerCase(Locale.ROOT));
        return parsed != null && parsed.toString().equals(id.trim().toLowerCase(Locale.ROOT));
    }
}
