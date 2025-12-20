package com.winlabs.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a collection of cues (a playlist).
 * Uses ObservableList for automatic UI updates when cues are added/removed.
 */
public class Playlist {
    private final ObservableList<Cue> cues;
    private String name;
    private String filePath;
    
    /**
     * Creates a new empty playlist.
     */
    public Playlist() {
        this.cues = FXCollections.observableArrayList();
        this.name = "Untitled Playlist";
        this.filePath = null;
    }
    
    /**
     * Creates a new playlist with a name.
     */
    public Playlist(String name) {
        this();
        this.name = name;
    }
    
    /**
     * Adds a cue to the playlist.
     */
    public void addCue(Cue cue) {
        cues.add(cue);
    }
    
    /**
     * Adds a cue at a specific index.
     */
    public void addCue(int index, Cue cue) {
        cues.add(index, cue);
    }
    
    /**
     * Removes a cue from the playlist.
     */
    public void removeCue(Cue cue) {
        cues.remove(cue);
    }
    
    /**
     * Removes a cue at a specific index.
     */
    public void removeCue(int index) {
        cues.remove(index);
    }
    
    /**
     * Gets a cue at a specific index.
     */
    public Cue getCue(int index) {
        return cues.get(index);
    }
    
    /**
     * Gets the observable list of cues.
     * Can be bound directly to TableView.
     */
    public ObservableList<Cue> getCues() {
        return cues;
    }
    
    /**
     * Returns the number of cues in the playlist.
     */
    public int size() {
        return cues.size();
    }
    
    /**
     * Clears all cues from the playlist.
     */
    public void clear() {
        cues.clear();
    }
    
    /**
     * Renumbers all cues sequentially starting from 1.
     */
    public void renumberCues() {
        for (int i = 0; i < cues.size(); i++) {
            cues.get(i).setNumber(i + 1);
        }
    }
    
    // Name getters/setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    // FilePath getters/setters
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public String toString() {
        return String.format("Playlist: %s (%d cues)", name, cues.size());
    }
}
