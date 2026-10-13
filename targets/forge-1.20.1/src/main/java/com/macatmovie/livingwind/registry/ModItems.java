package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LivingWind.MOD_ID);
    public static final RegistryObject<BlockItem> WIND_VANE = ITEMS.register("wind_vane", () ->
        new BlockItem(ModBlocks.WIND_VANE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> BIRD_WIND_VANE = ITEMS.register("bird_wind_vane", () ->
        new BlockItem(ModBlocks.BIRD_WIND_VANE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> REDSTONE_WIND_VANE = ITEMS.register("redstone_wind_vane", () ->
        new BlockItem(ModBlocks.REDSTONE_WIND_VANE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> WINDMILL = ITEMS.register("windmill", () ->
        new com.macatmovie.livingwind.item.WindmillBlockItem(ModBlocks.WINDMILL.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> REDSTONE_WINDMILL = ITEMS.register("redstone_windmill", () ->
        new com.macatmovie.livingwind.item.WindmillBlockItem(ModBlocks.REDSTONE_WINDMILL.get(), new Item.Properties()));
    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
