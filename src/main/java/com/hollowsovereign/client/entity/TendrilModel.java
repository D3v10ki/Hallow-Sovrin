package com.hollowsovereign.client.entity;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.entity.TendrilEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

@Environment(EnvType.CLIENT)
public class TendrilModel extends GeoModel<TendrilEntity> {
    private static final Identifier MODEL = new Identifier(HollowSovereign.MOD_ID, "geo/tendril.geo.json");
    private static final Identifier TEXTURE = new Identifier(HollowSovereign.MOD_ID, "textures/entity/tendril.png");
    private static final Identifier ANIMATION = new Identifier(HollowSovereign.MOD_ID, "animations/tendril.animation.json");

    @Override public Identifier getModelResource(TendrilEntity animatable) { return MODEL; }
    @Override public Identifier getTextureResource(TendrilEntity animatable) { return TEXTURE; }
    @Override public Identifier getAnimationResource(TendrilEntity animatable) { return ANIMATION; }
}
