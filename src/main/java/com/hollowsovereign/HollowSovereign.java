package com.hollowsovereign;

import com.hollowsovereign.ability.HSEffects;
import com.hollowsovereign.command.HollowCommands;
import com.hollowsovereign.config.ServerConfig;
import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.entity.HSEntities;
import com.hollowsovereign.leveling.Leveling;
import com.hollowsovereign.net.HSNet;
import com.hollowsovereign.particle.HSParticles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HollowSovereign implements ModInitializer {
    public static final String MOD_ID = "hollowsovereign";
    public static final Logger LOGGER = LoggerFactory.getLogger("Hollow Sovereign");

    @Override
    public void onInitialize() {
        ServerConfig.load();
        HSParticles.register();
        HSEntities.register();
        HSNet.registerServerReceivers();

        // Advance cinematic effects each world tick.
        ServerTickEvents.END_WORLD_TICK.register(HSEffects::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
                HollowCommands.register(dispatcher, access, env));

        // Award XP to the killer.
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (entity instanceof ServerPlayerEntity player) {
                Leveling.awardKillXp(player, killed);
            }
        });

        // On join: push current progression to the client, and prompt class select if needed.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            HSNet.syncData(player);
            if (!PlayerData.get(player).hasClass()) {
                HSNet.openClassSelect(player);
            }
        });

        LOGGER.info("Hollow Sovereign initialized.");
    }
}
