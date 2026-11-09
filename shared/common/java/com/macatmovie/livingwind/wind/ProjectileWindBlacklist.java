package com.macatmovie.livingwind.wind;

import com.macatmovie.livingwind.config.WindConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Called exclusively inside the enabled, server-side projectile wind branch.
 * Config is combined into a hash set only when the config lists change, rather than
 * scanning user entries or allocating collections for every projectile tick.
 */
public final class ProjectileWindBlacklist {
    private static volatile List<? extends String> lastAdded;
    private static volatile List<? extends String> lastExceptions;
    private static volatile Set<String> excluded = Set.copyOf(BuiltinProjectileBlacklist.IDS);

    private ProjectileWindBlacklist() {}

    public static boolean excludes(Entity entity) {
        // The caller checks entityWind.enabled and entityWind.projectiles first.
        List<? extends String> added = WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST.get();
        List<? extends String> exceptions = WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS.get();
        if (added != lastAdded || exceptions != lastExceptions) refresh(added, exceptions);
        return excluded.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    /** Explicitly invalidated on GUI save, and lazily after external TOML reload. */
    public static synchronized void refresh() {
        refresh(WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST.get(),
            WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS.get());
    }

    private static synchronized void refresh(List<? extends String> added, List<? extends String> exceptions) {
        Set<String> updated = new HashSet<>(BuiltinProjectileBlacklist.IDS);
        for (String id : exceptions) if (id != null) updated.remove(id.trim().toLowerCase(Locale.ROOT));
        for (String id : added) if (id != null) updated.add(id.trim().toLowerCase(Locale.ROOT));
        excluded = Set.copyOf(updated);
        lastAdded = added;
        lastExceptions = exceptions;
    }
}
