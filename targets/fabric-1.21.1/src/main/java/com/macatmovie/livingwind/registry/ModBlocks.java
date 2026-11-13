package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.RedstoneWindVaneBlock;
import com.macatmovie.livingwind.block.WindVaneBlock;
import com.macatmovie.livingwind.block.WindmillBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final RegistryHandle<WindVaneBlock> WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<WindVaneBlock> BIRD_WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<RedstoneWindVaneBlock> REDSTONE_WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<WindmillBlock> WINDMILL = new RegistryHandle<>();
    public static final RegistryHandle<WindmillBlock> REDSTONE_WINDMILL = new RegistryHandle<>();
    private ModBlocks() {}
    public static void register() {
        WIND_VANE.set(Registry.register(BuiltInRegistries.BLOCK, LivingWind.id("wind_vane"),
            new WindVaneBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.METAL).noOcclusion())));
        BIRD_WIND_VANE.set(Registry.register(BuiltInRegistries.BLOCK, LivingWind.id("bird_wind_vane"),
            new WindVaneBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.METAL).noOcclusion())));
        REDSTONE_WIND_VANE.set(Registry.register(BuiltInRegistries.BLOCK, LivingWind.id("redstone_wind_vane"),
            new RedstoneWindVaneBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion())));
        WINDMILL.set(Registry.register(BuiltInRegistries.BLOCK, LivingWind.id("windmill"),
            new WindmillBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion(), false)));
        REDSTONE_WINDMILL.set(Registry.register(BuiltInRegistries.BLOCK, LivingWind.id("redstone_windmill"),
            new WindmillBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion(), true)));
    }
}
