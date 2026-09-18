package com.hollowsovereign.leveling;

import com.hollowsovereign.config.ServerConfig;
import com.hollowsovereign.data.PlayerData;
import com.hollowsovereign.net.HSNet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** XP curve and level-up handling. Numbers are intentionally easy to tune. */
public final class Leveling {
    private Leveling() {}

    /** XP required to go from {@code level} to {@code level + 1}. */
    public static int xpToNext(int level) {
        if (level >= PlayerData.MAX_LEVEL) return Integer.MAX_VALUE;
        return 20 + (level - 1) * 15;
    }

    /** XP awarded for killing a mob, before the server multiplier. */
    public static int baseXpFor(LivingEntity killed) {
        int fromHealth = Math.round(killed.getMaxHealth() / 2f);
        return Math.max(1, fromHealth);
    }

    public static void awardKillXp(ServerPlayerEntity player, LivingEntity killed) {
        PlayerData data = PlayerData.get(player);
        if (!data.hasClass()) return;
        int amount = Math.round(baseXpFor(killed) * ServerConfig.get().xpMultiplier);
        addXp(player, data, amount);
    }

    public static void addXp(ServerPlayerEntity player, PlayerData data, int amount) {
        if (amount <= 0) return;
        data.addXpRaw(amount);
        boolean leveled = false;
        while (data.getLevel() < PlayerData.MAX_LEVEL && data.getXp() >= xpToNext(data.getLevel())) {
            data.setXp(data.getXp() - xpToNext(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            data.addSkillPoints(1);
            leveled = true;
            HSNet.sendLevelUp(player, data.getLevel());
        }
        if (data.getLevel() >= PlayerData.MAX_LEVEL) {
            data.setXp(0);
        }
        HSNet.syncData(player);
        if (leveled) {
            player.getWorld().playSound(null, player.getBlockPos(),
                    net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
                    net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
    }
}
