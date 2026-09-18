package com.hollowsovereign.client.render;

import com.hollowsovereign.HollowSovereign;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Void Ward's barrier dome: an emissive, translucent fresnel sphere around each player with an active
 * ward (same rim technique as the black-hole core, its own {@code void_ward} shader). A small impact
 * buffer (4 slots) flares + bulges the dome where an enemy meleed it — the sphere-space cousin of the
 * void-water floor's ripple buffer. Wards are tracked client-side by player entity id (fed by the
 * WARD_STATE / WARD_IMPACT packets), so co-op partners see each other's domes too.
 */
@Environment(EnvType.CLIENT)
public final class VoidWardRenderer {
    private VoidWardRenderer() {}

    public static final Identifier SHADER_ID = new Identifier(HollowSovereign.MOD_ID, "void_ward");
    private static final long TIME_MOD = 100000L;
    private static final float RADIUS = 1.6f;
    private static final int LAT = 16, LON = 24;
    private static final int SLOTS = 4;

    private static ShaderProgram program;
    private static float tintR = 0.60f, tintG = 0.35f, tintB = 1.0f; // void purple

    public static void setProgram(ShaderProgram p) { program = p; }

    private static final class Ward {
        long endMs;
        final float[] dir = new float[SLOTS * 3];   // xyz per slot
        final float[] start = new float[SLOTS];      // shader-clock seconds
        final boolean[] active = new boolean[SLOTS];
        int next = 0;
    }

    private static final Map<Integer, Ward> WARDS = new HashMap<>();

    /** Client: a player's ward is (re)active for durationTicks. */
    public static void addWard(int playerId, int durationTicks) {
        Ward w = WARDS.computeIfAbsent(playerId, k -> new Ward());
        w.endMs = System.currentTimeMillis() + Math.max(150L, durationTicks * 50L);
    }

    /** Client: a melee hit landed on the dome from unit direction (dx,dy,dz) relative to the player. */
    public static void addImpact(int playerId, float dx, float dy, float dz) {
        Ward w = WARDS.get(playerId);
        if (w == null) return;
        int s = w.next % SLOTS;
        w.next++;
        w.dir[s * 3] = dx; w.dir[s * 3 + 1] = dy; w.dir[s * 3 + 2] = dz;
        w.start[s] = nowSec();
        w.active[s] = true;
    }

    private static float nowSec() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world == null ? 0f : (mc.world.getTime() % TIME_MOD) / 20.0f;
    }

    public static void render(WorldRenderContext ctx) {
        if (program == null || WARDS.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        Vec3d cam = ctx.camera().getPos();
        float time = (mc.world.getTime() % TIME_MOD) / 20.0f + ctx.tickDelta() / 20.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);        // translucent dome, don't occlude the player inside
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> program);
        setF("Time", time);
        setF("Radius", RADIUS);
        setF("TintColor", tintR, tintG, tintB);

        for (Iterator<Map.Entry<Integer, Ward>> it = WARDS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Ward> en = it.next();
            Ward w = en.getValue();
            if (now > w.endMs) { it.remove(); continue; }
            Entity e = mc.world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le)) continue;

            Vec3d center = le.getLerpedPos(ctx.tickDelta()).add(0, le.getHeight() * 0.5, 0);
            for (int i = 0; i < SLOTS; i++) {
                if (w.active[i] && time - w.start[i] > 0.7f) w.active[i] = false;
                if (w.active[i]) {
                    setF("Impact" + i, w.dir[i * 3], w.dir[i * 3 + 1], w.dir[i * 3 + 2], w.start[i]);
                } else {
                    setF("Impact" + i, 0f, 0f, 0f, 0f);
                }
            }

            MatrixStack mv = RenderSystem.getModelViewStack();
            mv.push();
            mv.loadIdentity();
            mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());
            mv.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);
            RenderSystem.applyModelViewMatrix();

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            for (int a = 0; a < LAT; a++) {
                double t0 = Math.PI * a / LAT, t1 = Math.PI * (a + 1) / LAT;
                for (int b = 0; b < LON; b++) {
                    double p0 = 2 * Math.PI * b / LON, p1 = 2 * Math.PI * (b + 1) / LON;
                    sphVert(bb, t0, p0);
                    sphVert(bb, t1, p0);
                    sphVert(bb, t1, p1);
                    sphVert(bb, t0, p1);
                }
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());
            mv.pop();
        }
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void sphVert(BufferBuilder bb, double theta, double phi) {
        double sx = Math.sin(theta) * Math.cos(phi);
        double sy = Math.cos(theta);
        double sz = Math.sin(theta) * Math.sin(phi);
        bb.vertex(sx * RADIUS, sy * RADIUS, sz * RADIUS).color(255, 255, 255, 255).texture(0f, 0f).next();
    }

    private static void setF(String name, float... v) {
        if (program == null) return;
        GlUniform u = program.getUniform(name);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 3 -> u.set(v[0], v[1], v[2]);
            case 4 -> u.set(v[0], v[1], v[2], v[3]);
            default -> { }
        }
    }
}
