package com.hollowsovereign.client.entity;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.entity.TendrilEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the tendril bone-chain. The model is authored along +Y at a fixed nominal length; here we
 * rotate that axis onto the aim direction and non-uniformly scale it (thick sideways, length to the
 * anchor→target distance) so one model serves any distance/orientation. A black cutout base plus a
 * gold-vein {@link EmissiveGeoLayer} give the black-and-gold look.
 * NOTE: orientation fights GeckoLib's default entity transforms — the most likely thing to need a
 * sign/flip tweak after an in-game check.
 */
@Environment(EnvType.CLIENT)
public class TendrilEntityRenderer extends GeoEntityRenderer<TendrilEntity> {
    private static final Identifier VEINS = new Identifier(HollowSovereign.MOD_ID, "textures/entity/tendril_veins.png");
    private static final float MODEL_LEN_BLOCKS = 24f / 16f; // model spans 24 units = 1.5 blocks

    public TendrilEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new TendrilModel());
        this.shadowRadius = 0f;
        addRenderLayer(new EmissiveGeoLayer<>(this, VEINS, false)); // glowing gold veins
    }

    @Override
    public void preRender(MatrixStack poseStack, TendrilEntity animatable, BakedGeoModel bakedModel,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        if (!isReRender) {
            Vec3d d = animatable.dir();
            Quaternionf q = new Quaternionf().rotationTo(0f, 1f, 0f, (float) d.x, (float) d.y, (float) d.z);
            poseStack.multiply(q, 0f, 0f, 0f);
            float lengthScale = Math.max(0.1f, animatable.length() / MODEL_LEN_BLOCKS);
            float thick = animatable.thickness();
            poseStack.scale(thick, lengthScale, thick);
        }
        super.preRender(poseStack, animatable, bakedModel, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public RenderLayer getRenderType(TendrilEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityCutoutNoCull(texture); // solid black base material
    }
}
