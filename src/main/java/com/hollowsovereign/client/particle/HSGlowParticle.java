package com.hollowsovereign.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Generic full-bright, non-colliding billboard particle that fades out over its
 * lifetime. Used for glow, star and shard effects (texture comes from the factory).
 */
@Environment(EnvType.CLIENT)
public class HSGlowParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected HSGlowParticle(ClientWorld world, double x, double y, double z,
                             double vx, double vy, double vz,
                             float scale, int life, SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.spriteProvider = sprites;
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.baseScale = scale;
        this.scale = scale;
        this.maxAge = life;
        this.gravityStrength = 0.0f;
        this.collidesWithWorld = false;
        this.alpha = 1.0f;
        setSpriteForAge(sprites);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0; // full bright — energy glows regardless of light level
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            markDead();
            return;
        }
        // gentle drift decay so bursts ease out
        this.velocityX *= 0.90;
        this.velocityY *= 0.90;
        this.velocityZ *= 0.90;
        move(this.velocityX, this.velocityY, this.velocityZ);

        float life = (float) this.age / this.maxAge;
        this.alpha = 1.0f - life * life;
        this.scale = this.baseScale * (1.0f + 0.5f * life);
        setSpriteForAge(this.spriteProvider);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;
        private final float scale;
        private final int life;

        public Factory(SpriteProvider sprites, float scale, int life) {
            this.sprites = sprites;
            this.scale = scale;
            this.life = life;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new HSGlowParticle(world, x, y, z, vx, vy, vz, this.scale, this.life, this.sprites);
        }
    }
}
