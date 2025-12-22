package com.winlabs.model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings for application-level preferences.
 * These settings apply globally to the application and are not specific to a workspace/playlist.
 * Includes default values for cue properties used across playlists.
 */
public class ApplicationSettings {
    
    // Appearance settings
    private final StringProperty theme;
    
    // General settings
    private final BooleanProperty autoSaveEnabled;
    private final IntegerProperty autoSaveInterval; // in seconds
    
    // Recent files settings
    private final List<String> recentFiles;
    private final Set<String> pinnedPlaylists;
    private static final int MAX_RECENT_FILES = 10;
    
    // Logging settings
    private final BooleanProperty loggingEnabled;
    private final ObjectProperty<LogLevel> logLevel;
    private final StringProperty logDirectory;
    private final IntegerProperty logRotationSizeMB;
    private final IntegerProperty logRetentionDays;
    
    // Default cue properties
    private final DoubleProperty preWaitDefault;
    private final DoubleProperty postWaitDefault;
    private final BooleanProperty autoFollowDefault;
    
    /**
     * Creates default application settings.
     */
    public ApplicationSettings() {
        this.theme = new SimpleStringProperty("dark");
        this.autoSaveEnabled = new SimpleBooleanProperty(false);
        this.autoSaveInterval = new SimpleIntegerProperty(300); // 5 minutes
        this.recentFiles = new ArrayList<>();
        this.pinnedPlaylists = new HashSet<>();
        
        // Initialize logging settings with defaults
        this.loggingEnabled = new SimpleBooleanProperty(true);
        this.logLevel = new SimpleObjectProperty<>(LogLevel.INFO);
        String userHome = System.getProperty("user.home");
        this.logDirectory = new SimpleStringProperty(userHome + "/.winlabs/logs");
        this.logRotationSizeMB = new SimpleIntegerProperty(10);
        this.logRetentionDays = new SimpleIntegerProperty(5);
        
        this.preWaitDefault = new SimpleDoubleProperty(0.0);
        this.postWaitDefault = new SimpleDoubleProperty(0.0);
        this.autoFollowDefault = new SimpleBooleanProperty(false);
    }
    
    // Theme property
    public StringProperty themeProperty() {
        return theme;
    }
    
    public String getTheme() {
        return theme.get();
    }
    
    public void setTheme(String theme) {
        this.theme.set(theme);
    }
    
