package com.hollowsovereign.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Cosmetic centrepiece for the Starless Expanse domain: a big GeckoLib black hole — a round event
 * horizon with a spinning accretion disc. It only hovers and spins; the {@code DomainInstance} that
 * spawns it owns its lifetime and discards it on close. No pull or damage (that's the Gravity Well).
 */
public class BlackHoleEntity extends Entity implements GeoEntity {
    private static final int MAX_LIFE = 6200; // safety net if the domain ever fails to clean it up

    /** Visual scale of the model, synced to clients. 1.0 = full domain size; the Singularity Grasp
     *  combat ability spawns one at a fraction of this so the same core+disc shaders read smaller. */
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(BlackHoleEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age = 0;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        // The model is scaled far larger than the tiny hitbox, so skip frustum culling —
        // otherwise the black hole vanishes whenever its centre point leaves the screen.
        this.ignoreCameraFrustum = true;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(SCALE, 1.0f);
    }

    /** Set before/at spawn; the domain leaves it at 1.0, Singularity Grasp shrinks it. */
    public void setScale(float scale) { this.dataTracker.set(SCALE, scale); }

    public float getScale() { return this.dataTracker.get(SCALE); }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(Vec3d.ZERO);
        this.setNoGravity(true);
        if (!this.getWorld().isClient && ++age >= MAX_LIFE) {
            this.discard();
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No GeckoLib animation needed: the core sphere + rim are static (the rim pulses via its
        // render layer), and the accretion disc is now a separate custom-shader mesh (void_disc).
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
    }
}
