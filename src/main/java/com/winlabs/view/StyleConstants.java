package com.winlabs.view;

/**
 * Centralized collection of CSS style constants used throughout the application.
 * 
 * This class eliminates magic CSS strings scattered throughout the UI code,
 * making it easier to maintain consistent styling and update styles globally.
 */
public final class StyleConstants {
    
    // Label styles
    /** Bold label style for section headers (14px, bold) */
    public static final String SECTION_HEADER_LABEL = "-fx-font-weight: bold; -fx-font-size: 14px;";
    
    // Status bar styles
    /** Status bar background color (dark gray) */
    public static final String STATUS_BAR_BACKGROUND = "-fx-background-color: #2d2d30;";
    
    /** Status bar text color (white) */
    public static final String STATUS_TEXT_WHITE = "-fx-text-fill: white;";
    
    /** File view toggle button style (transparent background, white text) */
    public static final String FILE_VIEW_TOGGLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px;";
    
    // Error and warning styles
    /** Error message label style (red text) */
    public static final String ERROR_TEXT = "-fx-text-fill: #ff4444;";
    
    /** Warning message label style (orange text) */
    public static final String WARNING_TEXT = "-fx-text-fill: #ffaa00;";
    
    // Theme resource paths
    /** Template for loading theme CSS files with String.format. */
    public static final String THEME_CSS_PATH_TEMPLATE = "/css/%s-theme.css";

    private StyleConstants() {
        // Utility class - no instantiation
    }
    
    /**
     * Gets the theme CSS path for the specified theme name.
     * 
     * @param themeName the name of the theme (e.g., "dark", "light", "rainbow")
     * @return the resource path to the theme CSS file
     */
    public static String getThemeCssPath(String themeName) {
        return String.format("/css/%s-theme.css", themeName);
    }
}
