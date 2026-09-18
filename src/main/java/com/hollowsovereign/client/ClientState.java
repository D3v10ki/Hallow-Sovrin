package com.hollowsovereign.client;

import com.hollowsovereign.SorcererClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Client-side cache of the player's progression, kept in sync by S2C packets. */
@Environment(EnvType.CLIENT)
public final class ClientState {
    private ClientState() {}

    public static int classId = -1;
    public static int level = 1;
    public static int xp = 0;
    public static int xpToNext = 1;
    public static int skillPoints = 0;

    // HUD cooldown timers (wall-clock, ms). Index 0..5 abilities, 6 ultimate.
    public static final long[] cooldownEndMs = new long[7];
    public static final long[] cooldownTotalMs = new long[7];

    // Impact / flash feedback.
    public static long flashEndMs = 0;
    public static long flashTotalMs = 0;
    public static int flashTier = 0;

    // Level-up popup.
    public static long levelUpEndMs = 0;
    public static int levelUpLevel = 0;

    // Domain interior: refreshed by server pings; auto-clears if they stop.
    public static long domainUntilMs = 0;
    // World position of the domain's singularity (black-hole centre) + radius, from the latest ping.
    public static double domainCx = 0, domainCy = 0, domainCz = 0;
    public static int domainRadius = 22;

    public static void onDomainPing(double cx, double cy, double cz, int radius) {
        domainUntilMs = System.currentTimeMillis() + 800;
        domainCx = cx;
        domainCy = cy;
        domainCz = cz;
        domainRadius = radius;
    }

    public static boolean inDomain() {
        return System.currentTimeMillis() < domainUntilMs;
    }

    public static SorcererClass sorcererClass() {
        return SorcererClass.byOrdinalOrNull(classId);
    }

    public static boolean hasClass() {
        return classId >= 0;
    }

    public static void onSync(int classId, int level, int xp, int xpToNext, int skillPoints) {
        ClientState.classId = classId;
        ClientState.level = level;
        ClientState.xp = xp;
        ClientState.xpToNext = xpToNext;
        ClientState.skillPoints = skillPoints;
    }

    public static void onAbilityFeedback(int slot, int cooldownTicks, int tier) {
        long now = System.currentTimeMillis();
        if (slot >= 0 && slot < cooldownEndMs.length && cooldownTicks > 0) {
            cooldownTotalMs[slot] = cooldownTicks * 50L;
            cooldownEndMs[slot] = now + cooldownTotalMs[slot];
        }
        flashTier = tier;
        flashTotalMs = tier >= 3 ? 260 : 160;
        flashEndMs = now + flashTotalMs;
    }

    public static void onLevelUp(int level) {
        levelUpLevel = level;
        levelUpEndMs = System.currentTimeMillis() + 2600;
    }

    public static void reset() {
        classId = -1;
        level = 1;
        xp = 0;
        xpToNext = 1;
        skillPoints = 0;
        java.util.Arrays.fill(cooldownEndMs, 0);
        java.util.Arrays.fill(cooldownTotalMs, 0);
        flashEndMs = 0;
        levelUpEndMs = 0;
    }
}
