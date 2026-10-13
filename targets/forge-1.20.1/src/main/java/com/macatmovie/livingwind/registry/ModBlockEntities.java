package com.macatmovie.livingwind.registry;

import com.macatmovie.livingwind.LivingWind;
import com.macatmovie.livingwind.block.entity.WindVaneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LivingWind.MOD_ID);
    public static final RegistryObject<BlockEntityType<WindVaneBlockEntity>> WIND_VANE =
        TYPES.register("wind_vane", () -> BlockEntityType.Builder.of(WindVaneBlockEntity::new, ModBlocks.WIND_VANE.get(), ModBlocks.BIRD_WIND_VANE.get(), ModBlocks.REDSTONE_WIND_VANE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.macatmovie.livingwind.block.entity.WindmillBlockEntity>> WINDMILL =
        TYPES.register("windmill", () -> BlockEntityType.Builder.of(com.macatmovie.livingwind.block.entity.WindmillBlockEntity::new,
            ModBlocks.WINDMILL.get(), ModBlocks.REDSTONE_WINDMILL.get()).build(null));
    private ModBlockEntities() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
