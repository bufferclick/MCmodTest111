package com.bufferclick.handyutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bufferclick.handyutils.HandyUtilsMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HandyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "handyutils.json");

    private static HandyConfig INSTANCE = new HandyConfig();

    // Config options
    public boolean enableInfoHud = true;
    public boolean enableArmorHud = true;
    public boolean enableFullbright = false;
    public boolean enableZoom = true;
    public boolean enableShulkerPreview = true;
    public boolean enableInventorySorter = true;
    public boolean enableDeathTracker = true;

    public float zoomDivisor = 4.0f; // 4x zoom
    public float zoomSmoothSpeed = 0.5f;
    public double fullbrightGamma = 12.0;
    public double defaultGamma = 0.5;

    public boolean infoHudShowFps = true;
    public boolean infoHudShowPing = true;
    public boolean infoHudShowCoords = true;
    public boolean infoHudShowBiome = true;
    public boolean infoHudShowLight = true;
    public boolean infoHudShowDirection = true;

    public int hudX = 6;
    public int hudY = 6;

    public static HandyConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, HandyConfig.class);
                if (INSTANCE == null) INSTANCE = new HandyConfig();
                HandyUtilsMod.LOGGER.info("[HandyUtils] Config loaded");
            } catch (Exception e) {
                HandyUtilsMod.LOGGER.error("[HandyUtils] Failed to load config, using defaults", e);
                INSTANCE = new HandyConfig();
            }
        } else {
            INSTANCE = new HandyConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            HandyUtilsMod.LOGGER.error("[HandyUtils] Failed to save config", e);
        }
    }
}
