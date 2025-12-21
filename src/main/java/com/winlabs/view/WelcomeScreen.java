package com.winlabs.view;

import com.winlabs.model.RecentPlaylist;
import com.winlabs.view.components.RecentPlaylistCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Welcome screen displayed at application startup as a separate window.
 * Shows recent playlists and application controls (new, open, settings, help, exit).
 * Features two-column layout: actions on left, recent playlists on right.
 */
public class WelcomeScreen extends Stage {
    
    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreen.class);
    
    private final Runnable onNewPlaylist;
    private final Runnable onOpenPlaylist;
    private final Runnable onOpenSettings;
    private final Runnable onOpenDocumentation;
    private final Runnable onExit;
    
    private VBox recentPlaylistsContainer;
    private boolean playlistSelected = false;
    
    public WelcomeScreen(Runnable onNewPlaylist, Runnable onOpenPlaylist, 
                        Runnable onOpenSettings, Runnable onOpenDocumentation, Runnable onExit) {
        // Defensive: validate all callbacks are provided
        this.onNewPlaylist = onNewPlaylist != null ? onNewPlaylist : () -> {};
        this.onOpenPlaylist = onOpenPlaylist != null ? onOpenPlaylist : () -> {};
        this.onOpenSettings = onOpenSettings != null ? onOpenSettings : () -> {};
        this.onOpenDocumentation = onOpenDocumentation != null ? onOpenDocumentation : () -> {};
        this.onExit = onExit != null ? onExit : () -> System.exit(0);
        
        logger.info("WelcomeScreen initialized");
        setTitle("Win-Labs - Welcome");
        setWidth(900);
        setHeight(600);
        setResizable(true);
        setMinWidth(700);
        setMinHeight(500);
        
        initializeUI();
    }
    
    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        
        // Top: Title section
        VBox headerSection = createHeaderSection();
        root.setTop(headerSection);
        
        // Center: Two-column layout (left: actions, right: recent)
        HBox centerSection = createCenterSection();
        root.setCenter(centerSection);
        
        // Bottom: Footer buttons
        VBox footerSection = createFooterSection();
        root.setBottom(footerSection);
        
        Scene scene = new Scene(root);
        setScene(scene);
    }
    
    /**
     * Creates the header section with title.
     */
    private VBox createHeaderSection() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(25));
        header.getStyleClass().add("header-section");
        header.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Win-Labs");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        
        Label subtitleLabel = new Label("Cue List Manager for Sound Technicians");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.getStyleClass().add("subtitle-label");
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
    
    /**
     * Creates the center section with two columns: left (actions) and right (recent).
     */
    private HBox createCenterSection() {
        HBox center = new HBox(20);
        center.setPadding(new Insets(20));
        center.getStyleClass().add("center-section");
        
        // LEFT COLUMN: Action buttons
        VBox leftColumn = createLeftColumn();
        
        // DIVIDER
        Separator divider = new Separator();
        divider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // RIGHT COLUMN: Recent playlists
        VBox rightColumn = createRightColumn();
        
        center.getChildren().addAll(leftColumn, divider, rightColumn);
        
        // Set column sizes
        HBox.setHgrow(leftColumn, Priority.NEVER);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        
        return center;
    }
    
    /**
     * Creates the left column with action buttons.
     */
    private VBox createLeftColumn() {
        VBox column = new VBox(15);
        column.getStyleClass().add("action-column");
        column.setPadding(new Insets(15));
        column.setPrefWidth(250);
        
        Label actionsLabel = new Label("Start");
        actionsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        actionsLabel.getStyleClass().add("section-title");
        
        // New Playlist button
        Button newPlaylistBtn = createLargeActionButton("+ New Playlist");
        newPlaylistBtn.setOnAction(e -> {
            try {
                logger.info("New Playlist button clicked");
                playlistSelected = true;
                onNewPlaylist.run();
                // Don't close here - callback will close if successful
            } catch (Exception ex) {
                logger.error("Failed to create new playlist: {}", ex.getMessage(), ex);
                showError("Failed to create new playlist", ex.getMessage());
                playlistSelected = false;
            }
        });
        
        // Open Playlist button
        logger.trace("Creating Open Playlist button");
        Button openPlaylistBtn = createLargeActionButton("📂 Open Playlist");
        logger.trace("Open Playlist button created: {}", openPlaylistBtn);
        logger.debug("Setting action handler for Open Playlist button");
        openPlaylistBtn.setOnAction(e -> {
            logger.trace("Open Playlist button clicked - event handler entry");
            logger.info("Open Playlist button clicked by user");
            logger.debug("Action event: {}", e);
            
            try {
                logger.debug("Starting playlist open operation");
                // Mark that we're attempting to open
                logger.trace("Current playlistSelected value: {}", playlistSelected);
                logger.debug("Setting playlistSelected to false before open attempt");
                playlistSelected = false;
                logger.trace("playlistSelected set to: {}", playlistSelected);
                
                logger.debug("Calling onOpenPlaylist callback");
                logger.trace("onOpenPlaylist callback: {}", onOpenPlaylist);
                onOpenPlaylist.run();
                logger.info("onOpenPlaylist callback executed successfully");
                // Don't close here - callback will close if successful
                logger.trace("Waiting for callback to handle window closure");
            } catch (Exception ex) {
                logger.error("Failed to open playlist: {}", ex.getMessage(), ex);
                logger.debug("Exception type: {}", ex.getClass().getName());
                logger.trace("Exception stack trace:", ex);
                logger.warn("Displaying error dialog to user");
                showError("Failed to open playlist", ex.getMessage());
                logger.debug("Error dialog shown");
                logger.debug("Resetting playlistSelected to false after error");
                playlistSelected = false;
                logger.trace("playlistSelected reset to: {}", playlistSelected);
            }
            
            logger.trace("Open Playlist button event handler exit");
        });
        logger.debug("Action handler set for Open Playlist button");
        
        column.getChildren().addAll(
            actionsLabel,
            newPlaylistBtn,
            openPlaylistBtn
        );
        
        return column;
    }
    
    /**
     * Creates the right column with recent playlists.
     */
    private VBox createRightColumn() {
        VBox column = new VBox(10);
        column.setPadding(new Insets(10, 15, 10, 15));
        
        Label titleLabel = new Label("Recently Opened");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.getStyleClass().add("section-title");
        
        // Recent playlists container with scroll
        recentPlaylistsContainer = new VBox(8);
        recentPlaylistsContainer.getStyleClass().add("recent-playlists-container");
        recentPlaylistsContainer.setPadding(new Insets(12));
        
        Label placeholderLabel = new Label("No recent playlists");
        placeholderLabel.setStyle("-fx-font-size: 12;");
        placeholderLabel.getStyleClass().add("placeholder-label");
        recentPlaylistsContainer.getChildren().add(placeholderLabel);
        
        ScrollPane scrollPane = new ScrollPane(recentPlaylistsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("recent-playlists-scroll");
        
        column.getChildren().addAll(titleLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return column;
    }
    
    /**
     * Creates the footer section with settings, help, and exit buttons.
     */
    private VBox createFooterSection() {
        VBox footer = new VBox(8);
        footer.setPadding(new Insets(15));
        footer.getStyleClass().add("footer-section");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button settingsBtn = createFooterButton("⚙ Settings");
        settingsBtn.setOnAction(e -> {
            try {
                onOpenSettings.run();
            } catch (Exception ex) {
                showError("Failed to open settings", ex.getMessage());
            }
        });
        
        Button helpBtn = createFooterButton("? Help");
        helpBtn.setOnAction(e -> {
            try {
                onOpenDocumentation.run();
            } catch (Exception ex) {
                showError("Failed to open documentation", ex.getMessage());
            }
        });
        
        Button exitBtn = createFooterButton("✕ Exit");
        exitBtn.getStyleClass().add("exit-button");
        exitBtn.setOnAction(e -> {
            try {
                onExit.run();
            } catch (Exception ex) {
                System.err.println("Error during exit: " + ex.getMessage());
            } finally {
                close();
            }
        });
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        buttonBox.getChildren().addAll(settingsBtn, helpBtn, spacer, exitBtn);
        footer.getChildren().add(buttonBox);
        
        return footer;
    }
    
    /**
     * Creates a large action button for the left column.
     */
    private Button createLargeActionButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("System", 13));
        button.setStyle(
            "-fx-padding: 12 15;" +
            "-fx-font-weight: bold;" +
            "-fx-text-alignment: left;" +
            "-fx-min-width: 180;" +
            "-fx-pref-width: 180;"
        );
        button.setMinHeight(40);
        button.setWrapText(true);
        return button;
    }
    
    /**
     * Creates a footer button.
     */
    private Button createFooterButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("System", 11));
        button.setStyle("-fx-padding: 8 16; -fx-min-width: 100;");
        button.setMinHeight(28);
        return button;
    }
    
    /**
     * Updates the recent playlists display.
     * Defensive: handles null or empty list gracefully.
     */
    public void updateRecentPlaylists(List<RecentPlaylist> recentPlaylists) {
        try {
            recentPlaylistsContainer.getChildren().clear();
            
            // Defensive: check if list is null or empty
            if (recentPlaylists == null || recentPlaylists.isEmpty()) {
                Label emptyLabel = new Label("No recent playlists");
                emptyLabel.setStyle("-fx-font-size: 12;");
                emptyLabel.getStyleClass().add("placeholder-label");
                recentPlaylistsContainer.getChildren().add(emptyLabel);
            } else {
                for (RecentPlaylist playlist : recentPlaylists) {
                    // Defensive: skip null playlist entries
                    if (playlist != null) {
                        try {
                            RecentPlaylistCard card = new RecentPlaylistCard(
                                playlist,
                                () -> {
                                    try {
                                        playlistSelected = true;
                                        onOpenPlaylist.run();
                                        close();
                                    } catch (Exception ex) {
                                        showError("Failed to open playlist", ex.getMessage());
                                        playlistSelected = false;
                                    }
                                },
                                () -> {} // Pin callback
                            );
                            recentPlaylistsContainer.getChildren().add(card);
                        } catch (Exception ex) {
                            System.err.println("Failed to create card for recent playlist: " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating recent playlists: " + e.getMessage());
        }
    }
    
    /**
     * Applies a theme to this window.
     * Defensive: handles missing theme gracefully.
     */
    public void applyTheme(String themeName) {
        try {
            if (themeName == null || themeName.isEmpty()) {
                themeName = "dark";
            }
            String themePath = "/css/" + themeName + "-theme.css";
            getScene().getStylesheets().clear();
            String resource = getClass().getResource(themePath).toExternalForm();
            if (resource != null) {
                getScene().getStylesheets().add(resource);
            }
        } catch (Exception e) {
            System.err.println("Failed to apply theme '" + themeName + "': " + e.getMessage());
            // Fall back to no stylesheet rather than crashing
        }
    }
    
    /**
     * Shows an error dialog.
     * Defensive: ensures dialog is shown even if theme fails.
     */
    private void showError(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title != null ? title : "Error");
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "An unknown error occurred");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error: " + title + " - " + message);
        }
    }
    
    /**
     * Returns whether a playlist was successfully selected.
     */
    public boolean wasPlaylistSelected() {
        return playlistSelected;
    }
    
    /**
     * Closes the welcome screen (called by successful callbacks).
     */
    public void closeWelcomeScreen() {
        close();
    }
}

