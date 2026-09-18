package com.hollowsovereign.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The teleport / dash afterimage trail. When a Void Weaver blinks or dashes, the server sends a trail
 * packet; the client spawns a set of render-only "ghosts" spaced along the travel line, each rendering
 * the caster's actual player model + skin at the rotation captured at cast, tinted void-purple and
 * fading out. There is no entity and no collision — the "travelled fast" illusion is entirely the
 * pre-spaced, gradient-faded trail. Ghosts nearer the destination are brighter and linger slightly
 * longer, with a small per-ghost timing stagger so they don't all vanish on one frame.
 *
 * NOTE (unverifiable in a headless build): the pose block below mirrors vanilla LivingEntityRenderer's
 * transform order (180-bodyYaw yaw, scale(-1,-1,1), player 0.9375 scale, -1.501 feet offset). If a
 * ghost renders upside-down, mirrored, or floating/sunk, those four constants are the knobs to tweak —
 * same kind of orientation caveat as the tendril model.
 */
@Environment(EnvType.CLIENT)
public final class VoidGhostRenderer {
    private VoidGhostRenderer() {}

    // Void purple multiply applied over the model.
    private static final float TINT_R = 0.62f, TINT_G = 0.35f, TINT_B = 1.0f;

    private static final List<Ghost> GHOSTS = new ArrayList<>();

    private static final class Ghost {
        final int playerId;
        final Vec3d pos;
        final float bodyYaw, headYaw, pitch;
        final long spawnMs, lifeMs;
        final float startAlpha;
        Ghost(int playerId, Vec3d pos, float bodyYaw, float headYaw, float pitch,
              long spawnMs, long lifeMs, float startAlpha) {
            this.playerId = playerId; this.pos = pos;
            this.bodyYaw = bodyYaw; this.headYaw = headYaw; this.pitch = pitch;
            this.spawnMs = spawnMs; this.lifeMs = lifeMs; this.startAlpha = startAlpha;
        }
    }

    /** Client: lay down {@code count} ghosts along from -> to (call on the render thread). */
    public static void addTrail(int playerId, Vec3d from, Vec3d to, int count,
                                float bodyYaw, float headYaw, float pitch, int lifeTicks) {
        long now = System.currentTimeMillis();
        long baseLife = Math.max(150L, lifeTicks * 50L); // ~0.25-0.4s per the spec
        int n = Math.max(1, count);
        for (int i = 0; i < n; i++) {
            float f = n == 1 ? 1f : (float) i / (n - 1);   // 0 at start, 1 at destination
            Vec3d p = from.add(to.subtract(from).multiply(f));
            float startAlpha = Math.min(0.75f, 0.22f + 0.5f * f); // nearer destination = brighter
            long life = baseLife + (long) (i * 22);              // slight stagger
            GHOSTS.add(new Ghost(playerId, p, bodyYaw, headYaw, pitch, now, life, startAlpha));
        }
        if (GHOSTS.size() > 256) GHOSTS.subList(0, GHOSTS.size() - 256).clear(); // safety cap
    }

    public static void render(WorldRenderContext ctx) {
        if (GHOSTS.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        Vec3d cam = ctx.camera().getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        boolean drewAny = false;

        for (Iterator<Ghost> it = GHOSTS.iterator(); it.hasNext(); ) {
            Ghost g = it.next();
            float life = (now - g.spawnMs) / (float) g.lifeMs;
            if (life >= 1f) { it.remove(); continue; }

            Entity e = mc.world.getEntityById(g.playerId);
            if (!(e instanceof AbstractClientPlayerEntity p)) continue;
            EntityRenderer<?> r = mc.getEntityRenderDispatcher().getRenderer(p);
            if (!(r instanceof PlayerEntityRenderer per)) continue;

            float alpha = g.startAlpha * (1f - life);
            if (alpha <= 0.02f) continue;

            PlayerEntityModel<AbstractClientPlayerEntity> model = per.getModel();
            Identifier skin = p.getSkinTexture();

            model.child = false;
            model.riding = false;
            model.handSwingProgress = 0f;
            model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
            model.rightArmPose = BipedEntityModel.ArmPose.EMPTY;
            float netHead = MathHelper.wrapDegrees(g.headYaw - g.bodyYaw);
            model.setAngles(p, 0f, 0f, (float) (mc.world.getTime()), netHead, g.pitch);

            matrices.push();
            matrices.translate(g.pos.x - cam.x, g.pos.y - cam.y, g.pos.z - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - g.bodyYaw));
            matrices.scale(-1f, -1f, 1f);
            matrices.scale(0.9375f, 0.9375f, 0.9375f);
            matrices.translate(0f, -1.501f, 0f);
            VertexConsumer vc = imm.getBuffer(RenderLayer.getEntityTranslucent(skin));
            model.render(matrices, vc, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    OverlayTexture.DEFAULT_UV, TINT_R, TINT_G, TINT_B, alpha);
            matrices.pop();
            drewAny = true;
        }

        if (drewAny) imm.draw(); // flush our translucent ghosts in this pass
    }

    public static void clear() { GHOSTS.clear(); }
}
