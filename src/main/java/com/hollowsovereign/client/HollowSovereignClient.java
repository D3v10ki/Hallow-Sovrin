package com.hollowsovereign.client;

import com.hollowsovereign.ability.Abilities;
import com.hollowsovereign.client.entity.GravityOrbRenderer;
import com.hollowsovereign.client.hud.HollowHud;
import com.hollowsovereign.client.particle.HSGlowParticle;
import com.hollowsovereign.client.screen.ClassSelectScreen;
import com.hollowsovereign.client.screen.SkillTreeScreen;
import com.hollowsovereign.entity.HSEntities;
import com.hollowsovereign.net.HSNet;
import com.hollowsovereign.particle.HSParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public class HollowSovereignClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HSClientConfig.load();
        HSKeybinds.register();

        ParticleFactoryRegistry.getInstance().register(HSParticles.VOID_GLOW,
                sprites -> new HSGlowParticle.Factory(sprites, 0.30f, 16));
        ParticleFactoryRegistry.getInstance().register(HSParticles.VOID_STAR,
                sprites -> new HSGlowParticle.Factory(sprites, 0.16f, 22));
        ParticleFactoryRegistry.getInstance().register(HSParticles.VOID_SHARD,
                sprites -> new HSGlowParticle.Factory(sprites, 0.22f, 14));

        // Bloom (custom Satin post-shader) is shelved — it black-screens and can't be verified
        // without running GLSL. Reliable glow comes from bright particles + an external shaderpack.
        // HSBloom is intentionally NOT initialized.

        EntityRendererRegistry.register(HSEntities.GRAVITY_ORB, GravityOrbRenderer::new);
        EntityRendererRegistry.register(HSEntities.BLACK_HOLE, com.hollowsovereign.client.entity.BlackHoleRenderer::new);
        EntityRendererRegistry.register(HSEntities.TENDRIL, com.hollowsovereign.client.entity.TendrilEntityRenderer::new);

        // Custom core shaders for the water-style void floor and the black-hole accretion disc.
        net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(com.hollowsovereign.client.render.VoidFloorRenderer.SHADER_ID,
                    net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE,
                    com.hollowsovereign.client.render.VoidFloorRenderer::setProgram);
            context.register(com.hollowsovereign.client.render.VoidCoreRenderer.SHADER_ID,
                    net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE,
                    com.hollowsovereign.client.render.VoidCoreRenderer::setProgram);
            context.register(com.hollowsovereign.client.render.VoidDiscRenderer.SHADER_ID,
                    net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE,
                    com.hollowsovereign.client.render.VoidDiscRenderer::setProgram);
        });
        // Draw after the world's translucent pass: starfield wall backdrop first, then floor, then
        // the black-hole core (opaque, writes depth) before the disc so the core occludes the disc.
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                com.hollowsovereign.client.render.VoidWallRenderer::render);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                com.hollowsovereign.client.render.VoidFloorRenderer::render);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                com.hollowsovereign.client.render.VoidCoreRenderer::render);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                com.hollowsovereign.client.render.VoidDiscRenderer::render);
        // Teleport / dash afterimage ghosts render in the entity pass (translucent player models).
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(
                com.hollowsovereign.client.render.VoidGhostRenderer::render);

        registerReceivers();

        HudRenderCallback.EVENT.register(new HollowHud());

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        for (int i = 0; i < HSKeybinds.ABILITIES.length; i++) {
            while (HSKeybinds.ABILITIES[i].wasPressed()) {
                sendUse(i);
            }
        }
        while (HSKeybinds.ULTIMATE.wasPressed()) {
            sendUse(Abilities.ULT_SLOT);
        }
        while (HSKeybinds.SKILL_TREE.wasPressed()) {
            if (ClientState.hasClass()) {
                client.setScreen(new SkillTreeScreen());
            }
        }

        if (ClientState.inDomain()) {
            com.hollowsovereign.client.render.VoidFloorRenderer.trackImpacts(client);
        }
    }

    private void sendUse(int slot) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        ClientPlayNetworking.send(HSNet.USE_ABILITY, buf);
    }

    private void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(HSNet.SYNC_DATA, (client, handler, buf, sender) -> {
            int classId = buf.readInt();
            int level = buf.readInt();
            int xp = buf.readInt();
            int xpToNext = buf.readInt();
            int skillPoints = buf.readInt();
            client.execute(() -> ClientState.onSync(classId, level, xp, xpToNext, skillPoints));
        });

        ClientPlayNetworking.registerGlobalReceiver(HSNet.OPEN_CLASS_SELECT, (client, handler, buf, sender) ->
                client.execute(() -> client.setScreen(new ClassSelectScreen())));

        ClientPlayNetworking.registerGlobalReceiver(HSNet.ABILITY_FEEDBACK, (client, handler, buf, sender) -> {
            int slot = buf.readInt();
            int cd = buf.readInt();
            int tier = buf.readInt();
            client.execute(() -> ClientState.onAbilityFeedback(slot, cd, tier));
        });

        ClientPlayNetworking.registerGlobalReceiver(HSNet.LEVEL_UP, (client, handler, buf, sender) -> {
            int level = buf.readInt();
            client.execute(() -> ClientState.onLevelUp(level));
        });

        ClientPlayNetworking.registerGlobalReceiver(HSNet.DOMAIN_STATE, (client, handler, buf, sender) -> {
            double cx = buf.readDouble();
            double cy = buf.readDouble();
            double cz = buf.readDouble();
            int radius = buf.readInt();
            client.execute(() -> ClientState.onDomainPing(cx, cy, cz, radius));
        });

        ClientPlayNetworking.registerGlobalReceiver(HSNet.TELEPORT_TRAIL, (client, handler, buf, sender) -> {
            int id = buf.readInt();
            net.minecraft.util.math.Vec3d from = new net.minecraft.util.math.Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            net.minecraft.util.math.Vec3d to = new net.minecraft.util.math.Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int count = buf.readInt();
            float bodyYaw = buf.readFloat();
            float headYaw = buf.readFloat();
            float pitch = buf.readFloat();
            int life = buf.readInt();
            client.execute(() -> com.hollowsovereign.client.render.VoidGhostRenderer.addTrail(
                    id, from, to, count, bodyYaw, headYaw, pitch, life));
        });

        ClientPlayNetworking.registerGlobalReceiver(HSNet.FLOOR_RIPPLE, (client, handler, buf, sender) -> {
            double x = buf.readDouble();
            double z = buf.readDouble();
            client.execute(() -> com.hollowsovereign.client.render.VoidFloorRenderer.rippleNow(x, z));
        });

    }

}
