package com.winlabs.model;

import javafx.beans.property.*;

/**
 * Playlist-specific settings that are saved with each playlist (.wlp file).
 * These settings are playlist-dependent and may vary between different playlists.
 */
public class PlaylistSettings {
    
    // Playlist-specific audio settings
    private final DoubleProperty masterVolume;
    private final StringProperty audioFileDirectory;
    
    // Playlist-specific cue defaults (can override app defaults)
    private final DoubleProperty defaultPreWait;
    private final DoubleProperty defaultPostWait;
    private final BooleanProperty defaultAutoFollow;
    
    // Metadata
    private final StringProperty playlistName;
    private final LongProperty lastModified;
    private final BooleanProperty isPinned;
    
    public PlaylistSettings() {
        this.masterVolume = new SimpleDoubleProperty(1.0);
        this.audioFileDirectory = new SimpleStringProperty("");
        this.defaultPreWait = new SimpleDoubleProperty(0.0);
        this.defaultPostWait = new SimpleDoubleProperty(0.0);
        this.defaultAutoFollow = new SimpleBooleanProperty(false);
        this.playlistName = new SimpleStringProperty("");
        this.lastModified = new SimpleLongProperty(System.currentTimeMillis());
        this.isPinned = new SimpleBooleanProperty(false);
    }
    
    // Master Volume (0.0 - 1.0)
    public DoubleProperty masterVolumeProperty() {
        return masterVolume;
    }
    
    public double getMasterVolume() {
        return masterVolume.get();
    }
    
    public void setMasterVolume(double volume) {
        masterVolume.set(Math.max(0.0, Math.min(1.0, volume)));
    }
    
    // Audio File Directory
    public StringProperty audioFileDirectoryProperty() {
        return audioFileDirectory;
    }
    
    public String getAudioFileDirectory() {
        return audioFileDirectory.get();
    }
    
    public void setAudioFileDirectory(String directory) {
        audioFileDirectory.set(directory);
    }
    
    // Default Pre-Wait (in seconds)
    public DoubleProperty defaultPreWaitProperty() {
        return defaultPreWait;
    }
    
    public double getDefaultPreWait() {
        return defaultPreWait.get();
    }
    
    public void setDefaultPreWait(double seconds) {
        defaultPreWait.set(Math.max(0.0, seconds));
    }
    
    // Default Post-Wait (in seconds)
    public DoubleProperty defaultPostWaitProperty() {
        return defaultPostWait;
    }
    
    public double getDefaultPostWait() {
        return defaultPostWait.get();
    }
    
    public void setDefaultPostWait(double seconds) {
        defaultPostWait.set(Math.max(0.0, seconds));
    }
    
    // Default Auto-Follow
    public BooleanProperty defaultAutoFollowProperty() {
        return defaultAutoFollow;
    }
    
    public boolean isDefaultAutoFollow() {
        return defaultAutoFollow.get();
    }
    
    public void setDefaultAutoFollow(boolean autoFollow) {
        defaultAutoFollow.set(autoFollow);
    }
    
    // Playlist Name
    public StringProperty playlistNameProperty() {
        return playlistName;
    }
    
    public String getPlaylistName() {
        return playlistName.get();
    }
    
    public void setPlaylistName(String name) {
        playlistName.set(name);
    }
    
    // Last Modified timestamp
    public LongProperty lastModifiedProperty() {
        return lastModified;
    }
    
    public long getLastModified() {
        return lastModified.get();
    }
    
    public void setLastModified(long timestamp) {
        lastModified.set(timestamp);
    }
    
    // Pinned status
    public BooleanProperty isPinnedProperty() {
        return isPinned;
    }
    
    public boolean isPinned() {
        return isPinned.get();
    }
    
    public void setIsPinned(boolean pinned) {
        isPinned.set(pinned);
    }
    
    /**
     * Resets all settings to their defaults.
     */
    public void resetToDefaults() {
        setMasterVolume(1.0);
        setAudioFileDirectory("");
        setDefaultPreWait(0.0);
        setDefaultPostWait(0.0);
        setDefaultAutoFollow(false);
        setLastModified(System.currentTimeMillis());
    }
}
