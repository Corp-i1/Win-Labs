package com.winlabs.model;

/**
 * Enumeration of available log levels for the application logging system.
 * Maps to SLF4J log levels.
 */
public enum LogLevel {
    TRACE("Trace"),
    DEBUG("Debug"),
    INFO("Info"),
    WARN("Warn"),
    ERROR("Error");
    
    private final String displayName;
    
    LogLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
