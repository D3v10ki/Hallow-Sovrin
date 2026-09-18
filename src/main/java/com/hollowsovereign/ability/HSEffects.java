package com.hollowsovereign.ability;

import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lightweight per-world scheduler for multi-tick cinematic effects (vortexes,
 * travelling orbs, domains). Ticked from {@code ServerTickEvents.END_WORLD_TICK}.
 */
public final class HSEffects {
    private HSEffects() {}

    /** One running effect. Return {@code true} to keep going, {@code false} when finished. */
    public interface Effect {
        boolean tick(ServerWorld world);
    }

    private static final Map<ServerWorld, List<Effect>> ACTIVE = new WeakHashMap<>();

    public static void add(ServerWorld world, Effect effect) {
        ACTIVE.computeIfAbsent(world, w -> new ArrayList<>()).add(effect);
    }

    public static void tick(ServerWorld world) {
        List<Effect> list = ACTIVE.get(world);
        if (list == null || list.isEmpty()) return;
        list.removeIf(e -> !safeTick(e, world));
    }

    private static boolean safeTick(Effect e, ServerWorld world) {
        try {
            return e.tick(world);
        } catch (Exception ex) {
            return false; // never let a broken effect spam the log or crash the tick
        }
    }
}
