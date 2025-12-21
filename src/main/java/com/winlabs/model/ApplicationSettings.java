package com.winlabs.model;

import javafx.beans.property.*;

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
    }
}
