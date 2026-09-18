package com.hollowsovereign.ability;

import com.hollowsovereign.entity.HSEntities;
import com.hollowsovereign.net.HSNet;
import com.hollowsovereign.particle.HSParticles;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooSaplingBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Control class. Every ability leans on {@link VoidFx} and, when it needs to
 * animate over time, {@link HSEffects}. */
public final class VoidWeaver {
    private VoidWeaver() {}

    private static final double MAX_BLINK = 200.0;
    /** Radius of the domain (Starless Expanse). Bigger = grander but more block churn on open/close. */
    public static final double DOMAIN_RADIUS = 30.0;

    public static int activate(ServerPlayerEntity player, int slot) {
        ServerWorld world = player.getServerWorld();
        return switch (slot) {
            case 0 -> voidPull(world, player);
            case 1 -> blinkStep(world, player);
            case 2 -> umbralDash(world, player);
            case 3 -> voidWard(world, player);
            case 4 -> gravitySnare(world, player);
            case 5 -> singularityGrasp(world, player);
            case Abilities.ULT_SLOT -> starlessExpanse(world, player);
            default -> 2;
        };
    }

    // ---------------- 0: Void Pull ----------------

    /** A one-shot yank of a single aimed enemy toward the caster (the gravity-well pull math delivered
     *  as a burst, not a sustained field), with a brief stagger when it arrives. A thin tendril tether
     *  marks the pull for its short duration. */
    private static final int PULL_MAX_TARGETS = 5;

