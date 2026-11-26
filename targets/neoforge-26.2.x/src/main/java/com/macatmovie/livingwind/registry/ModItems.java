package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LivingWind.MOD_ID);
    public static final DeferredHolder<Item, BlockItem> WIND_VANE = ITEMS.register("wind_vane", id ->
        new BlockItem(ModBlocks.WIND_VANE.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))));
    public static final DeferredHolder<Item, BlockItem> BIRD_WIND_VANE = ITEMS.register("bird_wind_vane", id ->
        new BlockItem(ModBlocks.BIRD_WIND_VANE.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))));
    public static final DeferredHolder<Item, BlockItem> REDSTONE_WIND_VANE = ITEMS.register("redstone_wind_vane", id ->
        new BlockItem(ModBlocks.REDSTONE_WIND_VANE.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))));
    public static final DeferredHolder<Item, BlockItem> WINDMILL = ITEMS.register("windmill", id ->
        new BlockItem(ModBlocks.WINDMILL.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))));
    public static final DeferredHolder<Item, BlockItem> REDSTONE_WINDMILL = ITEMS.register("redstone_windmill", id ->
        new BlockItem(ModBlocks.REDSTONE_WINDMILL.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))));
    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
