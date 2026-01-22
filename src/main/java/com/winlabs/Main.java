package com.winlabs;

import com.winlabs.model.RecentPlaylist;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            logger.error("Failed to load settings, using defaults: {}", e.getMessage(), e);
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
            
            // Set callbacks for opening recent playlists and toggling pins
            welcomeScreen.setOnOpenRecentPlaylist((filePath) -> {
                try {
                    MainWindow mainWindow = new MainWindow();
                    mainWindow.openPlaylistFile(filePath);
                    mainWindow.show();
                    if (welcomeScreen != null) {
                        welcomeScreen.closeWelcomeScreen();
                    }
                } catch (Exception e) {
                    logger.error("Failed to open recent playlist: {}", filePath, e);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to Open Playlist");
                    alert.setContentText("Could not open playlist: " + e.getMessage());
                    alert.showAndWait();
                }
            });
            
            welcomeScreen.setOnTogglePin((filePath) -> {
                try {
                    Settings updatedSettings = settingsService.load();
                    updatedSettings.togglePinned(filePath);
                    settingsService.save(updatedSettings);
                    logger.info("Toggled pin status for: {}", filePath);
                    
                    // Refresh the welcome screen with updated pinned/recent lists
                    List<RecentPlaylist> refreshedPlaylists = new ArrayList<>();
                    
                    // Load pinned playlists
                    Set<String> pinnedFiles = updatedSettings.getPinnedPlaylists();
                    if (pinnedFiles != null) {
                        for (String path : pinnedFiles) {
                            try {
                                Path p = Paths.get(path);
                                String fileName = p.getFileName().toString();
                                String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                                RecentPlaylist playlist = new RecentPlaylist(playlistName, path);
                                playlist.setIsPinned(true);
                                refreshedPlaylists.add(playlist);
                            } catch (Exception e) {
                                logger.warn("Failed to add pinned playlist: {}", path, e);
                            }
                        }
                    }
                    
                    // Load recent playlists
                    List<String> recentFiles = updatedSettings.getRecentFiles();
                    if (recentFiles != null) {
                        for (String path : recentFiles) {
                            if (pinnedFiles != null && pinnedFiles.contains(path)) {
                                continue;
                            }
                            try {
                                Path p = Paths.get(path);
                                String fileName = p.getFileName().toString();
                                String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                                RecentPlaylist playlist = new RecentPlaylist(playlistName, path);
                                playlist.setIsPinned(false);
                                refreshedPlaylists.add(playlist);
                            } catch (Exception e) {
                                logger.warn("Failed to add recent playlist: {}", path, e);
                            }
                        }
                    }
                    
                    welcomeScreen.updateRecentPlaylists(refreshedPlaylists);
                } catch (Exception e) {
                    logger.error("Failed to toggle pin status: {}", filePath, e);
                }
            });
            
            // Load and display recent playlists and pinned playlists
            List<RecentPlaylist> allPlaylists = new ArrayList<>();
            
            // First, add all pinned playlists
            Set<String> pinnedFiles = settings.getPinnedPlaylists();
            logger.info("Loading pinned playlists at startup: {}", pinnedFiles != null ? pinnedFiles.size() : 0);
            if (pinnedFiles != null) {
                for (String filePath : pinnedFiles) {
                    try {
                        Path path = Paths.get(filePath);
                        String fileName = path.getFileName().toString();
                        String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                        
                        RecentPlaylist playlist = new RecentPlaylist(playlistName, filePath);
                        playlist.setIsPinned(true);
                        allPlaylists.add(playlist);
                        logger.debug("Added pinned playlist at startup: {}", filePath);
                    } catch (Exception e) {
                        logger.warn("Failed to add pinned playlist at startup: {}", filePath, e);
                    }
                }
            }
            
            // Then, add recent (non-pinned) playlists
            List<String> recentFiles = settings.getRecentFiles();
            logger.info("Loading recent files at startup: {}", recentFiles != null ? recentFiles.size() : 0);
            if (recentFiles != null) {
                for (String filePath : recentFiles) {
                    // Skip if already added as pinned
                    if (pinnedFiles != null && pinnedFiles.contains(filePath)) {
                        continue;
                    }
                    
                    try {
                        Path path = Paths.get(filePath);
                        String fileName = path.getFileName().toString();
                        String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                        
                        RecentPlaylist playlist = new RecentPlaylist(playlistName, filePath);
                        playlist.setIsPinned(false);
                        allPlaylists.add(playlist);
                    } catch (Exception e) {
                        logger.warn("Failed to add recent playlist at startup: {}", filePath, e);
                    }
                }
            }
            
            welcomeScreen.updateRecentPlaylists(allPlaylists);
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
            logger.error("Error creating new playlist: {}", e.getMessage(), e);
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
            logger.error("Error opening playlist: {}", e.getMessage(), e);
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
            logger.error("Error opening settings: {}", e.getMessage(), e);
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
            logger.error("Error showing documentation: {}", e.getMessage(), e);
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


