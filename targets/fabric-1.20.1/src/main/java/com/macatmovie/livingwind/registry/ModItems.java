package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final RegistryHandle<BlockItem> WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<BlockItem> BIRD_WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<BlockItem> REDSTONE_WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<BlockItem> WINDMILL = new RegistryHandle<>();
    public static final RegistryHandle<BlockItem> REDSTONE_WINDMILL = new RegistryHandle<>();
    private ModItems() {}
    public static void register() {
        WIND_VANE.set(Registry.register(BuiltInRegistries.ITEM, LivingWind.id("wind_vane"), new BlockItem(ModBlocks.WIND_VANE.get(), new Item.Properties())));
        BIRD_WIND_VANE.set(Registry.register(BuiltInRegistries.ITEM, LivingWind.id("bird_wind_vane"), new BlockItem(ModBlocks.BIRD_WIND_VANE.get(), new Item.Properties())));
        REDSTONE_WIND_VANE.set(Registry.register(BuiltInRegistries.ITEM, LivingWind.id("redstone_wind_vane"), new BlockItem(ModBlocks.REDSTONE_WIND_VANE.get(), new Item.Properties())));
        WINDMILL.set(Registry.register(BuiltInRegistries.ITEM, LivingWind.id("windmill"), new BlockItem(ModBlocks.WINDMILL.get(), new Item.Properties())));
        REDSTONE_WINDMILL.set(Registry.register(BuiltInRegistries.ITEM, LivingWind.id("redstone_windmill"), new BlockItem(ModBlocks.REDSTONE_WINDMILL.get(), new Item.Properties())));
    }
}
