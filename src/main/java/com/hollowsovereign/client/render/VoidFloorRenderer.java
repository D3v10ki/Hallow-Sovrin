package com.hollowsovereign.client.render;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.client.ClientState;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * The custom water-style void floor: its own core shader ({@code hollowsovereign:void_floor}) drawing
 * a generated, wave-displaced mesh over the domain floor within the domain radius. No particles, no
 * BlockState changes. All wave math runs in CENTRE-RELATIVE coordinates (world XZ minus the domain
 * centre, computed in double precision on the CPU and passed as small floats), so the pattern is
 * continuous everywhere and never loses precision at large world coordinates.
 */
@Environment(EnvType.CLIENT)
public final class VoidFloorRenderer {
    private VoidFloorRenderer() {}

    public static final Identifier SHADER_ID = new Identifier(HollowSovereign.MOD_ID, "void_floor");
    private static final Identifier REFLECT = new Identifier(HollowSovereign.MOD_ID, "textures/effect/void_reflect.png");
    private static final long TIME_MOD = 100000L;      // keep the shader clock small for float precision
    private static final float RIPPLE_LIFE = 1.6f;     // seconds; must match the shader

    private static ShaderProgram program;

    // Configurable void colour (drives glow/emissive/base). Default: void purple.
    private static float tintR = 0.55f, tintG = 0.30f, tintB = 0.95f;

    // Recent impacts, capped and stored CENTRE-RELATIVE: x, z, startSec, active.
    private static final int MAX_IMPACTS = 16;
    private static final float[] impX = new float[MAX_IMPACTS];
    private static final float[] impZ = new float[MAX_IMPACTS];
    private static final float[] impStart = new float[MAX_IMPACTS];
    private static final boolean[] impActive = new boolean[MAX_IMPACTS];

    // Per-entity step throttle: entityId -> {lastTick, lastWorldX, lastWorldZ}.
    private static final Map<Integer, double[]> lastStep = new HashMap<>();
    private static final int STEP_TICKS = 6;      // min ticks between an entity's impacts
    private static final double STEP_DIST = 0.7;  // min blocks moved before a new footstep counts
    private static double lastCx = Double.NaN, lastCz = Double.NaN;

    public static void setProgram(ShaderProgram p) { program = p; }

    /** Set the void colour (0..1). Feeds the TintColor uniform each frame. */
    public static void setTint(float r, float g, float b) { tintR = r; tintG = g; tintB = b; }

    private static float nowSec(MinecraftClient mc) {
        return (mc.world.getTime() % TIME_MOD) / 20.0f;
    }

    /** Record an impact (world coords) the floor should ripple from — footstep, consume, projectile. */
    public static void addImpact(double worldX, double worldZ, float startSec) {
        float rx = (float) (worldX - ClientState.domainCx);
        float rz = (float) (worldZ - ClientState.domainCz);
        int slot = -1;
        float oldest = Float.MAX_VALUE;
        for (int i = 0; i < MAX_IMPACTS; i++) {
            if (!impActive[i]) { slot = i; break; }          // reuse a free slot first
            if (impStart[i] < oldest) { oldest = impStart[i]; slot = i; }  // else evict the OLDEST
        }
        impX[slot] = rx;
        impZ[slot] = rz;
        impStart[slot] = startSec;
        impActive[slot] = true;
    }

    /** Ripple the floor at world XZ using the current shader clock (in-domain Gravity Snare synergy). */
    public static void rippleNow(double worldX, double worldZ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        addImpact(worldX, worldZ, nowSec(mc));
    }

