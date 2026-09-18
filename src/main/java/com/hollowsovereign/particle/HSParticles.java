package com.hollowsovereign.particle;

import com.hollowsovereign.HollowSovereign;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Custom particle types used for Hollow Sovereign's cinematic effects. */
public final class HSParticles {
    private HSParticles() {}

    /** Soft round glow — orbs, cores, general energy. */
    public static final DefaultParticleType VOID_GLOW = FabricParticleTypes.simple();
    /** Bright 4-point sparkle — starfield, highlights. */
    public static final DefaultParticleType VOID_STAR = FabricParticleTypes.simple();
    /** Jagged sliver — space-time cracks / shatter. */
    public static final DefaultParticleType VOID_SHARD = FabricParticleTypes.simple();

    public static void register() {
        reg("void_glow", VOID_GLOW);
        reg("void_star", VOID_STAR);
        reg("void_shard", VOID_SHARD);
    }

    private static void reg(String name, DefaultParticleType type) {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(HollowSovereign.MOD_ID, name), type);
    }
}
