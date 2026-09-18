package com.hollowsovereign.client.entity;

import com.hollowsovereign.entity.BlackHoleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Re-renders the whole black-hole model with an emissive render type but a texture that is only
 * painted where this layer should appear (rim texels, or the swirl-disc region) — everything else in
 * that texture is transparent, so only the intended part glows. The base cutout pass already drew the
 * opaque, diffuse-shaded core into the depth buffer, so these emissive passes are correctly occluded
 * by it. Used twice: a pulsing rim and a steady, semi-transparent accretion disc.
 */
@Environment(EnvType.CLIENT)
public class BlackHoleOverlayLayer extends GeoRenderLayer<BlackHoleEntity> {
    private static final int FULLBRIGHT = 0xF000F0;
    private final Identifier texture;
    private final boolean pulse;

    public BlackHoleOverlayLayer(GeoRenderer<BlackHoleEntity> renderer, Identifier texture, boolean pulse) {
        super(renderer);
        this.texture = texture;
        this.pulse = pulse;
    }

    @Override
    public void render(MatrixStack poseStack, BlackHoleEntity animatable, BakedGeoModel bakedModel,
                       RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        float t = animatable.getWorld().getTime() + partialTick;
        float alpha = pulse ? 0.55f + 0.45f * (float) Math.sin(t * 0.15f) : 1.0f;
        RenderLayer emissive = RenderLayer.getEntityTranslucentEmissive(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissive,
                bufferSource.getBuffer(emissive), partialTick, FULLBRIGHT, OverlayTexture.DEFAULT_UV,
                1f, 1f, 1f, alpha);
    }
}
