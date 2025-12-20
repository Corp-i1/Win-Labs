package com.winlabs.model;

/**
 * Available themes for the application UI.
 */
public enum Theme {
    DARK("Dark", "/css/dark-theme.css"),
    LIGHT("Light", "/css/light-theme.css"),
    RAINBOW("Rainbow", "/css/rainbow-theme.css");
    
    private final String displayName;
    private final String cssPath;
    
    Theme(String displayName, String cssPath) {
        this.displayName = displayName;
        this.cssPath = cssPath;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCssPath() {
        return cssPath;
    }
}
