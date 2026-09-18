package com.hollowsovereign.data;

import com.hollowsovereign.SorcererClass;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player Hollow Sovereign progression. Persistent fields (class, level, xp,
 * skill points) survive death and logout. Cooldowns are transient.
 */
public class PlayerData {
    public static final int MAX_LEVEL = 30;

    private int classId = -1;   // -1 = no class chosen yet
    private int level = 1;
    private int xp = 0;
    private int skillPoints = 0;

    // Transient: ability slot -> world time (tick) when the cooldown ends.
    private final Map<Integer, Long> cooldownEnds = new HashMap<>();

    public static PlayerData get(PlayerEntity player) {
        return ((PlayerDataAccess) player).hollowsovereign$getData();
    }

    // --- class ---
    public boolean hasClass() { return classId >= 0; }
    public SorcererClass getSorcererClass() { return SorcererClass.byOrdinalOrNull(classId); }
    public int getClassId() { return classId; }

    public void setSorcererClass(SorcererClass clazz) {
        this.classId = clazz == null ? -1 : clazz.ordinal();
    }

    /** Full reset used by Rebirth Talisman / admin reset. */
    public void resetClass() {
        this.classId = -1;
        this.level = 1;
        this.xp = 0;
        this.skillPoints = 0;
        this.cooldownEnds.clear();
    }

    // --- progression ---
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(MAX_LEVEL, level)); }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = Math.max(0, xp); }
    public void addXpRaw(int amount) { this.xp += Math.max(0, amount); }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int sp) { this.skillPoints = Math.max(0, sp); }
    public void addSkillPoints(int sp) { this.skillPoints = Math.max(0, this.skillPoints + sp); }

    // --- cooldowns ---
    public long getCooldownEnd(int slot) { return cooldownEnds.getOrDefault(slot, 0L); }
    public void setCooldownEnd(int slot, long tick) { cooldownEnds.put(slot, tick); }
    public void clearCooldowns() { cooldownEnds.clear(); }

    public boolean isOnCooldown(int slot, long now) { return now < getCooldownEnd(slot); }

    // --- copy (respawn) ---
    public void copyFrom(PlayerData other) {
        this.classId = other.classId;
        this.level = other.level;
        this.xp = other.xp;
        this.skillPoints = other.skillPoints;
        // cooldowns intentionally not copied (reset on respawn)
    }

    // --- persistence ---
    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("classId", classId);
        nbt.putInt("level", level);
        nbt.putInt("xp", xp);
        nbt.putInt("skillPoints", skillPoints);
    }

    public void readNbt(NbtCompound nbt) {
        this.classId = nbt.contains("classId") ? nbt.getInt("classId") : -1;
        this.level = nbt.contains("level") ? Math.max(1, nbt.getInt("level")) : 1;
        this.xp = nbt.getInt("xp");
        this.skillPoints = nbt.getInt("skillPoints");
    }
}
