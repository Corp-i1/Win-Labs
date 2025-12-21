package com.winlabs.model;

/**
 * Container for both application and workspace settings.
 * Provides backward compatibility with legacy Settings API.
 * 
 * For new code, use ApplicationSettings and WorkspaceSettings directly.
 */
public class Settings {
    
    private final ApplicationSettings applicationSettings;
    private final WorkspaceSettings workspaceSettings;
    
    /**
     * Creates default settings.
     */
    public Settings() {
        this.applicationSettings = new ApplicationSettings();
        this.workspaceSettings = new WorkspaceSettings();
    }
    
    /**
     * Gets the application settings.
     */
    public ApplicationSettings getApplicationSettings() {
        return applicationSettings;
    }
    
    /**
     * Gets the workspace settings.
     */
    public WorkspaceSettings getWorkspaceSettings() {
        return workspaceSettings;
    }
    
    // ===== Delegating methods for backward compatibility =====
    
    public String getTheme() {
        return applicationSettings.getTheme();
    }
    
    public void setTheme(String theme) {
        applicationSettings.setTheme(theme);
    }
    
    public double getMasterVolume() {
        return workspaceSettings.getMasterVolume();
    }
    
    public void setMasterVolume(double volume) {
        workspaceSettings.setMasterVolume(volume);
    }
    
    public boolean isAutoSaveEnabled() {
        return applicationSettings.isAutoSaveEnabled();
    }
    
    public void setAutoSaveEnabled(boolean enabled) {
        applicationSettings.setAutoSaveEnabled(enabled);
    }
    
    public int getAutoSaveInterval() {
        return applicationSettings.getAutoSaveInterval();
    }
    
    public void setAutoSaveInterval(int interval) {
        applicationSettings.setAutoSaveInterval(interval);
    }
    
    public String getLastPlaylistPath() {
        return workspaceSettings.getLastPlaylistPath();
    }
    
    public void setLastPlaylistPath(String path) {
        workspaceSettings.setLastPlaylistPath(path);
    }
    
    public String getAudioFileDirectory() {
        return workspaceSettings.getAudioFileDirectory();
    }
    
    public void setAudioFileDirectory(String directory) {
        workspaceSettings.setAudioFileDirectory(directory);
    }
    
    public double getPreWaitDefault() {
        return applicationSettings.getPreWaitDefault();
    }
    
    public void setPreWaitDefault(double value) {
        applicationSettings.setPreWaitDefault(value);
    }
    
    public double getPostWaitDefault() {
        return applicationSettings.getPostWaitDefault();
    }
    
    public void setPostWaitDefault(double value) {
        applicationSettings.setPostWaitDefault(value);
    }
    
    public boolean isAutoFollowDefault() {
        return applicationSettings.isAutoFollowDefault();
    }
    
    public void setAutoFollowDefault(boolean value) {
        applicationSettings.setAutoFollowDefault(value);
    }
    
    /**
     * Resets all settings to default values.
     */
    public void resetToDefaults() {
        applicationSettings.resetToDefaults();
        workspaceSettings.resetToDefaults();
    }
}
