package com.hollowsovereign.client.entity;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.entity.GravityOrbEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

@Environment(EnvType.CLIENT)
public class GravityOrbModel extends GeoModel<GravityOrbEntity> {
    private static final Identifier MODEL = new Identifier(HollowSovereign.MOD_ID, "geo/grav_orb.geo.json");
    private static final Identifier TEXTURE = new Identifier(HollowSovereign.MOD_ID, "textures/entity/grav_orb.png");
    private static final Identifier ANIMATION = new Identifier(HollowSovereign.MOD_ID, "animations/grav_orb.animation.json");

    @Override
    public Identifier getModelResource(GravityOrbEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GravityOrbEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GravityOrbEntity animatable) {
        return ANIMATION;
    }
}
