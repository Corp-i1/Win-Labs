package com.winlabs.model;

import javafx.beans.property.*;

/**
 * Settings for workspace/playlist-specific preferences.
 * These settings apply to the current workspace and can vary between different playlists.
 */
public class WorkspaceSettings {
    
    // Audio settings
    private final DoubleProperty masterVolume;
    
    // Workspace settings
    private final StringProperty lastPlaylistPath;
    private final StringProperty audioFileDirectory;
    
    /**
     * Creates default workspace settings.
     */
    public WorkspaceSettings() {
        this.masterVolume = new SimpleDoubleProperty(1.0); // 0.0 to 1.0
        this.lastPlaylistPath = new SimpleStringProperty("");
        this.audioFileDirectory = new SimpleStringProperty(System.getProperty("user.home"));
    }
    
    // Master volume property
    public DoubleProperty masterVolumeProperty() {
        return masterVolume;
    }
    
    public double getMasterVolume() {
        return masterVolume.get();
    }
    
    public void setMasterVolume(double volume) {
        this.masterVolume.set(Math.max(0.0, Math.min(1.0, volume)));
    }
    
    // Last playlist path property
    public StringProperty lastPlaylistPathProperty() {
        return lastPlaylistPath;
    }
    
    public String getLastPlaylistPath() {
        return lastPlaylistPath.get();
    }
    
    public void setLastPlaylistPath(String path) {
        this.lastPlaylistPath.set(path);
    }
    
    // Audio file directory property
    public StringProperty audioFileDirectoryProperty() {
        return audioFileDirectory;
    }
    
    public String getAudioFileDirectory() {
        return audioFileDirectory.get();
    }
    
    public void setAudioFileDirectory(String directory) {
        this.audioFileDirectory.set(directory);
    }
    
    /**
     * Resets all settings to default values.
     */
    public void resetToDefaults() {
        setMasterVolume(1.0);
        setLastPlaylistPath("");
        setAudioFileDirectory(System.getProperty("user.home"));
    }
}
