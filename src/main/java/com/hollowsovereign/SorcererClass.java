package com.hollowsovereign;

import net.minecraft.util.Formatting;

/**
 * The four playable sorcerer classes. Ordinal is used as the network/NBT id,
 * so DO NOT reorder these without a data migration.
 */
public enum SorcererClass {
    VOID_WEAVER("void_weaver", "Void Weaver", "Control", Formatting.DARK_PURPLE),
    CRIMSON_WARDEN("crimson_warden", "Crimson Warden", "Tank / Sustain", Formatting.DARK_RED),
    HEX_BINDER("hex_binder", "Hex Binder", "Summoner / Support", Formatting.DARK_AQUA),
    ASHEN_BLADE("ashen_blade", "Ashen Blade", "Melee / Boss Breaker", Formatting.GOLD);

    private final String id;
    private final String displayName;
    private final String role;
    private final Formatting color;

    SorcererClass(String id, String displayName, String role, Formatting color) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
        this.color = color;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String role() { return role; }
    public Formatting color() { return color; }

    public static SorcererClass byOrdinalOrNull(int ordinal) {
        SorcererClass[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return null;
        return values[ordinal];
    }

    public static SorcererClass byIdOrNull(String id) {
        for (SorcererClass c : values()) {
            if (c.id.equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}