    private static int voidPull(ServerWorld world, ServerPlayerEntity player) {
        List<LivingEntity> targets = targetsInCone(world, player, 24.0, 0.82, PULL_MAX_TARGETS);
        if (targets.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.literal("No targets in your sights.")
                    .formatted(net.minecraft.util.Formatting.GRAY), true);
            return 0;
        }
        play(world, player.getPos(), SoundEvents.ENTITY_LEASH_KNOT_BREAK, 0.9f, 0.8f);
        play(world, player.getPos(), SoundEvents.BLOCK_CONDUIT_AMBIENT, 0.9f, 1.4f);
        Vec3d anchor = player.getPos().add(0, 1.1, 0);
        for (LivingEntity target : targets) {
            // each pulled enemy gets its own thin tether, so several converge on the caster at once
            com.hollowsovereign.entity.TendrilEntity tether =
                    new com.hollowsovereign.entity.TendrilEntity(HSEntities.TENDRIL, world);
            tether.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z, 0f, 0f);
            tether.setTether(player, target, 1.0f, 10);
            world.spawnEntity(tether);
            HSEffects.add(world, new PullEffect(player, target));
        }
        return 3;
    }

    private static final class PullEffect implements HSEffects.Effect {
        private final ServerPlayerEntity owner;
        private final LivingEntity target;
        private int age = 0;
        private static final int LIFE = 10;
        PullEffect(ServerPlayerEntity owner, LivingEntity target) { this.owner = owner; this.target = target; }

        @Override public boolean tick(ServerWorld world) {
            if (!owner.isAlive() || !target.isAlive() || target.isRemoved()) return false;
            Vec3d to = owner.getPos().add(0, 1.0, 0).subtract(target.getPos().add(0, target.getHeight() * 0.5, 0));
            double dist = to.length();
            if (dist > 1.4) {
                Vec3d pull = to.normalize().multiply(Math.min(1.5, 0.5 + dist * 0.14));
                target.setVelocity(pull.x, Math.max(-0.2, Math.min(0.6, pull.y)), pull.z);
                target.velocityModified = true;
                target.fallDistance = 0f;
            }
            boolean end = ++age >= LIFE;
            if (end) {
                // brief stagger on arrival — a punish, not a full CC lock
                target.setVelocity(0, target.getVelocity().y, 0);
                target.velocityModified = true;
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 12, 6, false, false));
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.9f, 0.8f);
            }
            return !end;
        }
    }

    // ---------------- 1: Blink Step ----------------

    /** Void Step — teleport up to 200 blocks to the block the player is looking at. */
    private static int blinkStep(ServerWorld world, ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = eye.add(dir.multiply(MAX_BLINK));
        BlockHitResult hit = world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

        Vec3d dest;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // stand on the face we hit (air block next to the target block)
            BlockPos stand = hit.getBlockPos().offset(hit.getSide());
            dest = new Vec3d(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        } else {
            // nothing within range — jump to the far point along the look vector
            dest = end;
        }

        Vec3d origin = player.getPos().add(0, 1.0, 0);
        VoidFx.rift(world, origin, 2.4, 30);
        play(world, origin, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.9f);

        // Capture the body/head pose at cast so the afterimage ghosts freeze the moment of departure.
        Vec3d fromFeet = player.getPos();
        float bodyYaw = player.bodyYaw, headYaw = player.headYaw, pitch = player.getPitch();

        player.networkHandler.requestTeleport(dest.x, dest.y, dest.z, player.getYaw(), player.getPitch());
        player.fallDistance = 0f;

        // Afterimage: fading ghosts pre-spaced along the whole hop (the teleport itself stays instant),
        // plus a rupture shockwave ring at both the departure and arrival points.
        HSNet.sendTrail(player, fromFeet, dest, 7, bodyYaw, headYaw, pitch, 1.0f, true, 8);

        Vec3d to = dest.add(0, 1.0, 0);
        VoidFx.rift(world, to, 2.4, 30);
        VoidFx.burst(world, HSParticles.VOID_STAR, to, 22, 0.3, 0.1);
        play(world, to, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
        play(world, to, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f);
        return 3;
    }

    // ================ Control kit (slots 2-5) ================
    // Crowd-control tools that work anywhere. Displacement / immobilization / zone denial over damage.

    // ---------------- 2: Umbral Dash ----------------

    /** A hard forward dash (real travel, not a teleport) that shoulders hostiles in its path aside.
     *  It's a control tool because of that displacement, not the small contact damage. The afterimage
     *  ghost trail is laid down progressively as it travels (one ghost per tick) rather than all at once. */
    private static int umbralDash(ServerWorld world, ServerPlayerEntity player) {
        Vec3d dir = flat(player);
        play(world, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        play(world, player.getPos(), SoundEvents.ENTITY_PHANTOM_FLAP, 0.9f, 0.7f);
        HSEffects.add(world, new DashEffect(player, dir));
        return 3;
    }

    private static final class DashEffect implements HSEffects.Effect {
        private final ServerPlayerEntity owner;
        private final Vec3d dir;
        private final Vec3d side;                          // perpendicular, for shoving enemies aside
        private final java.util.Set<UUID> shoved = new java.util.HashSet<>();
        private Vec3d prev;
        private int age = 0;
        private static final int LIFE = 6;                 // ~0.3s of powered travel
        private static final double SPEED = 1.75;

        DashEffect(ServerPlayerEntity owner, Vec3d dir) {
            this.owner = owner;
            this.dir = dir;
            this.side = new Vec3d(-dir.z, 0, dir.x);
            this.prev = owner.getPos();
        }

        @Override public boolean tick(ServerWorld world) {
            if (!owner.isAlive()) return false;
            owner.setVelocity(dir.x * SPEED, owner.getVelocity().y * 0.2, dir.z * SPEED);
            owner.velocityModified = true;
            owner.fallDistance = 0f;

            // Wider capsule around the dash line (3.4) so it catches enemies slightly off-centre too.
            for (LivingEntity e : enemiesNear(world, owner.getPos(), 3.4, owner)) {
                if (!shoved.add(e.getUuid())) continue;    // shove each caught enemy once per dash
                Vec3d rel = e.getPos().subtract(owner.getPos());
                Vec3d perp = rel.subtract(dir.multiply(rel.dotProduct(dir)));   // offset from the dash line
                Vec3d away = perp.lengthSquared() < 0.01 ? side : perp.normalize();
                Vec3d push = away.multiply(1.2).add(dir.multiply(0.15));         // shove away from the line
                e.setVelocity(push.x, 0.4, push.z);
                e.velocityModified = true;
                e.damage(source(owner), 3.0f);
            }

            Vec3d now = owner.getPos();
            // one stretched ghost per tick along the segment just travelled = a progressive motion streak
            HSNet.sendTrail(owner, prev, now, 1, owner.bodyYaw, owner.headYaw, owner.getPitch(), 1.6f, false, 7);
            VoidFx.trail(world, prev.add(0, 1.0, 0), now.add(0, 1.0, 0), 3);
            prev = now;
            return ++age < LIFE;
        }
    }

    // ---------------- 3: Void Ward ----------------

    /** A short personal barrier: strong damage reduction while up, and any enemy that presses into
     *  melee range is rooted + slowed — punishing attacking into it. Simple particle shell for now. */
    /** Players with an active Void Ward: UUID -> world-tick the ward ends. Read by the damage event. */
    public static final Map<UUID, Long> ACTIVE_WARDS = new HashMap<>();
    private static final int WARD_TICKS = 100;             // 5s

    private static int voidWard(ServerWorld world, ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, WARD_TICKS, 1, false, true)); // -40% dmg
        ACTIVE_WARDS.put(player.getUuid(), world.getTime() + WARD_TICKS);
        HSNet.sendWard(player, WARD_TICKS);                // client draws the emissive dome
        play(world, player.getPos(), SoundEvents.BLOCK_CONDUIT_ACTIVATE, 0.8f, 1.4f);
        play(world, player.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);
        return 3;
    }

    /** Whether this player currently has an active ward (checked by the on-hit damage event). */
    public static boolean isWardActive(ServerPlayerEntity p) {
        Long end = ACTIVE_WARDS.get(p.getUuid());
        return end != null && p.getServerWorld().getTime() < end;
    }

    /**
     * Real on-hit response (called from {@code ServerLivingEntityEvents.ALLOW_DAMAGE}): when a warded
     * player is struck in melee, root + slow the attacker and flare the dome at the hit direction. The
     * RESISTANCE from casting already handles the damage reduction, so we never cancel the hit here.
     */
    public static void wardStruck(ServerPlayerEntity victim, LivingEntity attacker) {
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 6, false, false));
        attacker.setVelocity(0, Math.min(0, attacker.getVelocity().y), 0); // root pulse
        attacker.velocityModified = true;
        Vec3d dir = attacker.getPos().subtract(victim.getPos());
        dir = dir.lengthSquared() < 0.01 ? new Vec3d(0, 0, 1) : dir.normalize();
        HSNet.sendWardImpact(victim, dir);
        victim.getServerWorld().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.5f, 1.8f);
    }

    // ---------------- 4: Gravity Snare ----------------

    /** A placed, stationary mini gravity field: continuously drags enemies within it toward its centre
     *  and slows them for a few seconds. A scaled-down domain gravity well, minus the blocks/floor
     *  shader — just a small dark distortion patch on the ground. */
    private static int gravitySnare(ServerWorld world, ServerPlayerEntity player) {
        Vec3d spot = groundTarget(world, player, 20.0);
        // Domain synergy: inside your open Starless Expanse the snare grows + pulls harder, and its
        // ground visual switches to rippling the domain's void-water floor instead of the dark patch.
        DomainInstance dom = activeDomainOf(player);
        boolean inDomain = dom != null && dom.contains(spot);
        double radius = inDomain ? 11.0 : 7.0;
        double pullMul = inDomain ? 1.8 : 1.0;
        play(world, spot, SoundEvents.BLOCK_CONDUIT_AMBIENT, 1.0f, 0.6f);
        play(world, spot, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.7f, 1.6f);
        if (inDomain) play(world, spot, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 0.9f, 0.7f);
        HSEffects.add(world, new SnareEffect(spot, player, radius, pullMul, inDomain));
        return 3;
    }

    /** Placed pull-zone. Radius/strength and the "domain" visual mode are passed in so the same effect
     *  serves both the normal cast and the upgraded in-domain cast (see gravitySnare / domain synergy). */
    private static final class SnareEffect implements HSEffects.Effect {
        private final Vec3d center;
        private final ServerPlayerEntity owner;
        private final double radius;
        private final double pullMul;
        private final boolean domain;
        private int age = 0;
        private static final int LIFE = 100;               // 5s
        SnareEffect(Vec3d center, ServerPlayerEntity owner, double radius, double pullMul, boolean domain) {
            this.center = center; this.owner = owner; this.radius = radius; this.pullMul = pullMul; this.domain = domain;
        }

        @Override public boolean tick(ServerWorld world) {
            Vec3d c = center.add(0, 0.5, 0);
            List<LivingEntity> affected = enemiesNear(world, c, radius, owner);
            // group centroid so we can clump enemies toward each other, not only toward the zone centre
            Vec3d centroid = Vec3d.ZERO;
            if (affected.size() >= 2) {
                for (LivingEntity e : affected) centroid = centroid.add(e.getPos());
                centroid = centroid.multiply(1.0 / affected.size());
            }
            for (LivingEntity e : affected) {
                Vec3d ep = e.getPos().add(0, e.getHeight() * 0.5, 0);
                Vec3d toCenter = c.subtract(ep);
                double dist = toCenter.length();
                Vec3d pull = dist < 0.6 ? Vec3d.ZERO
                        : toCenter.normalize().multiply(Math.min(0.45, (0.08 + 0.5 / (dist + 1.0)) * pullMul));
                if (affected.size() >= 2) {                 // gentle clump toward the group centroid
                    Vec3d toGroup = centroid.subtract(e.getPos());
                    if (toGroup.lengthSquared() > 0.25) pull = pull.add(toGroup.normalize().multiply(0.05));
                }
                e.setVelocity(e.getVelocity().multiply(0.7).add(pull.x, pull.y * 0.3, pull.z));
                e.velocityModified = true;
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 2, false, false));
            }
            snareVisual(world);
            return ++age < LIFE;
        }

        private void snareVisual(ServerWorld world) {
            if (domain) {
                // Blend with the domain's void-water floor: ripple it at the snare centre + a rotating
                // ring of points around it, so the zone reads as a disturbance on the existing shader.
                if (age % 5 == 0) {
                    HSNet.sendFloorRipple(owner, center.x, center.z);
                    double rr = radius * 0.6;
                    for (int i = 0; i < 4; i++) {
                        double a = age * 0.15 + i * (Math.PI / 2);
                        HSNet.sendFloorRipple(owner, center.x + Math.cos(a) * rr, center.z + Math.sin(a) * rr);
                    }
                }
                VoidFx.dust(world, VoidFx.BLACK_VIOLET, 1.6f, center.add(0, 0.15, 0), 3, radius * 0.25);
            } else {
                VoidFx.dust(world, VoidFx.BLACK_VIOLET, 2.4f, center.add(0, 0.15, 0), 6, radius * 0.35);
                VoidFx.ring(world, HSParticles.VOID_STAR, center.add(0, 0.2, 0), radius * 0.75, 16, 0.05, 0.12, 0.0, age * 0.2);
            }
        }
    }

    // ---------------- 5: Singularity Grasp (level-5 payoff) ----------------

    /** The capstone control ability: drop a real (smaller-scale) black hole at the aimed spot. For a
     *  few seconds it drags EVERY enemy in a large radius inward far harder than any other tool here;
     *  for the final stretch it holds them rooted at the centre, then bursts them outward as it
     *  collapses. Reuses the domain's black-hole model + core/disc shaders at a fraction of the scale
     *  (see {@code BlackHoleEntity#setScale}). Long cooldown to match its power and signature look. */
    private static int singularityGrasp(ServerWorld world, ServerPlayerEntity player) {
        Vec3d spot = groundTarget(world, player, 24.0).add(0, 3.0, 0); // float it just above the ground
        com.hollowsovereign.entity.BlackHoleEntity bh = HSEntities.BLACK_HOLE.create(world);
        if (bh != null) {
            bh.setScale(0.42f);
            bh.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0f, 0f);
            world.spawnEntity(bh);
        }
        play(world, spot, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.3f, 0.6f);
        play(world, spot, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 1.3f, 0.4f);
        play(world, spot, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        HSEffects.add(world, new SingularityEffect(spot, player, bh));
        return 5;
    }

    private static final class SingularityEffect implements HSEffects.Effect {
        private final Vec3d center;
        private final ServerPlayerEntity owner;
        private final com.hollowsovereign.entity.BlackHoleEntity core;
        private int age = 0;
        private static final int LIFE = 110;                // ~5.5s
        private static final int HOLD_AT = 80;               // last ~1.5s: root them at the centre
        private static final double R = 16.0;                // large AOE: pulls ALL enemies in range at once
        SingularityEffect(Vec3d center, ServerPlayerEntity owner, com.hollowsovereign.entity.BlackHoleEntity core) {
            this.center = center; this.owner = owner; this.core = core;
        }

        @Override public boolean tick(ServerWorld world) {
            boolean holding = age >= HOLD_AT;
            for (LivingEntity e : enemiesNear(world, center, R, owner)) {
                Vec3d to = center.subtract(e.getPos().add(0, e.getHeight() * 0.5, 0));
                double dist = to.length();
                if (holding) {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8, 8, false, false));
                    if (dist > 1.4) {
                        Vec3d pull = to.normalize().multiply(1.4);
                        e.setVelocity(pull.x, pull.y * 0.4, pull.z);
                    } else {
                        e.setVelocity(0, e.getVelocity().y * 0.2, 0);   // held at centre
                    }
                    e.velocityModified = true;
                    e.fallDistance = 0f;
                } else {
                    Vec3d pull = dist < 1.0 ? Vec3d.ZERO
                            : to.normalize().multiply(Math.min(1.7, 0.4 + 8.0 / (dist + 1.5)));
                    e.setVelocity(e.getVelocity().multiply(0.5).add(pull));
                    e.velocityModified = true;
                    e.fallDistance = 0f;
                }
            }
            VoidFx.cloud(world, HSParticles.VOID_STAR, center, 6, R * 0.35, 0.0);
            boolean end = ++age >= LIFE;
            if (end) {
                for (LivingEntity e : enemiesNear(world, center, R, owner)) {
                    Vec3d k = e.getPos().subtract(center);
                    k = k.lengthSquared() < 0.01 ? new Vec3d(0, 1, 0) : k.normalize().multiply(0.9);
                    e.setVelocity(k.x, 0.45, k.z);
                    e.velocityModified = true;
                }
                VoidFx.burst(world, HSParticles.VOID_GLOW, center, 120, 1.6, 0.1);
                play(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                if (core != null) core.discard();
            }
            return !end;
        }
    }

    // ---------------- domain synergy ----------------

    /** The caster's own domain if it is currently open (ACTIVE), else null. */
    private static DomainInstance activeDomainOf(ServerPlayerEntity player) {
        DomainInstance d = DomainInstance.ACTIVE.get(player.getUuid());
        return (d != null && d.isActive()) ? d : null;
    }

    /** Domain synergy: Singularity Grasp (slot 5) costs far less while your Starless Expanse is open,
     *  reinforcing the black-hole identity of that state. Called by {@link Abilities#activate}. */
    public static int adjustCooldown(ServerPlayerEntity player, int slot, int baseCd) {
        if (slot == 5) {
            DomainInstance d = DomainInstance.ACTIVE.get(player.getUuid());
            if (d != null && d.isActive()) return Math.max(60, baseCd / 3); // 700t -> ~233t (~11.7s) in-domain
        }
        return baseCd;
    }

    // ---------------- targeting helpers ----------------

    /** All living enemies within range inside the aim cone (dot >= minDot), nearest first, capped at max. */
    private static List<LivingEntity> targetsInCone(ServerWorld world, ServerPlayerEntity player,
                                                    double range, double minDot, int max) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity e : enemiesNear(world, player.getPos(), range, player)) {
            Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d < 0.1) continue;
            if (to.multiply(1.0 / d).dotProduct(look) < minDot) continue;   // inside the cone
            hits.add(e);
        }
        hits.sort(java.util.Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        return hits.size() > max ? new ArrayList<>(hits.subList(0, max)) : hits;
    }

    /** Where the player is looking, out to maxDist — the block hit, or the far point if none. */
    private static Vec3d groundTarget(ServerWorld world, ServerPlayerEntity player, double maxDist) {
        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = eye.add(dir.multiply(maxDist));
        BlockHitResult hit = world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getPos() : end;
    }

    // ---------------- ULT: Starless Expanse ----------------

    private static int starlessExpanse(ServerWorld world, ServerPlayerEntity player) {
        // Toggle: if a domain is already open, pressing the ult key closes it.
        DomainInstance open = DomainInstance.ACTIVE.get(player.getUuid());
        if (open != null) {
            open.beginClose(world);
            player.sendMessage(net.minecraft.text.Text.literal("Domain collapsing...")
                    .formatted(net.minecraft.util.Formatting.DARK_PURPLE), true);
            return 4;
        }
        Vec3d cv = player.getPos();
        play(world, cv, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.5f);
        play(world, cv, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 1.4f, 0.4f);
        play(world, cv, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
        VoidFx.burst(world, HSParticles.VOID_GLOW, cv.add(0, 1, 0), 120, 1.8, 0.1);
        DomainInstance inst = new DomainInstance(player.getBlockPos(), (int) DOMAIN_RADIUS, player);
        DomainInstance.ACTIVE.put(player.getUuid(), inst);
        HSEffects.add(world, inst);
        player.sendMessage(net.minecraft.text.Text.literal("Domain Expansion: Starless Expanse")
                .formatted(net.minecraft.util.Formatting.DARK_PURPLE), true);
        return 4;
    }

    /**
     * The domain as a single toggle-able instance. On open it raises solid black walls, spreads a
     * glowing floor sigil and HOLLOWS OUT the interior into an empty void; it holds until the caster
     * presses the ult again, then plays a closing animation and restores everything it touched.
     */
    private static final class DomainInstance implements HSEffects.Effect {
        /** One live domain per owner (by UUID). */
        static final Map<UUID, DomainInstance> ACTIVE = new HashMap<>();

        private enum Phase { OPENING, ACTIVE, CLOSING }

        private final BlockPos center;
        private final int radius;
        private final Vec3d c;
        private final UUID ownerId;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<BlockPos> order = new ArrayList<>();
        private Phase phase = Phase.OPENING;
        private int layer = -1;
        private int age = 0;
        private int restoreIndex = 0;
        private com.hollowsovereign.entity.BlackHoleEntity core;
        private static final int MAX_LIFE = 6000; // 5-minute safety cap
        static final double CORE_Y = 18.0;        // singularity height above the floor centre (high up)

        DomainInstance(BlockPos center, int radius, ServerPlayerEntity owner) {
            this.center = center;
            this.radius = radius;
            this.c = Vec3d.ofCenter(center);
            this.ownerId = owner.getUuid();
        }

        // --- accessors used by the domain-only abilities ---
        boolean isActive() { return phase == Phase.ACTIVE; }
        boolean contains(Vec3d p) { return p.squaredDistanceTo(c) <= (double) radius * radius; }
        Vec3d centerVec() { return c; }
        int radiusBlocks() { return radius; }
        /** World position of the black-hole singularity (matches the spawned model + client floor). */
        Vec3d corePos() { return c.add(0, CORE_Y, 0); }

        void beginClose(ServerWorld world) {
            if (phase == Phase.CLOSING) return;
            phase = Phase.CLOSING;
            restoreIndex = order.size();
            if (core != null) { core.discard(); core = null; }
            VoidFx.burst(world, HSParticles.VOID_GLOW, c.add(0, 1, 0), 90, 1.4, 0.1);
            world.playSound(null, center, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 0.6f);
            world.playSound(null, center, SoundEvents.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 1.2f, 0.5f);
        }

        /** Spawn the GeckoLib black-hole centrepiece once the dome finishes building. */
        private void spawnCore(ServerWorld world) {
            if (core != null) return;
            com.hollowsovereign.entity.BlackHoleEntity e = com.hollowsovereign.entity.HSEntities.BLACK_HOLE.create(world);
            if (e == null) return;
            e.refreshPositionAndAngles(c.x, c.y + CORE_Y, c.z, 0f, 0f);
            world.spawnEntity(e);
            core = e;
        }

        @Override
        public boolean tick(ServerWorld world) {
            switch (phase) {
                case OPENING -> {
                    layer++;
                    if (layer <= radius) buildLayer(world, layer);
                    if (layer >= radius) {
                        phase = Phase.ACTIVE;
                        spawnCore(world);
                    }
                }
                case ACTIVE -> {
                    boolean ownerOk = world.getEntity(ownerId) instanceof ServerPlayerEntity sp && sp.isAlive();
                    if (!ownerOk || age > MAX_LIFE) {
                        beginClose(world);
                        break;
                    }
                    if (age % 20 == 0) applyBuffs(world);
                    Box box = new Box(c.subtract(radius, radius, radius), c.add(radius, radius, radius));
                    for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, box,
                            x -> x.getPos().squaredDistanceTo(c) <= (double) radius * radius)) {
                        domainVisualsFor(world, p);
                        if (age % 10 == 0 && p instanceof ServerPlayerEntity sp) HSNet.sendDomainPing(sp, c.x, c.y, c.z, radius);
                    }
                }
                case CLOSING -> {
                    if (closeStep(world)) {
                        ACTIVE.remove(ownerId);
                        return false;
                    }
                }
            }
            age++;
            return true;
        }

        private void buildLayer(ServerWorld world, int y) {
            placeWallShell(world, y);
            placeFloor(world, y);
            clearInside(world, y);
        }

        /**
         * Watertight hollow-hemisphere shell. For this height we test EVERY block in the slice and
         * keep the ones that fall in the sphere's outer band (radius in [R-1.4, R]). Testing every
         * block — instead of tracing a ring — is what makes the dome leak-proof: no top-cap gap, no
         * diagonal pinholes. The 1.4-block band thickness guarantees no 6-connected hole to the sky.
         */
        private void placeWallShell(ServerWorld world, int y) {
            double outer2 = (double) radius * radius;
            double inner = radius - 1.4;
            double inner2 = inner * inner;
            int worldY = center.getY() + y;
            int rMax = (int) Math.ceil(Math.sqrt(Math.max(0.0, outer2 - (double) y * y)));
            for (int dx = -rMax; dx <= rMax; dx++) {
                for (int dz = -rMax; dz <= rMax; dz++) {
                    double d2 = (double) dx * dx + (double) dz * dz + (double) y * y;
                    if (d2 > outer2 || d2 < inner2) continue;
                    boolean glow = (Math.abs(dx) + Math.abs(dz) + y) % 7 == 0;
                    set(world, new BlockPos(center.getX() + dx, worldY, center.getZ() + dz),
                            (glow ? Blocks.CRYING_OBSIDIAN : Blocks.BLACK_CONCRETE).getDefaultState(), false);
                }
            }
        }

        /**
         * Solid floor disc that spreads one ring outward per layer (set() dedups, so each tick only
         * the new outer ring actually gets placed). Concentric glow rings every 5 blocks draw the
         * magic-circle sigil. Filling the whole disc — not tracing rings — keeps the floor gap-free.
         */
        private void placeFloor(ServerWorld world, int y) {
            int fy = center.getY() - 1;
            for (int dx = -y; dx <= y; dx++) {
                for (int dz = -y; dz <= y; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > y * y) continue;
                    int ring = (int) Math.round(Math.sqrt(d2));
                    boolean glow = ring % 5 == 0; // concentric glowing rings = the "magic circle"
                    set(world, new BlockPos(center.getX() + dx, fy, center.getZ() + dz),
                            (glow ? Blocks.CRYING_OBSIDIAN : Blocks.BLACK_CONCRETE).getDefaultState(), false);
                }
            }
        }

        /**
         * Hollow everything strictly inside the shell's inner face at this height. Clearing to the
         * exact inner face (d² &lt; inner²) — the complement of the wall band (d² ≥ inner²) — leaves
         * no ring of terrain hugging the wall, which is what caused the leftover edge blocks. set()
         * itself protects containers and fragile blocks, so this never deletes a chest/sign/door.
         */
        private void clearInside(ServerWorld world, int y) {
            double inner = radius - 1.4;
            double inner2 = inner * inner;
            int worldY = center.getY() + y;
            int rMax = (int) Math.ceil(Math.sqrt(Math.max(0.0, inner2 - (double) y * y)));
            for (int dx = -rMax; dx <= rMax; dx++) {
                for (int dz = -rMax; dz <= rMax; dz++) {
                    if ((double) dx * dx + (double) dz * dz + (double) y * y >= inner2) continue;
                    set(world, new BlockPos(center.getX() + dx, worldY, center.getZ() + dz),
                            Blocks.AIR.getDefaultState(), true);
                }
            }
        }

        private void set(ServerWorld world, BlockPos p, BlockState newState, boolean skipAir) {
            if (original.containsKey(p)) return;
            BlockState cur = world.getBlockState(p);
            if (cur.isOf(Blocks.BEDROCK)) return;
            if (skipAir && cur.isAir()) return;
            if (cur == newState) return;
            if (!isReplaceable(world, p, cur)) return;
            original.put(p, cur);
            order.add(p);
            world.setBlockState(p, newState, Block.NOTIFY_LISTENERS);
        }

        /**
         * Whether the domain is allowed to touch this block. It clears/replaces almost everything —
         * terrain AND light stuff like leaves, grass, flowers, vines, glass, fences, slabs — so the
         * interior becomes a true empty void (all of it is restored on close). It refuses only:
         *   - blocks with a block entity (chests, barrels, shulkers, furnaces, hoppers, signs,
         *     banners, lecterns, spawners...) so their contents/text are never lost, and
         *   - a few functional / vertically-stacked blocks (doors, beds, bamboo, sugar cane, cactus)
         *     that a player builds with or that would cascade-drop items when their base is removed.
         */
        private boolean isReplaceable(ServerWorld world, BlockPos p, BlockState s) {
            if (s.isAir()) return true;
            if (s.hasBlockEntity()) return false;
            Block b = s.getBlock();
            return !(b instanceof DoorBlock
                    || b instanceof BedBlock
                    || b instanceof BambooBlock
                    || b instanceof BambooSaplingBlock
                    || b instanceof SugarCaneBlock
                    || b instanceof CactusBlock);
        }

        /** Restore in reverse build order over ~20 ticks. Returns true when fully restored. */
        private boolean closeStep(ServerWorld world) {
            int perTick = Math.max(96, order.size() / 20 + 1);
            for (int done = 0; done < perTick && restoreIndex > 0; done++) {
                restoreIndex--;
                BlockPos p = order.get(restoreIndex);
                BlockState orig = original.get(p);
                if (orig != null) world.setBlockState(p, orig, Block.NOTIFY_LISTENERS);
            }
            VoidFx.cloud(world, HSParticles.VOID_STAR, c.add(0, 1, 0), 6, radius * 0.4, 0.0);
            return restoreIndex <= 0;
        }

        private void applyBuffs(ServerWorld world) {
            Box box = new Box(c.subtract(radius, radius, radius), c.add(radius, radius, radius));
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box,
                    x -> x.isAlive() && x.getPos().squaredDistanceTo(c) <= (double) radius * radius)) {
                if (e instanceof PlayerEntity) {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 45, 0, false, false));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 60, 0, false, false));
                } else {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 45, 2, false, false));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 45, 1, false, false));
                }
            }
        }

        private void domainVisualsFor(ServerWorld world, PlayerEntity p) {
            Vec3d pp = p.getPos();
            for (int i = 0; i < 6; i++) {
                Vec3d s = pp.add((Math.random() - 0.5) * 22, Math.random() * 16, (Math.random() - 0.5) * 22);
                VoidFx.mote(world, HSParticles.VOID_STAR, s, new Vec3d(0, 0.01, 0));
            }
        }
    }

    // ---------------- helpers ----------------

    private static Vec3d flat(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d f = new Vec3d(look.x, 0, look.z);
        return f.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : f.normalize();
    }

    private static List<LivingEntity> enemiesNear(ServerWorld w, Vec3d c, double r, PlayerEntity owner) {
        Box box = new Box(c.subtract(r, r, r), c.add(r, r, r));
        return w.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && !(e instanceof PlayerEntity) && e.getPos().squaredDistanceTo(c) <= r * r);
    }

    private static DamageSource source(ServerPlayerEntity player) {
        return player.getDamageSources().playerAttack(player);
    }

    private static void play(ServerWorld world, Vec3d p, net.minecraft.sound.SoundEvent sound, float vol, float pitch) {
        world.playSound(null, p.x, p.y, p.z, sound, SoundCategory.PLAYERS, vol, pitch);
    }
}
