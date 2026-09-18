package com.hollowsovereign.client.entity;

import com.hollowsovereign.entity.GravityOrbEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class GravityOrbRenderer extends GeoEntityRenderer<GravityOrbEntity> {
    public GravityOrbRenderer(EntityRendererFactory.Context context) {
        super(context, new GravityOrbModel());
        this.shadowRadius = 0f;
    }

    @Override
    public RenderLayer getRenderType(GravityOrbEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        // Translucent (NOT emissive): the dark core must actually render dark so it
        // reads as a black hole; the bright accretion disk still blooms under shaders.
        return RenderLayer.getEntityTranslucent(texture);
    }
}
