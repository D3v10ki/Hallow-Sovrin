package com.hollowsovereign.client.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Reusable emissive overlay for a GeoEntity: re-renders the model with an emissive render type and a
 * second texture that is transparent except where it should glow (e.g. the tendril's gold veins on
 * black, or the black hole's rim). Same technique used across the mod's glowing accents.
 */
@Environment(EnvType.CLIENT)
public class EmissiveGeoLayer<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T> {
    private static final int FULLBRIGHT = 0xF000F0;
    private final Identifier texture;
    private final boolean pulse;

    public EmissiveGeoLayer(GeoRenderer<T> renderer, Identifier texture, boolean pulse) {
        super(renderer);
        this.texture = texture;
        this.pulse = pulse;
    }

    @Override
    public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        float alpha = pulse
                ? 0.6f + 0.4f * (float) Math.sin((animatable.getWorld().getTime() + partialTick) * 0.15f)
                : 1.0f;
        RenderLayer emissive = RenderLayer.getEntityTranslucentEmissive(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissive,
                bufferSource.getBuffer(emissive), partialTick, FULLBRIGHT, OverlayTexture.DEFAULT_UV,
                1f, 1f, 1f, alpha);
    }
}
