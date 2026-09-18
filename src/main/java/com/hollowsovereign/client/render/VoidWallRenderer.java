package com.hollowsovereign.client.render;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.client.ClientState;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Replaces the look of the domain walls with a static space/starfield backdrop: a fullbright (unlit,
 * via the vanilla position_color_tex shader) hemisphere mesh sitting just inside the dome wall
 * surface, so the black-concrete walls read as a fixed field of stars. NO vertex displacement, NO UV
 * scrolling — completely still. Each quad gets a one-time UV rotation from a position hash so the
 * texture doesn't tile in an obvious grid. This is an overlay mesh, like the floor — no BlockStates
 * are changed (there is no block-texture-swap system; the walls stay black concrete underneath).
 */
@Environment(EnvType.CLIENT)
public final class VoidWallRenderer {
    private VoidWallRenderer() {}

    private static final Identifier STARFIELD = new Identifier(HollowSovereign.MOD_ID, "textures/effect/starfield.png");
    private static final int LAT = 24, LON = 48;

    // Four 90°-rotated UV corner assignments; picked per quad by a position hash.
    private static final float[][][] UVROT = {
            {{0, 0}, {1, 0}, {1, 1}, {0, 1}},
            {{0, 1}, {0, 0}, {1, 0}, {1, 1}},
            {{1, 1}, {0, 1}, {0, 0}, {1, 0}},
            {{1, 0}, {1, 1}, {0, 1}, {0, 0}},
    };

    public static void render(WorldRenderContext ctx) {
        if (!ClientState.inDomain()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        double cx = ClientState.domainCx, cy = ClientState.domainCy, cz = ClientState.domainCz;
        double rr = ClientState.domainRadius - 0.6; // just inside the wall face
        Vec3d cam = ctx.camera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.setShaderTexture(0, STARFIELD);

        MatrixStack mv = RenderSystem.getModelViewStack();
        mv.push();
        mv.loadIdentity();
        mv.multiplyPositionMatrix(ctx.matrixStack().peek().getPositionMatrix());
        mv.translate(cx - cam.x, cy - cam.y, cz - cam.z); // dome centre, camera-relative
        RenderSystem.applyModelViewMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        for (int i = 0; i < LAT; i++) {
            double th0 = Math.PI / 2 * i / LAT, th1 = Math.PI / 2 * (i + 1) / LAT; // 0=top .. 90=base
            for (int j = 0; j < LON; j++) {
                double ph0 = 2 * Math.PI * j / LON, ph1 = 2 * Math.PI * (j + 1) / LON;
                float[][] uv = UVROT[hash(i, j) & 3];
                v(bb, rr, th0, ph0, uv[0]);
                v(bb, rr, th1, ph0, uv[1]);
                v(bb, rr, th1, ph1, uv[2]);
                v(bb, rr, th0, ph1, uv[3]);
            }
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());

        mv.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static int hash(int i, int j) {
        return (i * 73856093) ^ (j * 19349663);
    }

    private static void v(BufferBuilder bb, double r, double theta, double phi, float[] uv) {
        double x = r * Math.sin(theta) * Math.cos(phi);
        double y = r * Math.cos(theta);
        double z = r * Math.sin(theta) * Math.sin(phi);
        bb.vertex(x, y, z).color(255, 255, 255, 255).texture(uv[0], uv[1]).next();
    }
}
