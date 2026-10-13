package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LivingWind.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindVaneBlockEntity>> WIND_VANE =
        TYPES.register("wind_vane", () -> BlockEntityType.Builder.of(WindVaneBlockEntity::new, ModBlocks.WIND_VANE.get(), ModBlocks.BIRD_WIND_VANE.get(), ModBlocks.REDSTONE_WIND_VANE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.macatmovie.livingwind.block.entity.WindmillBlockEntity>> WINDMILL =
        TYPES.register("windmill", () -> BlockEntityType.Builder.of(com.macatmovie.livingwind.block.entity.WindmillBlockEntity::new,
            ModBlocks.WINDMILL.get(), ModBlocks.REDSTONE_WINDMILL.get()).build(null));
    private ModBlockEntities() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
