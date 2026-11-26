package com.macatmovie.livingwind.client;

/** Optional mod presence is stable for the lifetime of the running instance. */
final class CompatAvailability {
    private CompatAvailability() {}
    static final boolean AMBIENT_SOUNDS = loaded("ambientsounds");
    static final boolean DYNAMIC_SURROUNDINGS = loaded("dsurround");
    static final boolean AMBI = loaded("ambimod");
    private static boolean loaded(String id) { return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id); }
}
