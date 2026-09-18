package com.hollowsovereign.ability;

import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.net.HSNet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ability activation, cooldown gating and dispatch. The Void Weaver has its full
 * kit (see {@link VoidWeaver}); the other three classes have their two starter
 * abilities live, with the rest stubbed until their phase.
 */
public final class Abilities {
    private Abilities() {}

    public static final int ULT_SLOT = 6;

    public static final int[] UNLOCK_LEVEL = {1, 1, 5, 10, 20, 25};
    public static final int ULT_UNLOCK_LEVEL = 15;

    /** Void Weaver is the "domain-first" class: he starts with the domain (ult, level 1)
     *  and unlocks his toolkit — the abilities he uses inside it — as he levels. */
    private static final int[] VOID_UNLOCK = {5, 8, 11, 14, 17, 20};

    /** Level at which {@code slot} (0..5 or ULT_SLOT) unlocks for the given class. */
    public static int unlockLevel(SorcererClass clazz, int slot) {
        if (clazz == SorcererClass.VOID_WEAVER) {
            return slot == ULT_SLOT ? 1 : VOID_UNLOCK[slot];
        }
        return slot == ULT_SLOT ? ULT_UNLOCK_LEVEL : UNLOCK_LEVEL[slot];
    }

    public static final String[][] NAMES = {
        {"Void Pull", "Blink Step", "Umbral Dash", "Void Ward", "Gravity Snare", "Singularity Grasp"},
        {"Blood Whip", "Crimson Guard", "War Roar", "Hemorrhage Spikes", "Life Siphon", "Blood Pact"},
        {"Shade Hound", "Binding Chains", "Warding Sigil", "Curse Mark", "Shade Swarm", "Spirit Tether"},
        {"Quickdraw", "Storm Dash", "Thunder Combo", "Riposte Stance", "Skyfall Cleave", "Arc Slash"},
    };
    public static final String[] ULT_NAMES = {"Starless Expanse", "Heartpiercer Lance", "Hollow Menagerie", "Thousand Ash Cuts"};

    private static final int[][] COOLDOWN_TICKS = {
        // Void Pull, Blink | Umbral Dash, Void Ward, Gravity Snare, Singularity Grasp (ult-tier: long)
        {120, 100, 140, 300, 240, 700},
        {120, 240, 0, 0, 0, 0},
        {300, 160, 0, 0, 0, 0},
        {80, 120, 0, 0, 0, 0},
    };
    // Void Weaver's domain is a toggle (press to open, press again to close), so no cooldown.
    private static final int[] ULT_COOLDOWN = {0, 0, 0, 0};

    public static String abilityName(SorcererClass clazz, int slot) {
        if (slot == ULT_SLOT) return ULT_NAMES[clazz.ordinal()];
        return NAMES[clazz.ordinal()][slot];
    }

    public static int cooldownTicks(SorcererClass clazz, int slot) {
        if (slot == ULT_SLOT) return ULT_COOLDOWN[clazz.ordinal()];
        return COOLDOWN_TICKS[clazz.ordinal()][slot];
    }

    /** Whether the ability actually has a coded effect yet (drives HUD brightness). */
    public static boolean isImplemented(SorcererClass clazz, int slot) {
        if (clazz == SorcererClass.VOID_WEAVER) {
            return slot == ULT_SLOT || (slot >= 0 && slot <= 5);
        }
        return slot != ULT_SLOT && slot >= 0 && slot < 2;
    }

    public static void activate(ServerPlayerEntity player, int slot) {
        PlayerData data = PlayerData.get(player);
        if (!data.hasClass()) {
            actionbar(player, "Choose a class first.");
            return;
        }
        SorcererClass clazz = data.getSorcererClass();
        boolean isUlt = slot == ULT_SLOT;
        if (!isUlt && (slot < 0 || slot > 5)) return;

        String name = abilityName(clazz, slot);
        int unlock = unlockLevel(clazz, slot);
        if (data.getLevel() < unlock) {
            actionbar(player, name + " unlocks at level " + unlock + ".");
            return;
        }
        if (!isImplemented(clazz, slot)) {
            actionbar(player, name + " isn't wired up yet — coming soon.");
            return;
        }

        long now = player.getWorld().getTime();
        if (data.isOnCooldown(slot, now)) {
            long remain = (data.getCooldownEnd(slot) - now + 19) / 20;
            actionbar(player, name + " on cooldown (" + remain + "s)");
            return;
        }

        ServerWorld world = player.getServerWorld();
        int tier;
        if (clazz == SorcererClass.VOID_WEAVER) {
            tier = VoidWeaver.activate(player, slot);
        } else {
            tier = switch (clazz) {
                case CRIMSON_WARDEN -> (slot == 0) ? bloodWhip(world, player) : crimsonGuard(world, player);
                case HEX_BINDER -> (slot == 0) ? shadeHound(world, player) : bindingChains(world, player);
                case ASHEN_BLADE -> (slot == 0) ? quickdraw(world, player) : stormDash(world, player);
                default -> 2;
            };
        }

        // A return of 0 means the cast was refused (e.g. a domain-only ability used outside the
        // domain, or no valid target) — don't burn the cooldown for a no-op.
        int cd = tier <= 0 ? 0 : cooldownTicks(clazz, slot);
        if (clazz == SorcererClass.VOID_WEAVER && cd > 0) cd = VoidWeaver.adjustCooldown(player, slot, cd);
        if (cd > 0) data.setCooldownEnd(slot, now + cd);
        HSNet.sendAbilityFeedback(player, slot, cd, Math.max(tier, 0));
    }

