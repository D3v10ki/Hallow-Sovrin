package com.hollowsovereign.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Client visual settings (design doc section 15), saved to config/hollowsovereign-client.json. */
@Environment(EnvType.CLIENT)
public class HSClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HSClientConfig INSTANCE;

    /** 0 = Low, 1 = Medium, 2 = Max. */
    public int effectsIntensity = 2;
    /** 0..1 multiplier (used once camera shake lands in the Impact phase). */
    public float screenShake = 1.0f;
    /** Replaces rapid flashes with a soft tint for flash-sensitive players. */
    public boolean flashSafe = false;
    /** 0 = Always, 1 = First time only, 2 = Off. */
    public int bossIntros = 0;
    public float hudScale = 1.0f;
    /** Bloom post-processing (requires the Satin mod). Off by default — opt in via Mod Menu. */
    public boolean bloom = false;

    public static HSClientConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    /** Intensity multiplier applied to effect strength. */
    public float intensityScale() {
        return switch (effectsIntensity) {
            case 0 -> 0.4f;
            case 1 -> 0.7f;
            default -> 1.0f;
        };
    }

    public String intensityLabel() {
        return switch (effectsIntensity) {
            case 0 -> "Low";
            case 1 -> "Medium";
            default -> "Max";
        };
    }

    public String bossIntrosLabel() {
        return switch (bossIntros) {
            case 1 -> "First time only";
            case 2 -> "Off";
            default -> "Always";
        };
    }

    public static void load() {
        Path path = path();
        HSClientConfig cfg = new HSClientConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), HSClientConfig.class);
                if (cfg == null) cfg = new HSClientConfig();
            }
        } catch (Exception e) {
            System.err.println("[HollowSovereign] Failed to read client config, using defaults: " + e);
        }
        INSTANCE = cfg;
        save();
    }

    public static void save() {
        if (INSTANCE == null) return;
        try {
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (Exception e) {
            System.err.println("[HollowSovereign] Failed to write client config: " + e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("hollowsovereign-client.json");
    }
}
