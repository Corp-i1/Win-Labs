package com.winlabs.model;

import javafx.beans.property.*;

/**
 * Model class for application settings.
 * Uses JavaFX properties for reactive UI binding.
 */
public class Settings {
    
    // Appearance settings
    private final StringProperty theme;
    
    // Audio settings
    private final DoubleProperty masterVolume;
    private final BooleanProperty enableMultiTrackPlayback;
    
    // General settings
    private final BooleanProperty autoSaveEnabled;
    private final IntegerProperty autoSaveInterval; // in seconds
    
    /**
     * Creates default settings.
     */
    public Settings() {
        this.theme = new SimpleStringProperty("dark");
        this.masterVolume = new SimpleDoubleProperty(1.0); // 0.0 to 1.0
        this.enableMultiTrackPlayback = new SimpleBooleanProperty(true);
        this.autoSaveEnabled = new SimpleBooleanProperty(false);
        this.autoSaveInterval = new SimpleIntegerProperty(300); // 5 minutes
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
    
    // Multi-track playback property
    public BooleanProperty enableMultiTrackPlaybackProperty() {
        return enableMultiTrackPlayback;
    }
    
    public boolean isEnableMultiTrackPlayback() {
        return enableMultiTrackPlayback.get();
    }
    
    public void setEnableMultiTrackPlayback(boolean enable) {
        this.enableMultiTrackPlayback.set(enable);
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
    
    /**
     * Resets all settings to default values.
     */
    public void resetToDefaults() {
        setTheme("dark");
        setMasterVolume(1.0);
        setEnableMultiTrackPlayback(true);
        setAutoSaveEnabled(false);
        setAutoSaveInterval(300);
    }
}
