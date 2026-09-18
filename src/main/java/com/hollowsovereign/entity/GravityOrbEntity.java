package com.hollowsovereign.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import com.hollowsovereign.ability.VoidFx;
import com.hollowsovereign.particle.HSParticles;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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
 * Void Weaver "Gravity Well" — a short-lived floating construct that renders an
 * animated GeckoLib model and pulls nearby enemies toward it.
 */
public class GravityOrbEntity extends Entity implements GeoEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private static final int LIFE = 60;
    private static final double RADIUS = 8.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;
    private UUID ownerUuid;

    public GravityOrbEntity(EntityType<? extends GravityOrbEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public void setOwner(PlayerEntity player) {
        this.ownerUuid = player.getUuid();
    }

    @Override
    protected void initDataTracker() {
        // no synced data needed
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(Vec3d.ZERO);
        this.setNoGravity(true);
        if (!this.getWorld().isClient) {
            ServerWorld sw = (ServerWorld) this.getWorld();
            pull(sw);
            spawnAccretionFx(sw);
            if (++age >= LIFE) {
                this.discard();
            }
        }
    }

    /** Matter spiralling inward toward the singularity. */
    private void spawnAccretionFx(ServerWorld world) {
        Vec3d c = this.getPos().add(0, 0.5, 0);
        double phase = age * 0.55;
        for (int i = 0; i < 3; i++) {
            double a = phase + i * 2.094;
            double r = 3.2 - (age % 24) / 24.0 * 2.6;
            Vec3d p = c.add(Math.cos(a) * r, Math.sin(age * 0.3 + i) * 0.4, Math.sin(a) * r);
            Vec3d v = c.subtract(p).normalize().multiply(0.18);
            VoidFx.mote(world, HSParticles.VOID_STAR, p, v);
        }
        VoidFx.dust(world, VoidFx.PURPLE, 1.1f, c, 2, 0.4);
    }

    private void pull(ServerWorld world) {
        Vec3d c = this.getPos();
        Box box = new Box(c.subtract(RADIUS, RADIUS, RADIUS), c.add(RADIUS, RADIUS, RADIUS));
        PlayerEntity owner = ownerUuid == null ? null : world.getPlayerByUuid(ownerUuid);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box,
                x -> x.isAlive() && !(x instanceof PlayerEntity) && x.squaredDistanceTo(c) <= RADIUS * RADIUS)) {
            Vec3d dir = c.subtract(e.getPos());
            if (dir.lengthSquared() < 0.04) continue;
            Vec3d pull = dir.normalize().multiply(0.5);
            e.setVelocity(pull.x, Math.max(-0.1, pull.y * 0.4), pull.z);
            e.velocityModified = true;
            if (age % 12 == 0) {
                if (owner != null) {
                    e.damage(owner.getDamageSources().playerAttack(owner), 2.0f);
                } else {
                    e.damage(this.getDamageSources().magic(), 2.0f);
                }
            }
        }
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0, state -> {
            state.getController().setAnimation(SPIN);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- transient entity: minimal persistence ---
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
        if (nbt.containsUuid("Owner")) this.ownerUuid = nbt.getUuid("Owner");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        if (this.ownerUuid != null) nbt.putUuid("Owner", this.ownerUuid);
    }
}
