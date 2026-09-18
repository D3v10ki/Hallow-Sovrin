package com.hollowsovereign.entity;

import com.hollowsovereign.HollowSovereign;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class HSEntities {
    private HSEntities() {}

    public static final EntityType<GravityOrbEntity> GRAVITY_ORB = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(HollowSovereign.MOD_ID, "gravity_orb"),
            EntityType.Builder.<GravityOrbEntity>create(GravityOrbEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.0f, 1.0f)
                    .makeFireImmune()
                    .disableSaving()
                    .disableSummon()
                    .build("gravity_orb"));

    public static final EntityType<BlackHoleEntity> BLACK_HOLE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(HollowSovereign.MOD_ID, "black_hole"),
            EntityType.Builder.<BlackHoleEntity>create(BlackHoleEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .makeFireImmune()
                    .disableSaving()
                    .disableSummon()
                    .build("black_hole"));

    public static final EntityType<TendrilEntity> TENDRIL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(HollowSovereign.MOD_ID, "tendril"),
            EntityType.Builder.<TendrilEntity>create(TendrilEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.4f, 0.4f)
                    .makeFireImmune()
                    .disableSaving()
                    .disableSummon()
                    .build("tendril"));

    /** Call in mod init to force class-load + registration. */
    public static void register() {}
}