    // Auto-save enabled property
    public BooleanProperty autoSaveEnabledProperty() {
        return autoSaveEnabled;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled.get();
    }
    
    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled.set(enabled);
    }
    
    // Auto-save interval property
    public IntegerProperty autoSaveIntervalProperty() {
        return autoSaveInterval;
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval.get();
    }
    
    public void setAutoSaveInterval(int interval) {
        this.autoSaveInterval.set(Math.max(60, interval)); // Minimum 1 minute
    }
    
    // Pre-wait default property
    public DoubleProperty preWaitDefaultProperty() {
        return preWaitDefault;
    }
    
    public double getPreWaitDefault() {
        return preWaitDefault.get();
    }
    
    public void setPreWaitDefault(double value) {
        this.preWaitDefault.set(Math.max(0.0, value));
    }
    
    // Post-wait default property
    public DoubleProperty postWaitDefaultProperty() {
        return postWaitDefault;
    }
    
    public double getPostWaitDefault() {
        return postWaitDefault.get();
    }
    
    public void setPostWaitDefault(double value) {
        this.postWaitDefault.set(Math.max(0.0, value));
    }
    
    // Auto-follow default property
    public BooleanProperty autoFollowDefaultProperty() {
        return autoFollowDefault;
    }
    
    public boolean isAutoFollowDefault() {
        return autoFollowDefault.get();
    }
    
    public void setAutoFollowDefault(boolean value) {
        this.autoFollowDefault.set(value);
    }
    
    // Logging enabled property
    public BooleanProperty loggingEnabledProperty() {
        return loggingEnabled;
    }
    
    public boolean isLoggingEnabled() {
        return loggingEnabled.get();
    }
    
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled.set(enabled);
    }
    
    // Log level property
    public ObjectProperty<LogLevel> logLevelProperty() {
        return logLevel;
    }
    
    public LogLevel getLogLevel() {
        return logLevel.get();
    }
    
    public void setLogLevel(LogLevel level) {
        this.logLevel.set(level != null ? level : LogLevel.INFO);
    }
    
    // Log directory property
    public StringProperty logDirectoryProperty() {
        return logDirectory;
    }
    
    public String getLogDirectory() {
        return logDirectory.get();
    }
    
    public void setLogDirectory(String directory) {
        this.logDirectory.set(directory != null ? directory : System.getProperty("user.home") + "/.winlabs/logs");
    }
    
    // Log rotation size property
    public IntegerProperty logRotationSizeMBProperty() {
        return logRotationSizeMB;
    }
    
    public int getLogRotationSizeMB() {
        return logRotationSizeMB.get();
    }
    
    public void setLogRotationSizeMB(int sizeMB) {
        this.logRotationSizeMB.set(Math.max(1, sizeMB)); // Minimum 1 MB
    }
    
    // Log retention days property
    public IntegerProperty logRetentionDaysProperty() {
        return logRetentionDays;
    }
    
    public int getLogRetentionDays() {
        return logRetentionDays.get();
    }
    
    public void setLogRetentionDays(int days) {
        this.logRetentionDays.set(Math.max(1, days)); // Minimum 1 day
    }
    
    // Recent files methods
    
    /**
     * Gets the list of recent files.
     * @return An unmodifiable view of the recent files list
     */
    public List<String> getRecentFiles() {
        return new ArrayList<>(recentFiles);
    }
    
    /**
     * Sets the recent files list.
     * @param files The list of recent file paths (will be copied)
     */
    public void setRecentFiles(List<String> files) {
        recentFiles.clear();
        if (files != null) {
            // Add files up to MAX_RECENT_FILES limit
            int limit = Math.min(files.size(), MAX_RECENT_FILES);
            for (int i = 0; i < limit; i++) {
                recentFiles.add(files.get(i));
            }
        }
    }
    
    /**
     * Adds a file to the recent files list.
     * If the file already exists, it's moved to the front.
     * If the list exceeds MAX_RECENT_FILES, the oldest entry is removed.
     * Pinned files are NOT added to recent files (they're stored separately).
     * @param filePath The absolute path to the file
     */
    public void addRecentFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        
        // Don't add pinned files to recent files - they're stored separately
        if (pinnedPlaylists.contains(filePath)) {
            return;
        }
        
        // Remove if already exists (will be re-added at front)
        recentFiles.remove(filePath);
        
        // Add to front of list
        recentFiles.add(0, filePath);
        
        // Trim to max size
        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
    }
    
    /**
     * Removes a file from the recent files list.
     * @param filePath The absolute path to the file to remove
     */
    public void removeRecentFile(String filePath) {
        recentFiles.remove(filePath);
    }
    
    /**
     * Clears all recent files.
     */
    public void clearRecentFiles() {
        recentFiles.clear();
    }
    
    // Pinned playlists methods
    
    /**
     * Gets the set of pinned playlist paths.
     * @return A copy of the pinned playlists set
     */
    public Set<String> getPinnedPlaylists() {
        return new HashSet<>(pinnedPlaylists);
    }
    
    /**
     * Sets the pinned playlists set.
     * @param playlists The set of pinned playlist paths (will be copied)
     */
    public void setPinnedPlaylists(Set<String> playlists) {
        pinnedPlaylists.clear();
        if (playlists != null) {
            pinnedPlaylists.addAll(playlists);
        }
    }
    
    /**
     * Checks if a playlist is pinned.
     * @param filePath The absolute path to the playlist file
     * @return true if the playlist is pinned, false otherwise
     */
    public boolean isPinned(String filePath) {
        return pinnedPlaylists.contains(filePath);
    }
    
    /**
     * Toggles the pinned state of a playlist.
     * When pinning, the file is removed from recent files (stored separately).
     * When unpinning, the file is added back to recent files.
     * @param filePath The absolute path to the playlist file
     * @return true if now pinned, false if now unpinned
     */
    public boolean togglePinned(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        if (pinnedPlaylists.contains(filePath)) {
            // Unpinning - remove from pinned and add back to recent
            pinnedPlaylists.remove(filePath);
            addRecentFile(filePath);
            return false;
        } else {
            // Pinning - add to pinned and remove from recent
            pinnedPlaylists.add(filePath);
            recentFiles.remove(filePath);
            return true;
        }
    }
    
    /**
     * Clears all pinned playlists.
     */
    public void clearPinnedPlaylists() {
        pinnedPlaylists.clear();
    }
    
    /**
     * Resets all settings to default values.
     */
    public void resetToDefaults() {
        setTheme("dark");
        setAutoSaveEnabled(false);
        setAutoSaveInterval(300);
        setPreWaitDefault(0.0);
        setPostWaitDefault(0.0);
        setAutoFollowDefault(false);
        setLoggingEnabled(true);
        setLogLevel(LogLevel.INFO);
        setLogDirectory(System.getProperty("user.home") + "/.winlabs/logs");
        setLogRotationSizeMB(10);
        setLogRetentionDays(5);
        clearRecentFiles();
        clearPinnedPlaylists();
    }
}
