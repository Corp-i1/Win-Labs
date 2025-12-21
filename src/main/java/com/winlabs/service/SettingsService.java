package com.winlabs.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.winlabs.model.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for saving and loading application settings.
 * Settings are persisted to a JSON file in the user's home directory.
 */
public class SettingsService {
    
    private static final String SETTINGS_DIR = ".winlabs";
    private static final String SETTINGS_FILE = "settings.json";
    private final Gson gson;
    private final Path settingsPath;
    
    /**
     * Creates a new settings service.
     */
    public SettingsService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Settings are stored in user home directory: ~/.winlabs/settings.json
        String userHome = System.getProperty("user.home");
        Path settingsDir = Paths.get(userHome, SETTINGS_DIR);
        this.settingsPath = settingsDir.resolve(SETTINGS_FILE);
        
        // Create settings directory if it doesn't exist
        try {
            Files.createDirectories(settingsDir);
        } catch (IOException e) {
            System.err.println("Failed to create settings directory: " + e.getMessage());
        }
    }
    
    /**
     * Saves settings to file.
     * 
     * @param settings The settings to save
     * @throws IOException If saving fails
     */
    public void save(Settings settings) throws IOException {
        JsonObject json = new JsonObject();
        
        // Appearance
        json.addProperty("theme", settings.getTheme());
        
        // Audio
        json.addProperty("masterVolume", settings.getMasterVolume());
        json.addProperty("enableMultiTrackPlayback", settings.isEnableMultiTrackPlayback());
        
        // General
        json.addProperty("autoSaveEnabled", settings.isAutoSaveEnabled());
        json.addProperty("autoSaveInterval", settings.getAutoSaveInterval());
        
        String jsonString = gson.toJson(json);
        Files.writeString(settingsPath, jsonString);
    }
    
    /**
     * Loads settings from file.
     * If the file doesn't exist, returns default settings.
     * 
     * @return The loaded settings or default settings if file doesn't exist
     * @throws IOException If loading fails
     */
    public Settings load() throws IOException {
        Settings settings = new Settings();
        
        // If settings file doesn't exist, return defaults
        if (!Files.exists(settingsPath)) {
            return settings;
        }
        
        try {
            String jsonString = Files.readString(settingsPath);
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);
            
            // Load appearance settings
            if (json.has("theme")) {
                settings.setTheme(json.get("theme").getAsString());
            }
            
            // Load audio settings
            if (json.has("masterVolume")) {
                settings.setMasterVolume(json.get("masterVolume").getAsDouble());
            }
            if (json.has("enableMultiTrackPlayback")) {
                settings.setEnableMultiTrackPlayback(json.get("enableMultiTrackPlayback").getAsBoolean());
            }
            
            // Load general settings
            if (json.has("autoSaveEnabled")) {
                settings.setAutoSaveEnabled(json.get("autoSaveEnabled").getAsBoolean());
            }
            if (json.has("autoSaveInterval")) {
                settings.setAutoSaveInterval(json.get("autoSaveInterval").getAsInt());
            }
            
            return settings;
        } catch (Exception e) {
            System.err.println("Failed to load settings, using defaults: " + e.getMessage());
            return settings;
        }
    }
    
    /**
     * Gets the path to the settings file.
     * 
     * @return The settings file path
     */
    public Path getSettingsPath() {
        return settingsPath;
    }
    
    /**
     * Deletes the settings file.
     * 
     * @throws IOException If deletion fails
     */
    public void deleteSettings() throws IOException {
        if (Files.exists(settingsPath)) {
            Files.delete(settingsPath);
        }
    }
}
