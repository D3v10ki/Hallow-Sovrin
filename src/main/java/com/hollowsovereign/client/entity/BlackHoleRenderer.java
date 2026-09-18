package com.hollowsovereign.client.entity;

import com.hollowsovereign.entity.BlackHoleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The black hole entity's renderer. Its GeckoLib model is now an empty anchor — the visible black
 * hole (event-horizon sphere + rim + accretion disc) is drawn entirely by the custom-shader meshes
 * {@link com.hollowsovereign.client.render.VoidCoreRenderer} and
 * {@link com.hollowsovereign.client.render.VoidDiscRenderer}.
 */
@Environment(EnvType.CLIENT)
public class BlackHoleRenderer extends GeoEntityRenderer<BlackHoleEntity> {
    public BlackHoleRenderer(EntityRendererFactory.Context context) {
        super(context, new BlackHoleModel());
        this.shadowRadius = 0f;
        // The whole black hole is now shader meshes (VoidCoreRenderer + VoidDiscRenderer). This
        // GeckoLib model is an empty anchor and draws nothing; the entity just marks the position.
    }

    @Override
    public RenderLayer getRenderType(BlackHoleEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutoutNoCull(texture);
    }
}
