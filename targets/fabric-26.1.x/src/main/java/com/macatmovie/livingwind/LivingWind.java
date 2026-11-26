package com.macatmovie.livingwind;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.network.WindNetworking;
import com.macatmovie.livingwind.registry.ModBlocks;
import com.macatmovie.livingwind.registry.ModItems;
import com.macatmovie.livingwind.registry.ModBlockEntities;
import com.macatmovie.livingwind.registry.ModParticles;
import com.macatmovie.livingwind.server.ServerWindEvents;
import com.macatmovie.livingwind.server.ServerWindCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

public final class LivingWind implements ModInitializer {
    public static final String MOD_ID = "livingwind";
    @Override public void onInitialize() {
        WindConfig.SPEC.load("livingwind-common-fabric.json");
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModParticles.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(ModItems.WIND_VANE.get());
            entries.accept(ModItems.BIRD_WIND_VANE.get());
            entries.accept(ModItems.REDSTONE_WIND_VANE.get());
            entries.accept(ModItems.WINDMILL.get());
            entries.accept(ModItems.REDSTONE_WINDMILL.get());
        });
        WindNetworking.register();
        ServerTickEvents.END_LEVEL_TICK.register(ServerWindEvents::onLevelTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerWindEvents.onPlayerLogin(handler.player));
        // A PLAY connection remains active through dimension travel and respawning;
        // neither operation fires JOIN. Send an authoritative snapshot immediately
        // so the client never keeps the old dimension's wind while waiting for the
        // periodic server broadcast.
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
            (player, oldWorld, newWorld) -> ServerWindEvents.onPlayerLogin(player));
        ServerPlayerEvents.AFTER_RESPAWN.register(
            (oldPlayer, newPlayer, alive) -> ServerWindEvents.onPlayerLogin(newPlayer));
        // Every dimension join triggers a world sync; tick sync covers dimension travel.
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> ServerWindCommands.register(dispatcher));
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
}
