package com.winlabs.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for loading and saving playlists in JSON format.
 */
public class PlaylistService {
    
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
            throw new IllegalArgumentException("Playlist cannot be null");
        }
        
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        JsonObject root = new JsonObject();
        root.addProperty("name", playlist.getName());
        root.addProperty("version", "1.0");
        
        JsonArray cuesArray = new JsonArray();
        for (Cue cue : playlist.getCues()) {
            JsonObject cueObj = new JsonObject();
            cueObj.addProperty("number", cue.getNumber());
            cueObj.addProperty("name", cue.getName());
            cueObj.addProperty("duration", cue.getDuration());
            cueObj.addProperty("preWait", cue.getPreWait());
            cueObj.addProperty("postWait", cue.getPostWait());
            cueObj.addProperty("autoFollow", cue.isAutoFollow());
            cueObj.addProperty("filePath", cue.getFilePath());
            cuesArray.add(cueObj);
        }
        
        root.add("cues", cuesArray);
        
        String json = gson.toJson(root);
        Path path = Paths.get(filePath);
        Files.writeString(path, json);
        
        playlist.setFilePath(filePath);
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
