package com.hollowsovereign.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The Impaling Void tendril: a solid GeckoLib bone-chain model spawned at a wall/ceiling anchor,
 * aimed at a target, that grows toward it (cascading bone-scale animation) and applies the hit when
 * the tip arrives. Purely visual/hitbox-free otherwise — the hit is a single instant check at strike.
 * Direction + length are data-tracked so the client renderer can orient and length-scale the model.
 */
public class TendrilEntity extends Entity implements GeoEntity {
    private static final RawAnimation GROW = RawAnimation.begin().thenPlayAndHold("grow");
    private static final RawAnimation SWAY = RawAnimation.begin().thenLoop("sway");
    private static final int STRIKE = 5;      // tip-arrival tick (~0.25s)
    private static final int LIFETIME = 16;   // grow + brief hold, then vanish

    private static final TrackedData<Float> DIR_X = DataTracker.registerData(TendrilEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DIR_Y = DataTracker.registerData(TendrilEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DIR_Z = DataTracker.registerData(TendrilEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LENGTH = DataTracker.registerData(TendrilEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** Sideways scale of the mesh, synced so the renderer can draw a fat impale or a thin tether. */
    private static final TrackedData<Float> THICK = DataTracker.registerData(TendrilEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private UUID targetId;
    private UUID ownerId;

    // Tether mode (Void Pull): harmless, thin, and re-aimed each tick to track owner -> target
    // instead of striking once. Configurable strike/life so one entity serves both roles.
    private boolean tether = false;
    private float damageAmt = 12.0f;
    private int strikeTick = STRIKE;
    private int lifeTicks = LIFETIME;

    public TendrilEntity(EntityType<? extends TendrilEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.ignoreCameraFrustum = true; // model is scaled far past the tiny hitbox
    }

    /** Server: aim from the anchor (this entity's position) at the target. */
    public void setAim(Vec3d dir, float length, LivingEntity target, PlayerEntity owner) {
        this.dataTracker.set(DIR_X, (float) dir.x);
        this.dataTracker.set(DIR_Y, (float) dir.y);
        this.dataTracker.set(DIR_Z, (float) dir.z);
        this.dataTracker.set(LENGTH, length);
        this.targetId = target.getUuid();
        this.ownerId = owner.getUuid();
    }

    /**
     * Server: configure this tendril as a thin, harmless pull-line for Void Pull. It anchors at the
     * owner's chest and, each tick, re-aims itself at the (moving, being-pulled) target so the line
     * stays taut for its short life. The pull itself is applied by the ability's effect, not here.
     */
    public void setTether(PlayerEntity owner, LivingEntity target, float thickness, int life) {
        this.tether = true;
        this.damageAmt = 0f;
        this.strikeTick = -1;
        this.lifeTicks = life;
        this.ownerId = owner.getUuid();
        this.targetId = target.getUuid();
        this.dataTracker.set(THICK, thickness);
        reaim(owner.getPos().add(0, 1.1, 0), target);
    }

    private void reaim(Vec3d anchor, LivingEntity target) {
        Vec3d tp = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d d = tp.subtract(anchor);
        double len = d.length();
        if (len < 0.05) { d = new Vec3d(0, 1, 0); len = 1.0; } else d = d.multiply(1.0 / len);
        this.dataTracker.set(DIR_X, (float) d.x);
        this.dataTracker.set(DIR_Y, (float) d.y);
        this.dataTracker.set(DIR_Z, (float) d.z);
        this.dataTracker.set(LENGTH, (float) len);
    }

    public Vec3d dir() {
        return new Vec3d(dataTracker.get(DIR_X), dataTracker.get(DIR_Y), dataTracker.get(DIR_Z));
    }

    public float length() { return dataTracker.get(LENGTH); }

    public float thickness() { return dataTracker.get(THICK); }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(DIR_X, 0f);
        this.dataTracker.startTracking(DIR_Y, -1f);
        this.dataTracker.startTracking(DIR_Z, 0f);
        this.dataTracker.startTracking(LENGTH, 4f);
        this.dataTracker.startTracking(THICK, 5.0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(Vec3d.ZERO);
        this.setNoGravity(true);
        if (this.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) this.getWorld();
        if (tether) {
            PlayerEntity owner = ownerId == null ? null : world.getPlayerByUuid(ownerId);
            net.minecraft.entity.Entity t = targetId == null ? null : world.getEntity(targetId);
            if (owner == null || !(t instanceof LivingEntity target) || !target.isAlive()) {
                this.discard();
                return;
            }
            Vec3d anchor = owner.getPos().add(0, 1.1, 0);
            this.setPosition(anchor);
            reaim(anchor, target);
        }
        age++;
        if (age == strikeTick && damageAmt > 0f) applyHit(world);
        if (age >= lifeTicks) this.discard();
    }

    private void applyHit(ServerWorld world) {
        if (targetId == null) return;
        Entity t = world.getEntity(targetId);
        if (!(t instanceof LivingEntity target) || !target.isAlive()) return;
        PlayerEntity owner = ownerId == null ? null : world.getPlayerByUuid(ownerId);
        DamageSource src = owner != null ? owner.getDamageSources().playerAttack(owner)
                : this.getDamageSources().magic();
        target.damage(src, damageAmt);
        Vec3d d = dir();
        target.setVelocity(d.x * 0.2, Math.min(d.y * 0.25, 0.0), d.z * 0.2);
        target.velocityModified = true;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5, false, false)); // hit-stop
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "grow", 0, s -> {
            s.getController().setAnimation(GROW);
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "sway", 0, s -> {
            s.getController().setAnimation(SWAY);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
        if (nbt.containsUuid("Target")) this.targetId = nbt.getUuid("Target");
        if (nbt.containsUuid("Owner")) this.ownerId = nbt.getUuid("Owner");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        if (this.targetId != null) nbt.putUuid("Target", this.targetId);
        if (this.ownerId != null) nbt.putUuid("Owner", this.ownerId);
    }
}
