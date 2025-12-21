package com.winlabs;

import com.winlabs.service.FileAssociationService;
import com.winlabs.service.LoggerService;
import com.winlabs.service.SettingsService;
import com.winlabs.model.Settings;
import com.winlabs.view.MainWindow;
import com.winlabs.view.WelcomeScreen;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Main entry point for Win-Labs application.
 * A cross-platform cue list manager for sound technicians.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String playlistFileToOpen;
    private WelcomeScreen welcomeScreen;
    
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
        
        // Configure logging based on settings
        LoggerService.configureLogging(settings);
        logger.info("Win-Labs application starting (v1.0.0)");
        logger.debug("Settings loaded: theme={}, loggingEnabled={}, logLevel={}", 
            settings.getTheme(), settings.isLoggingEnabled(), settings.getLogLevel());
        
        // If a playlist file was passed as argument, open it directly
        if (playlistFileToOpen != null) {
            logger.info("Opening playlist file from command line: {}", playlistFileToOpen);
            MainWindow mainWindow = new MainWindow();
            mainWindow.openPlaylistFile(playlistFileToOpen);
            mainWindow.show();
        } else {
            logger.debug("Showing welcome screen");
            // Show welcome screen at startup (with theme applied)
            welcomeScreen = new WelcomeScreen(
                this::showNewPlaylistWindow,
                this::showOpenPlaylistWindow,
                this::showSettings,
                this::showDocumentation,
                this::closeApplication
            );
            // Apply theme AFTER the stage is created but before showing
            welcomeScreen.applyTheme(settings.getTheme());
            // Load and display recent playlists
            // TODO: Implement recent playlists storage in ApplicationSettings
            welcomeScreen.updateRecentPlaylists(new ArrayList<>());
            welcomeScreen.show();
        }
    }
    
    /**
     * Opens a new playlist window.
     * Only closes welcome screen if user confirms the dialog.
     */
    private void showNewPlaylistWindow() {
        try {
            MainWindow mainWindow = new MainWindow();
            mainWindow.newPlaylist(true); // Skip confirmation - nothing to lose from welcome screen
            // Check if a playlist was actually created (user didn't cancel)
            if (mainWindow.hasPlaylistLoaded()) {
                mainWindow.show();
                if (welcomeScreen != null) {
                    welcomeScreen.closeWelcomeScreen();
                }
            } else {
                // User cancelled - close the empty window
                mainWindow.close();
            }
        } catch (Exception e) {
            System.err.println("Error creating new playlist: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Opens a playlist from the file browser.
     * Only closes welcome screen if user actually selects a file.
     */
    private void showOpenPlaylistWindow() {
        try {
            MainWindow mainWindow = new MainWindow();
            mainWindow.openPlaylist();
            // Only show the window if a playlist was successfully loaded
            if (mainWindow.hasPlaylistLoaded()) {
                mainWindow.show();
                if (welcomeScreen != null) {
                    welcomeScreen.closeWelcomeScreen();
                }
            } else {
                // User cancelled the file dialog - close the empty window, keep welcome screen
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
        try {
            SettingsService settingsService = new SettingsService();
            Settings settings = settingsService.load();
            MainWindow tempWindow = new MainWindow();
            tempWindow.openSettings(settings, settingsService);
        } catch (Exception e) {
            System.err.println("Error opening settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Shows documentation.
     */
    private void showDocumentation() {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Win-Labs Documentation");
            alert.setHeaderText("Help & Documentation");
            alert.setContentText("Documentation is available at:\n" +
                "https://github.com/Corp-i1/Win-Labs\n\n" +
                "Features:\n" +
                "• Create and manage audio cue lists\n" +
                "• Set pre-wait and post-wait timers\n" +
                "• Auto-follow functionality\n" +
                "• Multi-track playback\n" +
                "• Theme customization\n\n" +
                "For more information, visit the repository.");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing documentation: " + e.getMessage());
        }
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


