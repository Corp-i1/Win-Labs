package com.winlabs;

import com.winlabs.service.FileAssociationService;
import com.winlabs.service.SettingsService;
import com.winlabs.model.Settings;
import com.winlabs.view.MainWindow;
import com.winlabs.view.WelcomeScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for Win-Labs application.
 * A cross-platform cue list manager for sound technicians.
 */
public class Main extends Application {
    
    private static String playlistFileToOpen;
    
    @Override
    public void start(Stage primaryStage) {
        // Load settings to check startup mode
        SettingsService settingsService = new SettingsService();
        Settings settings;
        try {
            settings = settingsService.load();
        } catch (Exception e) {
            System.err.println("Failed to load settings, using defaults: " + e.getMessage());
            settings = new Settings();
        }
        
        // If a playlist file was passed as argument, open it directly
        if (playlistFileToOpen != null) {
            MainWindow mainWindow = new MainWindow();
            mainWindow.openPlaylistFile(playlistFileToOpen);
            mainWindow.show();
        } else {
            // Show welcome screen at startup (with theme applied)
            WelcomeScreen welcomeScreen = new WelcomeScreen(
                this::showNewPlaylistWindow,
                this::showOpenPlaylistWindow,
                this::showSettings,
                this::showDocumentation,
                this::closeApplication
            );
            welcomeScreen.applyTheme(settings.getTheme());
            welcomeScreen.show();
        }
    }
    
    /**
     * Opens a new playlist window.
     */
    private void showNewPlaylistWindow() {
        try {
            MainWindow mainWindow = new MainWindow();
            mainWindow.newPlaylist();
            mainWindow.show();
        } catch (Exception e) {
            System.err.println("Error creating new playlist: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Opens a playlist from the file browser.
     */
    private void showOpenPlaylistWindow() {
        try {
            MainWindow mainWindow = new MainWindow();
            mainWindow.openPlaylist();
            // Only show the window if a playlist was successfully loaded
            if (mainWindow.hasPlaylistLoaded()) {
                mainWindow.show();
            } else {
                // User cancelled the file dialog, close the window
                mainWindow.close();
            }
        } catch (Exception e) {
            System.err.println("Error opening playlist: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Shows the settings window.
     */
    private void showSettings() {
        // Settings dialog will be shown from WelcomeScreen
        // This is a placeholder for future expansion
    }
    
    /**
     * Shows documentation.
     */
    private void showDocumentation() {
        // Documentation will be shown from WelcomeScreen
        // This is a placeholder for future expansion
    }
    
    /**
     * Closes the application.
     */
    private void closeApplication() {
        System.exit(0);
    }
    
    public static void main(String[] args) {
        // Check if a .wlp file was passed as argument
        if (args.length > 0) {
            playlistFileToOpen = FileAssociationService.extractPlaylistFileFromArgs(args);
        }
        
        launch(args);
    }
}