    /** Feed impacts from entities that actually STEP (moved) on the domain floor. Call while in domain. */
    public static void trackImpacts(MinecraftClient mc) {
        if (mc.world == null) return;
        double cx = ClientState.domainCx, cz = ClientState.domainCz;

        // New/moved domain: wipe stale state so nothing lingers from a previous cast.
        if (Double.isNaN(lastCx) || Math.abs(cx - lastCx) > 2 || Math.abs(cz - lastCz) > 2) {
            reset();
            lastCx = cx; lastCz = cz;
        }
        if (lastStep.size() > 128) lastStep.clear();

        int r = ClientState.domainRadius;
        long tick = mc.world.getTime();
        float now = nowSec(mc);
        Box box = new Box(cx - r, ClientState.domainCy - 3, cz - r, cx + r, ClientState.domainCy + 6, cz + r);
        for (LivingEntity e : mc.world.getEntitiesByClass(LivingEntity.class, box, x -> true)) {
            if (!e.isOnGround()) continue;
            double dx = e.getX() - cx, dz = e.getZ() - cz;
            if (dx * dx + dz * dz > (double) r * r) continue;

            double[] prev = lastStep.get(e.getId());
            if (prev != null) {
                double md = (e.getX() - prev[1]) * (e.getX() - prev[1]) + (e.getZ() - prev[2]) * (e.getZ() - prev[2]);
                if (tick - (long) prev[0] < STEP_TICKS || md < STEP_DIST * STEP_DIST) continue;
            }
            addImpact(e.getX(), e.getZ(), now);
            lastStep.put(e.getId(), new double[]{tick, e.getX(), e.getZ()});
        }
    }

    private static void reset() {
        for (int i = 0; i < MAX_IMPACTS; i++) impActive[i] = false;
        lastStep.clear();
    }

    private static void expire(float now) {
        for (int i = 0; i < MAX_IMPACTS; i++) {
            if (impActive[i] && now - impStart[i] > RIPPLE_LIFE) impActive[i] = false;
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (program == null || !ClientState.inDomain()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        double cx = ClientState.domainCx, cz = ClientState.domainCz;
        int r = ClientState.domainRadius;
        double floorY = Math.floor(ClientState.domainCy) + 0.02;
        Vec3d cam = ctx.camera().getPos();
        float now = nowSec(mc) + ctx.tickDelta() / 20.0f;
        expire(now);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> program);
        RenderSystem.setShaderTexture(0, REFLECT);

        setF("Time", now);
        setF("Radius", (float) r);
        setF("TintColor", tintR, tintG, tintB);
        for (int i = 0; i < MAX_IMPACTS; i++) {
            setF("Impact" + i, impX[i], impZ[i], impStart[i], impActive[i] ? 1.0f : 0.0f);
        }

        MatrixStack mv = RenderSystem.getModelViewStack();
        mv.push();
        mv.loadIdentity();
        mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        int x0 = MathHelper.floor(cx - r), x1 = MathHelper.ceil(cx + r);
        int z0 = MathHelper.floor(cz - r), z1 = MathHelper.ceil(cz + r);
        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                double mxd = (x + 0.5) - cx, mzd = (z + 0.5) - cz;
                if (mxd * mxd + mzd * mzd > (double) (r + 1) * (r + 1)) continue;
                vert(bb, cam, x, floorY, z, cx, cz, r);
                vert(bb, cam, x, floorY, z + 1, cx, cz, r);
                vert(bb, cam, x + 1, floorY, z + 1, cx, cz, r);
                vert(bb, cam, x + 1, floorY, z, cx, cz, r);
            }
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());

        mv.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vert(BufferBuilder bb, Vec3d cam, double wx, double wy, double wz,
                             double cx, double cz, int r) {
        double relX = wx - cx, relZ = wz - cz;                 // centre-relative (double, precise)
        double d = Math.sqrt(relX * relX + relZ * relZ);
        float f = (float) MathHelper.clamp(1.0 - d / r, 0.0, 1.0);
        f = f * f * (3f - 2f * f);                             // smoothstep for a soft rim
        int a = (int) (f * 255);
        bb.vertex(wx - cam.x, wy - cam.y, wz - cam.z)          // camera-relative for gl_Position
                .color(255, 255, 255, a)
                .texture((float) relX, (float) relZ)           // UV0 carries centre-relative XZ
                .next();
    }

    private static void setF(String name, float... v) {
        if (program == null) return;
        GlUniform u = program.getUniform(name);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 2 -> u.set(v[0], v[1]);
            case 3 -> u.set(v[0], v[1], v[2]);
            case 4 -> u.set(v[0], v[1], v[2], v[3]);
            default -> { }
        }
    }
}
