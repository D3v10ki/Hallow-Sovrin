package com.hollowsovereign.ability;

import com.hollowsovereign.particle.HSParticles;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Particle choreography primitives for the Void Weaver (deep purple/blue starfield look). */
public final class VoidFx {
    private VoidFx() {}

    public static final Vector3f PURPLE = new Vector3f(0.62f, 0.35f, 1.0f);
    public static final Vector3f INDIGO = new Vector3f(0.35f, 0.45f, 0.95f);
    public static final Vector3f BLACK_VIOLET = new Vector3f(0.20f, 0.08f, 0.35f);

    /** One controlled particle with an exact velocity (count=0 keeps the velocity we pass). */
    public static void mote(ServerWorld w, DefaultParticleType type, Vec3d p, Vec3d v) {
        w.spawnParticles(type, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1.0);
    }

    /** Scattered cloud of a custom particle. */
    public static void cloud(ServerWorld w, DefaultParticleType type, Vec3d p, int count, double spread, double speed) {
        w.spawnParticles(type, p.x, p.y, p.z, count, spread, spread, spread, speed);
    }

    /** Colored dust cloud (arbitrary color, great for rings/haze). */
    public static void dust(ServerWorld w, Vector3f color, float size, Vec3d p, int count, double spread) {
        DustParticleEffect effect = new DustParticleEffect(color, size);
        w.spawnParticles(effect, p.x, p.y, p.z, count, spread, spread, spread, 0.0);
    }

    /** A spinning ring of glow motes on the XZ plane, optionally pulling inward and rising. */
    public static void ring(ServerWorld w, DefaultParticleType type, Vec3d c, double radius, int n,
                            double spin, double inward, double up, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + (Math.PI * 2 * i) / n;
            double cos = Math.cos(a), sin = Math.sin(a);
            Vec3d pos = new Vec3d(c.x + cos * radius, c.y, c.z + sin * radius);
            Vec3d tangent = new Vec3d(-sin, 0, cos).multiply(spin);
            Vec3d pullIn = new Vec3d(-cos, 0, -sin).multiply(inward);
            Vec3d vel = tangent.add(pullIn).add(0, up, 0);
            mote(w, type, pos, vel);
        }
    }

    /** A colored dust ring (for the darker energy halo behind the glow). */
    public static void dustRing(ServerWorld w, Vector3f color, float size, Vec3d c, double radius, int n, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + (Math.PI * 2 * i) / n;
            Vec3d pos = new Vec3d(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius);
            dust(w, color, size, pos, 1, 0.02);
        }
    }

    /** Outward spherical burst of motes. */
    public static void burst(ServerWorld w, DefaultParticleType type, Vec3d c, int n, double speed, double upBias) {
        for (int i = 0; i < n; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.acos(2 * Math.random() - 1);
            Vec3d dir = new Vec3d(Math.sin(phi) * Math.cos(theta), Math.cos(phi) + upBias, Math.sin(phi) * Math.sin(theta));
            mote(w, type, c, dir.multiply(speed));
        }
    }

    /** A vertical rift of shards + light — the "crack in space-time". */
    public static void rift(ServerWorld w, Vec3d c, double height, int density) {
        for (int i = 0; i < density; i++) {
            double t = Math.random();
            double y = c.y + (t - 0.2) * height;
            double jitter = (Math.random() - 0.5) * 0.35;
            Vec3d pos = new Vec3d(c.x + jitter, y, c.z + jitter);
            Vec3d vel = new Vec3d((Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.25);
            mote(w, HSParticles.VOID_SHARD, pos, vel);
        }
        cloud(w, HSParticles.VOID_STAR, c.add(0, height * 0.3, 0), 10, 0.25, 0.05);
        dust(w, BLACK_VIOLET, 2.2f, c.add(0, height * 0.3, 0), 12, 0.4);
    }

    /** Trail of glow motes between two points (afterimage for blink / dashes). */
    public static void trail(ServerWorld w, Vec3d from, Vec3d to, int steps) {
        Vec3d step = to.subtract(from).multiply(1.0 / steps);
        Vec3d p = from;
        for (int i = 0; i <= steps; i++) {
            mote(w, HSParticles.VOID_GLOW, p, new Vec3d(0, 0.01, 0));
            if (i % 2 == 0) dust(w, INDIGO, 1.3f, p, 1, 0.05);
            p = p.add(step);
        }
    }
}
