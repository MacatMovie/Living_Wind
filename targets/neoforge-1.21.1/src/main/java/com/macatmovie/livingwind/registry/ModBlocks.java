package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.WindVaneBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LivingWind.MOD_ID);
    public static final DeferredHolder<Block, WindVaneBlock> WIND_VANE = BLOCKS.register("wind_vane", () ->
        new WindVaneBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Block, WindVaneBlock> BIRD_WIND_VANE = BLOCKS.register("bird_wind_vane", () ->
        new WindVaneBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.METAL).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredHolder<Block, com.macatmovie.livingwind.block.RedstoneWindVaneBlock> REDSTONE_WIND_VANE = BLOCKS.register("redstone_wind_vane", () ->
        new com.macatmovie.livingwind.block.RedstoneWindVaneBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Block, com.macatmovie.livingwind.block.WindmillBlock> WINDMILL = BLOCKS.register("windmill", () ->
        new com.macatmovie.livingwind.block.WindmillBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion(), false));
    public static final DeferredHolder<Block, com.macatmovie.livingwind.block.WindmillBlock> REDSTONE_WINDMILL = BLOCKS.register("redstone_windmill", () ->
        new com.macatmovie.livingwind.block.WindmillBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion(), true));
    private ModBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
