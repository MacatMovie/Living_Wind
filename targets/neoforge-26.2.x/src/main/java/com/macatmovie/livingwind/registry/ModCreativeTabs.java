package com.macatmovie.livingwind.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModItems.WIND_VANE.get());
            event.accept(ModItems.REDSTONE_WIND_VANE.get());
            event.accept(ModItems.WINDMILL.get());
            event.accept(ModItems.REDSTONE_WINDMILL.get());
        }
    }
}
