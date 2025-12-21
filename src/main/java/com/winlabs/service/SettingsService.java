package com.winlabs.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.winlabs.model.ApplicationSettings;
import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import com.winlabs.model.WorkspaceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for saving and loading application and workspace settings.
 * Settings are persisted to JSON files in the user's home directory.
 */
public class SettingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private static final String SETTINGS_DIR = ".winlabs";
    private static final String APP_SETTINGS_FILE = "app-settings.json";
    private static final String WORKSPACE_SETTINGS_FILE = "workspace-settings.json";
    private final Gson gson;
    private final Path appSettingsPath;
    private final Path workspaceSettingsPath;
    
    /**
     * Creates a new settings service.
     */
    public SettingsService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Settings are stored in user home directory: ~/.winlabs/
        String userHome = System.getProperty("user.home");
        Path settingsDir = Paths.get(userHome, SETTINGS_DIR);
        this.appSettingsPath = settingsDir.resolve(APP_SETTINGS_FILE);
        this.workspaceSettingsPath = settingsDir.resolve(WORKSPACE_SETTINGS_FILE);
        
        // Create settings directory if it doesn't exist
        try {
            Files.createDirectories(settingsDir);
        } catch (IOException e) {
            System.err.println("Failed to create settings directory: " + e.getMessage());
        }
    }
    
    /**
     * Saves settings to files.
     * 
     * @param settings The settings to save
     * @throws IOException If saving fails
     */
    public void save(Settings settings) throws IOException {
        saveApplicationSettings(settings.getApplicationSettings());
        saveWorkspaceSettings(settings.getWorkspaceSettings());
    }
    
    /**
     * Saves application settings to file.
     */
    private void saveApplicationSettings(ApplicationSettings settings) throws IOException {
        JsonObject json = new JsonObject();
        
        // Appearance
        json.addProperty("theme", settings.getTheme());
        
        // General
        json.addProperty("autoSaveEnabled", settings.isAutoSaveEnabled());
        json.addProperty("autoSaveInterval", settings.getAutoSaveInterval());
        
        // Default cue properties
        json.addProperty("preWaitDefault", settings.getPreWaitDefault());
        json.addProperty("postWaitDefault", settings.getPostWaitDefault());
        json.addProperty("autoFollowDefault", settings.isAutoFollowDefault());
        
        String jsonString = gson.toJson(json);
        Files.writeString(appSettingsPath, jsonString);
    }
    
    /**
     * Saves workspace settings to file.
     */
    private void saveWorkspaceSettings(WorkspaceSettings settings) throws IOException {
        JsonObject json = new JsonObject();
        
        // Audio
        json.addProperty("masterVolume", settings.getMasterVolume());
        
        // Workspace
        json.addProperty("lastPlaylistPath", settings.getLastPlaylistPath());
        json.addProperty("audioFileDirectory", settings.getAudioFileDirectory());
        
        String jsonString = gson.toJson(json);
        Files.writeString(workspaceSettingsPath, jsonString);
    }
    
    /**
     * Loads settings from files.
     * If files don't exist, returns default settings.
     * 
     * @return The loaded settings or default settings if files don't exist
     * @throws IOException If loading fails
     */
    public Settings load() throws IOException {
        Settings settings = new Settings();
        
        // Load application settings
        if (Files.exists(appSettingsPath)) {
            try {
                String jsonString = Files.readString(appSettingsPath);
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                loadApplicationSettings(settings.getApplicationSettings(), json);
            } catch (Exception e) {
                System.err.println("Failed to load application settings: " + e.getMessage());
            }
        }
        
        // Load workspace settings
        if (Files.exists(workspaceSettingsPath)) {
            try {
                String jsonString = Files.readString(workspaceSettingsPath);
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                loadWorkspaceSettings(settings.getWorkspaceSettings(), json);
            } catch (Exception e) {
                System.err.println("Failed to load workspace settings: " + e.getMessage());
            }
        }
        
        return settings;
    }
    
    /**
     * Loads application settings from JSON object.
     */
    private void loadApplicationSettings(ApplicationSettings settings, JsonObject json) {
        // Load appearance settings
        if (json.has("theme")) {
            settings.setTheme(json.get("theme").getAsString());
        }
        
        // Load general settings
        if (json.has("autoSaveEnabled")) {
            settings.setAutoSaveEnabled(json.get("autoSaveEnabled").getAsBoolean());
        }
        if (json.has("autoSaveInterval")) {
            settings.setAutoSaveInterval(json.get("autoSaveInterval").getAsInt());
        }
        
        // Load default cue properties
        if (json.has("preWaitDefault")) {
            settings.setPreWaitDefault(json.get("preWaitDefault").getAsDouble());
        }
        if (json.has("postWaitDefault")) {
            settings.setPostWaitDefault(json.get("postWaitDefault").getAsDouble());
        }
        if (json.has("autoFollowDefault")) {
            settings.setAutoFollowDefault(json.get("autoFollowDefault").getAsBoolean());
        }
    }
    
    /**
     * Loads workspace settings from JSON object.
     */
    private void loadWorkspaceSettings(WorkspaceSettings settings, JsonObject json) {
        // Load audio settings
        if (json.has("masterVolume")) {
            settings.setMasterVolume(json.get("masterVolume").getAsDouble());
        }
        
        // Load workspace settings
        if (json.has("lastPlaylistPath")) {
            settings.setLastPlaylistPath(json.get("lastPlaylistPath").getAsString());
        }
        if (json.has("audioFileDirectory")) {
            settings.setAudioFileDirectory(json.get("audioFileDirectory").getAsString());
        }
    }
    
    /**
     * Gets the path to the application settings file.
     * 
     * @return The application settings file path
     */
    public Path getAppSettingsPath() {
        return appSettingsPath;
    }
    
    /**
     * Gets the path to the workspace settings file.
     * 
     * @return The workspace settings file path
     */
    public Path getWorkspaceSettingsPath() {
        return workspaceSettingsPath;
    }
    
    /**
     * Deletes the settings files.
     * 
     * @throws IOException If deletion fails
     */
    public void deleteSettings() throws IOException {
        if (Files.exists(appSettingsPath)) {
            Files.delete(appSettingsPath);
        }
        if (Files.exists(workspaceSettingsPath)) {
            Files.delete(workspaceSettingsPath);
        }
    }
}
