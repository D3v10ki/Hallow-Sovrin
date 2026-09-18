package com.hollowsovereign.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Host-side settings, saved to config/hollowsovereign-server.json. */
public class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ServerConfig INSTANCE;

    // Tunables (see design doc section 15).
    public float xpMultiplier = 1.0f;
    public float bossHpScalingPerPlayer = 0.5f;
    public int downedTimerSeconds = 30;
    public int parryWindowTicks = 4;      // ~0.2s
    public boolean friendlyFire = false;
    public boolean downedEnabled = true;

    public static ServerConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path path = configPath();
        ServerConfig cfg = new ServerConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), ServerConfig.class);
                if (cfg == null) cfg = new ServerConfig();
            }
        } catch (Exception e) {
            System.err.println("[HollowSovereign] Failed to read server config, using defaults: " + e);
        }
        INSTANCE = cfg;
        save();
    }

    public static void save() {
        if (INSTANCE == null) return;
        try {
            Files.writeString(configPath(), GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[HollowSovereign] Failed to write server config: " + e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("hollowsovereign-server.json");
    }
}
