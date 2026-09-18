package com.hollowsovereign.net;

import com.hollowsovereign.HollowSovereign;
import com.hollowsovereign.SorcererClass;
import com.hollowsovereign.ability.Abilities;
import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.leveling.Leveling;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/** All packet channels. Server-side receivers live here; client receivers live in the client init. */
public final class HSNet {
    private HSNet() {}

    // Server -> Client
    public static final Identifier SYNC_DATA = id("sync_data");
    public static final Identifier OPEN_CLASS_SELECT = id("open_class_select");
    public static final Identifier ABILITY_FEEDBACK = id("ability_feedback");
    public static final Identifier LEVEL_UP = id("level_up");
    public static final Identifier DOMAIN_STATE = id("domain_state");
    public static final Identifier TELEPORT_TRAIL = id("teleport_trail");
    public static final Identifier FLOOR_RIPPLE = id("floor_ripple");
    public static final Identifier WARD_STATE = id("ward_state");
    public static final Identifier WARD_IMPACT = id("ward_impact");

    // Client -> Server
    public static final Identifier SELECT_CLASS = id("select_class");
    public static final Identifier USE_ABILITY = id("use_ability");

    private static Identifier id(String path) {
        return new Identifier(HollowSovereign.MOD_ID, path);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SELECT_CLASS, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readInt();
            server.execute(() -> {
                PlayerData data = PlayerData.get(player);
                if (data.hasClass()) return; // permanent choice; use admin command to change
                SorcererClass clazz = SorcererClass.byOrdinalOrNull(ordinal);
                if (clazz == null) return;
                data.setSorcererClass(clazz);
                syncData(player);
                player.sendMessage(net.minecraft.text.Text.literal("You awakened as the ")
                        .append(net.minecraft.text.Text.literal(clazz.displayName()).formatted(clazz.color()))
                        .append("."), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(USE_ABILITY, (server, player, handler, buf, sender) -> {
            int slot = buf.readInt();
            server.execute(() -> Abilities.activate(player, slot));
        });
    }

    // --- senders ---

    public static void syncData(ServerPlayerEntity player) {
        PlayerData data = PlayerData.get(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(data.getClassId());
        buf.writeInt(data.getLevel());
        buf.writeInt(data.getXp());
        buf.writeInt(Leveling.xpToNext(data.getLevel()));
        buf.writeInt(data.getSkillPoints());
        ServerPlayNetworking.send(player, SYNC_DATA, buf);
    }

    public static void openClassSelect(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_CLASS_SELECT, PacketByteBufs.create());
    }

    public static void sendAbilityFeedback(ServerPlayerEntity player, int slot, int cooldownTicks, int tier) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        buf.writeInt(cooldownTicks);
        buf.writeInt(tier);
        ServerPlayNetworking.send(player, ABILITY_FEEDBACK, buf);
    }

    public static void sendLevelUp(ServerPlayerEntity player, int level) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(level);
        ServerPlayNetworking.send(player, LEVEL_UP, buf);
    }

    /** Tells a client it is inside a domain, where its singularity is, and how big it is. */
    public static void sendDomainPing(ServerPlayerEntity player, double cx, double cy, double cz, int radius) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(cx);
        buf.writeDouble(cy);
        buf.writeDouble(cz);
        buf.writeInt(radius);
        ServerPlayNetworking.send(player, DOMAIN_STATE, buf);
    }

    /**
     * Spawn a fading afterimage "ghost trail" of {@code caster} along the segment from -> to. The
     * client spaces {@code count} ghosts along it and renders the caster's own model/skin at the
     * captured rotation. Sent to the caster AND everyone tracking them, so co-op partners see it too.
     * Blink sends one packet (count 5) for the whole hop; Umbral Dash sends one per tick (count 1)
     * so the trail is laid down progressively as it travels.
     */
    public static void sendTrail(ServerPlayerEntity caster, Vec3d from, Vec3d to, int count,
                                 float bodyYaw, float headYaw, float pitch, float stretch,
                                 boolean rupture, int lifeTicks) {
        Set<ServerPlayerEntity> recipients = new HashSet<>(PlayerLookup.tracking(caster));
        recipients.add(caster);
        for (ServerPlayerEntity p : recipients) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(caster.getId());
            buf.writeDouble(from.x); buf.writeDouble(from.y); buf.writeDouble(from.z);
            buf.writeDouble(to.x);   buf.writeDouble(to.y);   buf.writeDouble(to.z);
            buf.writeInt(count);
            buf.writeFloat(bodyYaw); buf.writeFloat(headYaw); buf.writeFloat(pitch);
            buf.writeFloat(stretch);       // >1 elongates ghosts along the motion axis (dash streak)
            buf.writeBoolean(rupture);     // spawn shockwave rings at both ends (Blink only)
            buf.writeInt(lifeTicks);
            ServerPlayNetworking.send(p, TELEPORT_TRAIL, buf);
        }
    }

    /** Ask nearby clients (caster + trackers) to ripple the domain's void-water floor at world XZ —
     *  used by the in-domain Gravity Snare so its zone blends with the existing floor shader. */
    public static void sendFloorRipple(ServerPlayerEntity caster, double worldX, double worldZ) {
        Set<ServerPlayerEntity> recipients = new HashSet<>(PlayerLookup.tracking(caster));
        recipients.add(caster);
        for (ServerPlayerEntity p : recipients) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeDouble(worldX);
            buf.writeDouble(worldZ);
            ServerPlayNetworking.send(p, FLOOR_RIPPLE, buf);
        }
    }

    /** Tell nearby clients a player's Void Ward dome is (re)active for durationTicks. */
    public static void sendWard(ServerPlayerEntity caster, int durationTicks) {
        Set<ServerPlayerEntity> recipients = new HashSet<>(PlayerLookup.tracking(caster));
        recipients.add(caster);
        for (ServerPlayerEntity p : recipients) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(caster.getId());
            buf.writeInt(durationTicks);
            ServerPlayNetworking.send(p, WARD_STATE, buf);
        }
    }

    /** Tell nearby clients a melee hit landed on a warded player's dome from unit direction dir. */
    public static void sendWardImpact(ServerPlayerEntity warded, Vec3d dir) {
        Set<ServerPlayerEntity> recipients = new HashSet<>(PlayerLookup.tracking(warded));
        recipients.add(warded);
        for (ServerPlayerEntity p : recipients) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(warded.getId());
            buf.writeFloat((float) dir.x);
            buf.writeFloat((float) dir.y);
            buf.writeFloat((float) dir.z);
            ServerPlayNetworking.send(p, WARD_IMPACT, buf);
        }
    }
}
