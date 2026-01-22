package com.winlabs.service;

import com.google.gson.*;
import com.winlabs.model.PlaylistSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for saving and loading playlist-specific settings from .wlp files.
 * .wlp (Win-Labs Playlist) files are JSON files stored alongside the playlist.json.
 * 
 * Example:
 * - Playlist: /music/setlist.json
 * - Settings: /music/setlist.wlp
 */
public class PlaylistSettingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaylistSettingsService.class);
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String WLP_EXTENSION = ".wlp";
    
    /**
     * Saves playlist settings to a .wlp file.
     * The .wlp file is created in the same directory as the playlist with the same base name.
     * 
     * @param playlistPath Path to the playlist .json file
     * @param settings The PlaylistSettings to save
     * @throws IOException if writing the file fails
     */
    public void save(Path playlistPath, PlaylistSettings settings) throws IOException {
        Path wlpPath = getWlpPath(playlistPath);
        logger.debug("Saving playlist settings to {}", wlpPath);
        
        // Create the JSON representation
        JsonObject json = new JsonObject();
        json.addProperty("masterVolume", settings.getMasterVolume());
        json.addProperty("audioFileDirectory", settings.getAudioFileDirectory());
        json.addProperty("defaultPreWait", settings.getDefaultPreWait());
        json.addProperty("defaultPostWait", settings.getDefaultPostWait());
        json.addProperty("defaultAutoFollow", settings.isDefaultAutoFollow());
        json.addProperty("playlistName", settings.getPlaylistName());
        json.addProperty("lastModified", settings.getLastModified());
        json.addProperty("isPinned", settings.isPinned());
        
        // Write to file
        String jsonString = gson.toJson(json);
        Files.writeString(wlpPath, jsonString);
        logger.info("Playlist settings saved to {}", wlpPath.getFileName());
    }
    
    /**
     * Loads playlist settings from a .wlp file.
     * If the file doesn't exist, returns a PlaylistSettings with default values.
     * 
     * @param playlistPath Path to the playlist .json file
     * @return PlaylistSettings loaded from the .wlp file or defaults if not found
     * @throws IOException if reading the file fails
     */
    public PlaylistSettings load(Path playlistPath) throws IOException {
        Path wlpPath = getWlpPath(playlistPath);
        PlaylistSettings settings = new PlaylistSettings();
        
        if (!Files.exists(wlpPath)) {
            logger.debug("No settings file found for {}, using defaults", playlistPath.getFileName());
            return settings;
        }
        
        logger.debug("Loading playlist settings from {}", wlpPath);
        try {
            String content = Files.readString(wlpPath);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            
            if (json.has("masterVolume")) {
                settings.setMasterVolume(json.get("masterVolume").getAsDouble());
            }
            if (json.has("audioFileDirectory")) {
                settings.setAudioFileDirectory(json.get("audioFileDirectory").getAsString());
            }
            if (json.has("defaultPreWait")) {
                settings.setDefaultPreWait(json.get("defaultPreWait").getAsDouble());
            }
            if (json.has("defaultPostWait")) {
                settings.setDefaultPostWait(json.get("defaultPostWait").getAsDouble());
            }
            if (json.has("defaultAutoFollow")) {
                settings.setDefaultAutoFollow(json.get("defaultAutoFollow").getAsBoolean());
            }
            if (json.has("playlistName")) {
                settings.setPlaylistName(json.get("playlistName").getAsString());
            }
            if (json.has("lastModified")) {
                settings.setLastModified(json.get("lastModified").getAsLong());
            }
            if (json.has("isPinned")) {
                settings.setIsPinned(json.get("isPinned").getAsBoolean());
            }
        } catch (Exception e) {
            // If parsing fails, return defaults
            logger.error("Failed to parse .wlp file: {}", wlpPath, e);
        }
        
        return settings;
    }
    
    /**
     * Deletes the .wlp file for a given playlist.
     * 
     * @param playlistPath Path to the playlist .json file
     * @throws IOException if deletion fails
     */
    public void delete(Path playlistPath) throws IOException {
        Path wlpPath = getWlpPath(playlistPath);
        if (Files.exists(wlpPath)) {
            Files.delete(wlpPath);
        }
    }
    
    /**
     * Checks if a .wlp file exists for the given playlist.
     * 
     * @param playlistPath Path to the playlist .json file
     * @return true if the .wlp file exists, false otherwise
     */
    public boolean exists(Path playlistPath) {
        return Files.exists(getWlpPath(playlistPath));
    }
    
    /**
     * Gets the .wlp path for a given playlist path.
     * 
     * Example: /music/setlist.json → /music/setlist.wlp
     * 
     * @param playlistPath Path to the playlist .json file
     * @return Path to the corresponding .wlp file
     */
    public Path getWlpPath(Path playlistPath) {
        String pathStr = playlistPath.toString();
        
        // Remove .json extension if present
        if (pathStr.endsWith(".json")) {
            pathStr = pathStr.substring(0, pathStr.length() - 5);
        }
        
        return Paths.get(pathStr + WLP_EXTENSION);
    }
}
