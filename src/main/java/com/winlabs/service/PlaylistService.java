package com.winlabs.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for loading and saving playlists in JSON format.
 */
public class PlaylistService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);
    private final Gson gson;
    
    public PlaylistService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Saves a playlist to a JSON file.
     */
    public void save(Playlist playlist, String filePath) throws IOException {
        if (playlist == null) {
            logger.error("Playlist parameter is null");
            throw new IllegalArgumentException("Playlist cannot be null");
        }

        if (filePath == null || filePath.isEmpty()) {
            logger.error("File path is null or empty: {}", filePath);
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        int cueCount = playlist.getCues() != null ? playlist.getCues().size() : 0;
        logger.info("Saving playlist '{}' with {} cues to: {}", playlist.getName(), cueCount, filePath);
        logger.trace("File path is valid: {}", filePath);
        logger.debug("File path length: {}", filePath.length());
        
        logger.debug("Creating JSON root object");
        logger.trace("Instantiating new JsonObject");
        JsonObject root = new JsonObject();
        logger.trace("JsonObject created: {}", root);
        
        logger.trace("Adding 'name' property to JSON");
        String playlistName = playlist.getName();
        logger.debug("Playlist name to save: {}", playlistName);
        root.addProperty("name", playlistName);
        logger.trace("Name property added");
        
        logger.trace("Adding 'version' property to JSON");
        root.addProperty("version", "1.0");
        logger.trace("Version property added: 1.0");
        
        logger.debug("Creating cues array");
        logger.trace("Instantiating new JsonArray for cues");
        JsonArray cuesArray = new JsonArray();
        logger.trace("JsonArray created: {}", cuesArray);
        
        logger.debug("Processing {} cues", playlist.getCues().size());
        logger.trace("Starting cue iteration loop");
        int cueIndex = 0;
        for (Cue cue : playlist.getCues()) {
            logger.trace("Processing cue {} of {}", cueIndex + 1, playlist.getCues().size());
            logger.debug("Current cue: number={}, name={}", cue.getNumber(), cue.getName());
            logger.trace("Cue details: {}", cue);
            
            logger.trace("Creating JsonObject for cue {}", cueIndex);
            JsonObject cueObj = new JsonObject();
            
            logger.trace("Adding cue properties to JSON object");
            logger.trace("Adding property 'number': {}", cue.getNumber());
            cueObj.addProperty("number", cue.getNumber());
            logger.trace("Adding property 'name': {}", cue.getName());
            cueObj.addProperty("name", cue.getName());
            logger.trace("Adding property 'duration': {}", cue.getDuration());
            cueObj.addProperty("duration", cue.getDuration());
            logger.trace("Adding property 'preWait': {}", cue.getPreWait());
            cueObj.addProperty("preWait", cue.getPreWait());
            logger.trace("Adding property 'postWait': {}", cue.getPostWait());
            cueObj.addProperty("postWait", cue.getPostWait());
            logger.trace("Adding property 'autoFollow': {}", cue.isAutoFollow());
            cueObj.addProperty("autoFollow", cue.isAutoFollow());
            logger.trace("Adding property 'filePath': {}", cue.getFilePath());
            cueObj.addProperty("filePath", cue.getFilePath());
            logger.debug("All properties added for cue {}", cueIndex);
            
            logger.trace("Adding cue object to array");
            cuesArray.add(cueObj);
            logger.trace("Cue {} added to array. Array size: {}", cueIndex, cuesArray.size());
            cueIndex++;
        }
        logger.info("Processed {} cues for JSON", cueIndex);
        logger.trace("Cue iteration loop complete");
        
        logger.trace("Adding cues array to root object");
        root.add("cues", cuesArray);
        logger.debug("Cues array added to root. Array size: {}", cuesArray.size());
        
        logger.debug("Converting JSON object to string");
        logger.trace("Calling gson.toJson()");
        String json = gson.toJson(root);
        logger.trace("JSON string generated. Length: {}", json.length());
        logger.debug("JSON string preview (first 100 chars): {}", json.substring(0, Math.min(100, json.length())));
        
        logger.debug("Creating Path object from file path");
        logger.trace("Calling Paths.get({})", filePath);
        Path path = Paths.get(filePath);
        logger.trace("Path object created: {}", path);
        logger.debug("Absolute path: {}", path.toAbsolutePath());
        
        logger.info("Writing JSON to file: {}", path);
        logger.trace("Calling Files.writeString()");
        Files.writeString(path, json);
        logger.debug("File written successfully");
        logger.trace("Bytes written: {}", json.getBytes().length);
        
        logger.debug("Setting file path on playlist object");
        logger.trace("Calling playlist.setFilePath({})", filePath);
        playlist.setFilePath(filePath);
        logger.trace("File path set on playlist");
        
        logger.info("Playlist '{}' saved successfully to {}", playlist.getName(), filePath);
        logger.trace("save() method exit");
    }
    
    /**
     * Loads a playlist from a JSON file.
     */
    public Playlist load(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }
        
        String json = Files.readString(path);
        JsonObject root = gson.fromJson(json, JsonObject.class);
        
        String name = root.has("name") ? root.get("name").getAsString() : "Untitled";
        Playlist playlist = new Playlist(name);
        playlist.setFilePath(filePath);
        
        if (root.has("cues")) {
            JsonArray cuesArray = root.getAsJsonArray("cues");
            for (int i = 0; i < cuesArray.size(); i++) {
                JsonObject cueObj = cuesArray.get(i).getAsJsonObject();
                
                Cue cue = new Cue();
                cue.setNumber(cueObj.has("number") ? cueObj.get("number").getAsInt() : i + 1);
                cue.setName(cueObj.has("name") ? cueObj.get("name").getAsString() : "");
                cue.setDuration(cueObj.has("duration") ? cueObj.get("duration").getAsDouble() : 0.0);
                cue.setPreWait(cueObj.has("preWait") ? cueObj.get("preWait").getAsDouble() : 0.0);
                cue.setPostWait(cueObj.has("postWait") ? cueObj.get("postWait").getAsDouble() : 0.0);
                cue.setAutoFollow(cueObj.has("autoFollow") ? cueObj.get("autoFollow").getAsBoolean() : false);
                cue.setFilePath(cueObj.has("filePath") ? cueObj.get("filePath").getAsString() : "");
                
                playlist.addCue(cue);
            }
        }
        
        return playlist;
    }
    
    /**
     * Creates a new empty playlist.
     */
    public Playlist createNew(String name) {
        return new Playlist(name);
    }
}
