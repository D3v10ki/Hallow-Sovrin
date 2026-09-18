package com.hollowsovereign.client.render;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.entity.BlackHoleEntity;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * The black hole's event-horizon sphere, drawn as its own custom-shader mesh ({@code void_core})
 * instead of GeckoLib geometry. A UV-sphere mesh (sphere-LOCAL coords, small + precise) is displaced
 * by 3D noise (`hs_sphereNoise` in void_common) for organic surface distortion, and the rim/halo glow
 * comes from a fresnel term in the fragment shader — no separate render layer. Rendered opaque with
 * depth-write BEFORE the disc, so it occludes the disc's far side. Its own independent tint.
 */
@Environment(EnvType.CLIENT)
public final class VoidCoreRenderer {
    private VoidCoreRenderer() {}

    public static final Identifier SHADER_ID = new Identifier(HollowSovereign.MOD_ID, "void_core");
    private static final long TIME_MOD = 100000L;
    private static final float RADIUS = 6.0f;
    private static final int LAT = 18, LON = 28;

    private static ShaderProgram program;
    private static float tintR = 0.60f, tintG = 0.35f, tintB = 1.0f; // core rim tint (default violet)

    public static void setProgram(ShaderProgram p) { program = p; }

    public static void setTint(float r, float g, float b) { tintR = r; tintG = g; tintB = b; }

    public static void render(WorldRenderContext ctx) {
        if (program == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Box search = mc.player.getBoundingBox().expand(80);
        List<BlackHoleEntity> found = mc.world.getEntitiesByClass(BlackHoleEntity.class, search, e -> true);
        if (found.isEmpty()) return;

        Vec3d cam = ctx.camera().getPos();
        float now = (mc.world.getTime() % TIME_MOD) / 20.0f + ctx.tickDelta() / 20.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);          // opaque core writes depth to occlude the disc
        RenderSystem.enableCull();
        RenderSystem.setShader(() -> program);
        setF("TintColor", tintR, tintG, tintB);
        setF("Time", now);

        // Every black hole in range gets its own draw at its own scale (domain-size = 1.0, the
        // Singularity Grasp combat orb a fraction of that), so both can render at once.
        for (BlackHoleEntity bh : found) {
            Vec3d center = bh.getLerpedPos(ctx.tickDelta());
            float radius = RADIUS * bh.getScale();
            setF("Radius", radius);            // shader falloff must track the mesh size

            MatrixStack mv = RenderSystem.getModelViewStack();
            mv.push();
            mv.loadIdentity();
            mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());
            mv.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);
            RenderSystem.applyModelViewMatrix();

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            for (int i = 0; i < LAT; i++) {
                double t0 = Math.PI * i / LAT, t1 = Math.PI * (i + 1) / LAT;
                for (int j = 0; j < LON; j++) {
                    double p0 = 2 * Math.PI * j / LON, p1 = 2 * Math.PI * (j + 1) / LON;
                    sphVert(bb, t0, p0, radius);
                    sphVert(bb, t1, p0, radius);
                    sphVert(bb, t1, p1, radius);
                    sphVert(bb, t0, p1, radius);
                }
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());

            mv.pop();
        }
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableBlend();
    }

    private static void sphVert(BufferBuilder bb, double theta, double phi, float radius) {
        double sx = Math.sin(theta) * Math.cos(phi);
        double sy = Math.cos(theta);
        double sz = Math.sin(theta) * Math.sin(phi);
        bb.vertex(sx * radius, sy * radius, sz * radius).color(255, 255, 255, 255).texture(0f, 0f).next();
    }

    private static void setF(String name, float... v) {
        if (program == null) return;
        GlUniform u = program.getUniform(name);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 3 -> u.set(v[0], v[1], v[2]);
            default -> { }
        }
    }
}
