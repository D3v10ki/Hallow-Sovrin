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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * The black hole's accretion disc, drawn with the same shader technique as the void floor but as its
 * OWN custom-shader mesh ({@code hollowsovereign:void_disc}) — a tilted annulus around the black-hole
 * entity, displaced by a rotating swirl (not distance ripples), with its own independent tint. Built
 * in disc-LOCAL coordinates (small, precise — the same coordinate fix as the floor), with the tilt +
 * world placement carried by the model-view matrix. Shares helper GLSL with the floor via moj_import.
 */
@Environment(EnvType.CLIENT)
public final class VoidDiscRenderer {
    private VoidDiscRenderer() {}

    public static final Identifier SHADER_ID = new Identifier(HollowSovereign.MOD_ID, "void_disc");
    private static final Identifier REFLECT = new Identifier(HollowSovereign.MOD_ID, "textures/effect/void_reflect.png");
    private static final long TIME_MOD = 100000L;
    private static final float TILT = 17f;
    private static final double INNER = 9.0, OUTER = 20.0;   // visible zone (must match shader uniforms)
    private static final double MESH_IN = 7.0, MESH_OUT = 21.0;

    private static ShaderProgram program;

    // Independent disc colour (default: hot orange, distinct from the purple floor).
    private static float tintR = 1.0f, tintG = 0.5f, tintB = 0.15f;

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
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> program);
        RenderSystem.setShaderTexture(0, REFLECT);
        setF("Time", now);
        setF("TintColor", tintR, tintG, tintB);

        int segs = 72;
        // One tilted annulus per black hole, each sized to its own scale (see VoidCoreRenderer).
        for (BlackHoleEntity bh : found) {
            Vec3d center = bh.getLerpedPos(ctx.tickDelta());
            float scale = bh.getScale();
            setF("InnerR", (float) (INNER * scale));   // shader falloff tracks the scaled mesh
            setF("OuterR", (float) (OUTER * scale));

            MatrixStack mv = RenderSystem.getModelViewStack();
            mv.push();
            mv.loadIdentity();
            mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());  // world -> view
            mv.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);        // to disc centre
            mv.multiply(RotationAxis.POSITIVE_X.rotationDegrees(TILT), 0f, 0f, 0f);     // tilt the plane
            RenderSystem.applyModelViewMatrix();

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            double step = scale; // keep ~one ring per unscaled block
            for (double r = MESH_IN * scale; r < MESH_OUT * scale; r += step) {
                double r1 = r + step;
                for (int s = 0; s < segs; s++) {
                    double a0 = (Math.PI * 2 * s) / segs, a1 = (Math.PI * 2 * (s + 1)) / segs;
                    discVert(bb, r, a0);
                    discVert(bb, r1, a0);
                    discVert(bb, r1, a1);
                    discVert(bb, r, a1);
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

    private static void discVert(BufferBuilder bb, double r, double ang) {
        double x = Math.cos(ang) * r, z = Math.sin(ang) * r; // disc-local (shader displaces Y)
        bb.vertex(x, 0.0, z).color(255, 255, 255, 255).texture(0f, 0f).next();
    }

    private static void setF(String name, float... v) {
        if (program == null) return;
        GlUniform u = program.getUniform(name);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 2 -> u.set(v[0], v[1]);
            case 3 -> u.set(v[0], v[1], v[2]);
            default -> { }
        }
    }
}
