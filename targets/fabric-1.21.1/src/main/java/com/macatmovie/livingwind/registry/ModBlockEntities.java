package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import com.macatmovie.livingwind.block.entity.WindmillBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final RegistryHandle<BlockEntityType<WindVaneBlockEntity>> WIND_VANE = new RegistryHandle<>();
    public static final RegistryHandle<BlockEntityType<WindmillBlockEntity>> WINDMILL = new RegistryHandle<>();
    private ModBlockEntities() {}
    public static void register() {
        WIND_VANE.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, LivingWind.id("wind_vane"),
            BlockEntityType.Builder.of(WindVaneBlockEntity::new, ModBlocks.WIND_VANE.get(),
                ModBlocks.BIRD_WIND_VANE.get(), ModBlocks.REDSTONE_WIND_VANE.get()).build(null)));
        WINDMILL.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, LivingWind.id("windmill"),
            BlockEntityType.Builder.of(WindmillBlockEntity::new, ModBlocks.WINDMILL.get(),
                ModBlocks.REDSTONE_WINDMILL.get()).build(null)));
    }
}