    // ---------- Crimson Warden ----------

    private static int bloodWhip(ServerWorld world, ServerPlayerEntity player) {
        LivingEntity target = nearestEnemy(world, player, 10.0);
        if (target != null) {
            Vec3d pull = player.getPos().subtract(target.getPos()).normalize().multiply(1.1);
            target.setVelocity(pull.x, 0.35, pull.z);
            target.velocityModified = true;
            target.damage(playerSource(player), 3.0f);
            if (target instanceof MobEntity mob) mob.setTarget(player);
            spawnLine(world, player.getPos().add(0, 1, 0), target.getPos().add(0, 1, 0), redDust());
        }
        play(world, player, SoundEvents.ENTITY_LEASH_KNOT_BREAK, 0.9f, 0.8f);
        return 2;
    }

    private static int crimsonGuard(ServerWorld world, ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1, false, true));
        world.spawnParticles(redDust(), player.getX(), player.getY() + 1, player.getZ(), 50, 0.6, 1.0, 0.6, 0.02);
        play(world, player, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 0.6f);
        return 2;
    }

    // ---------- Hex Binder ----------

    private static int shadeHound(ServerWorld world, ServerPlayerEntity player) {
        WolfEntity hound = EntityType.WOLF.create(world);
        if (hound != null) {
            Vec3d look = player.getRotationVec(1.0f);
            hound.refreshPositionAndAngles(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5,
                    player.getYaw(), 0);
            hound.setTamed(true);
            hound.setOwnerUuid(player.getUuid());
            hound.setCustomName(Text.literal("Shade Hound"));
            hound.setPersistent();
            hound.setHealth(hound.getMaxHealth());
            world.spawnEntity(hound);
            world.spawnParticles(ParticleTypes.SOUL, hound.getX(), hound.getY() + 0.4, hound.getZ(), 30, 0.3, 0.3, 0.3, 0.02);
        }
        play(world, player, SoundEvents.ENTITY_WOLF_HOWL, 0.9f, 0.7f);
        return 2;
    }

    private static int bindingChains(ServerWorld world, ServerPlayerEntity player) {
        LivingEntity target = nearestEnemy(world, player, 10.0);
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 4, false, true));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1, false, true));
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 0.5, target.getZ(), 30, 0.3, 0.6, 0.3, 0.01);
        }
        play(world, player, SoundEvents.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        return 2;
    }

    // ---------- Ashen Blade ----------

    private static int quickdraw(ServerWorld world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        DamageSource src = playerSource(player);
        for (LivingEntity target : nearbyEnemies(world, player, 4.5)) {
            Vec3d to = target.getPos().subtract(player.getPos()).normalize();
            if (to.dotProduct(look) > 0.3) {
                target.damage(src, 6.0f);
            }
        }
        Vec3d tip = player.getEyePos().add(look.multiply(2.5));
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, tip.x, tip.y, tip.z, 4, 0.4, 0.4, 0.4, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 30, 0.6, 0.6, 0.6, 0.2);
        play(world, player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        return 2;
    }

    private static int stormDash(ServerWorld world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        player.setVelocity(look.x * 1.6, 0.25, look.z * 1.6);
        player.velocityModified = true;
        DamageSource src = playerSource(player);
        for (LivingEntity target : nearbyEnemies(world, player, 3.5)) {
            target.damage(src, 4.0f);
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.6, 0.4, 0.4);
        play(world, player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
        return 2;
    }

    // ---------- helpers ----------

    private static List<LivingEntity> nearbyEnemies(ServerWorld world, ServerPlayerEntity player, double radius) {
        Box box = player.getBoundingBox().expand(radius);
        return world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof PlayerEntity) && e.squaredDistanceTo(player) <= radius * radius);
    }

    private static LivingEntity nearestEnemy(ServerWorld world, ServerPlayerEntity player, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearbyEnemies(world, player, radius)) {
            double d = e.squaredDistanceTo(player);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return best;
    }

    private static DamageSource playerSource(ServerPlayerEntity player) {
        return player.getDamageSources().playerAttack(player);
    }

    private static DustParticleEffect redDust() {
        return new DustParticleEffect(new Vector3f(0.7f, 0.02f, 0.05f), 1.4f);
    }

    private static void spawnLine(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect particle) {
        int steps = 12;
        Vec3d step = to.subtract(from).multiply(1.0 / steps);
        Vec3d p = from;
        for (int i = 0; i <= steps; i++) {
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            p = p.add(step);
        }
    }

    private static void play(ServerWorld world, Entity at, net.minecraft.sound.SoundEvent sound, float vol, float pitch) {
        world.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundCategory.PLAYERS, vol, pitch);
    }

    private static void actionbar(ServerPlayerEntity player, String msg) {
        player.sendMessage(Text.literal(msg), true);
    }
}
