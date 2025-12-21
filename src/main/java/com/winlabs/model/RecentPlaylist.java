package com.winlabs.model;

import javafx.beans.property.*;
import java.nio.file.Path;

/**
 * Metadata for a recent playlist used in the WelcomeScreen.
 * Tracks playlist information for display and quick access.
 */
public class RecentPlaylist {
    
    private final StringProperty playlistName;
    private final StringProperty playlistPath;  // Full path to .json file
    private final LongProperty lastOpened;
    private final BooleanProperty isPinned;
    
    public RecentPlaylist(String name, String path) {
        this.playlistName = new SimpleStringProperty(name);
        this.playlistPath = new SimpleStringProperty(path);
        this.lastOpened = new SimpleLongProperty(System.currentTimeMillis());
        this.isPinned = new SimpleBooleanProperty(false);
    }
    
    public RecentPlaylist(String name, Path path) {
        this(name, path.toString());
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
    
    // Playlist Path
    public StringProperty playlistPathProperty() {
        return playlistPath;
    }
    
    public String getPlaylistPath() {
        return playlistPath.get();
    }
    
    public void setPlaylistPath(String path) {
        playlistPath.set(path);
    }
    
    // Last Opened timestamp
    public LongProperty lastOpenedProperty() {
        return lastOpened;
    }
    
    public long getLastOpened() {
        return lastOpened.get();
    }
    
    public void setLastOpened(long timestamp) {
        lastOpened.set(timestamp);
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
     * Updates the last opened timestamp to current time.
     */
    public void updateLastOpened() {
        setLastOpened(System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return playlistName.get() + " (" + playlistPath.get() + ")";
    }
}
