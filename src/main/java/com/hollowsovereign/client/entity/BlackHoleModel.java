package com.hollowsovereign.client.entity;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.entity.BlackHoleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

@Environment(EnvType.CLIENT)
public class BlackHoleModel extends GeoModel<BlackHoleEntity> {
    private static final Identifier MODEL = new Identifier(HollowSovereign.MOD_ID, "geo/black_hole.geo.json");
    private static final Identifier TEXTURE = new Identifier(HollowSovereign.MOD_ID, "textures/entity/black_hole.png");
    private static final Identifier ANIMATION = new Identifier(HollowSovereign.MOD_ID, "animations/black_hole.animation.json");

    @Override
    public Identifier getModelResource(BlackHoleEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(BlackHoleEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(BlackHoleEntity animatable) {
        return ANIMATION;
    }
}
