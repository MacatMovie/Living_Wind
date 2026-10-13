package com.macatmovie.livingwind.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.macatmovie.livingwind.LivingWind;

@Mod.EventBusSubscriber(modid = LivingWind.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private ModCreativeTabs() {}
    @SubscribeEvent public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModItems.WIND_VANE.get());
            event.accept(ModItems.REDSTONE_WIND_VANE.get());
            event.accept(ModItems.WINDMILL.get());
            event.accept(ModItems.REDSTONE_WINDMILL.get());
        }
    }
}
