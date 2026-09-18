package com.hollowsovereign.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Short-lived "void rupture" shockwave rings — a camera-facing annulus that expands outward and fades,
 * spawned at the departure AND arrival points of a Blink so each end reads as a small tear in space.
 * Intentionally a lightweight additive vertex-colour mesh (not a bespoke shader): a flat ring has no
 * meaningful surface normal, so the black hole's per-pixel fresnel wouldn't add anything here, and this
 * keeps it reliable. Purple, matching the kit palette.
 */
@Environment(EnvType.CLIENT)
public final class VoidRingRenderer {
    private VoidRingRenderer() {}

    private static final int SEGMENTS = 40;
    private static final List<Ring> RINGS = new ArrayList<>();

    private static final class Ring {
        final Vec3d center;
        final long startMs;
        final long lifeMs;
        final float maxRadius;
        Ring(Vec3d center, float maxRadius, long lifeMs) {
            this.center = center; this.maxRadius = maxRadius; this.startMs = System.currentTimeMillis(); this.lifeMs = lifeMs;
        }
    }

    /** Client: spawn an expanding rupture ring centred at {@code center}. */
    public static void addRing(Vec3d center, float maxRadius, int lifeTicks) {
        RINGS.add(new Ring(center, maxRadius, Math.max(150L, lifeTicks * 50L)));
        if (RINGS.size() > 64) RINGS.subList(0, RINGS.size() - 64).clear();
    }

    public static void render(WorldRenderContext ctx) {
        if (RINGS.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        RINGS.removeIf(r -> (now - r.startMs) >= r.lifeMs);   // purge before any GL work
        if (RINGS.isEmpty()) return;

        Vec3d cam = ctx.camera().getPos();

        // Camera-facing basis from yaw/pitch (billboards the flat ring toward the viewer).
        double yr = Math.toRadians(ctx.camera().getYaw());
        double pr = Math.toRadians(ctx.camera().getPitch());
        Vec3d fwd = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3d right = fwd.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : right.normalize();
        Vec3d up = right.crossProduct(fwd).normalize();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE); // additive glow
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        MatrixStack mv = RenderSystem.getModelViewStack();
        mv.push();
        mv.loadIdentity();
        mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Ring r : RINGS) {
            float t = (now - r.startMs) / (float) r.lifeMs;
            float ease = 1f - (1f - t) * (1f - t);              // ease-out expansion
            float outer = r.maxRadius * (0.15f + 0.85f * ease);
            float inner = outer * 0.72f;
            int alpha = (int) (Math.max(0f, 1f - t) * 200);
            for (int s = 0; s < SEGMENTS; s++) {
                double a0 = (Math.PI * 2 * s) / SEGMENTS, a1 = (Math.PI * 2 * (s + 1)) / SEGMENTS;
                ringVert(bb, cam, r.center, right, up, inner, a0, alpha);
                ringVert(bb, cam, r.center, right, up, outer, a0, alpha);
                ringVert(bb, cam, r.center, right, up, outer, a1, alpha);
                ringVert(bb, cam, r.center, right, up, inner, a1, alpha);
            }
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());

        mv.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void ringVert(BufferBuilder bb, Vec3d cam, Vec3d c, Vec3d right, Vec3d up,
                                 double radius, double ang, int alpha) {
        double cos = Math.cos(ang) * radius, sin = Math.sin(ang) * radius;
        double wx = c.x + right.x * cos + up.x * sin;
        double wy = c.y + right.y * cos + up.y * sin;
        double wz = c.z + right.z * cos + up.z * sin;
        bb.vertex(wx - cam.x, wy - cam.y, wz - cam.z).color(158, 90, 255, alpha).next();
    }
}
