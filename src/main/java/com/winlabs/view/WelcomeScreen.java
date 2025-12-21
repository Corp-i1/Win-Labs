package com.winlabs.view;

import com.winlabs.model.RecentPlaylist;
import com.winlabs.view.components.RecentPlaylistCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.List;

/**
 * Welcome screen displayed at application startup as a separate window.
 * Shows recent playlists and application controls (new, open, settings, help, exit).
 */
public class WelcomeScreen extends Stage {
    
    private final Runnable onNewPlaylist;
    private final Runnable onOpenPlaylist;
    private final Runnable onOpenSettings;
    private final Runnable onOpenDocumentation;
    private final Runnable onExit;
    
    private VBox recentPlaylistsContainer;
    
    public WelcomeScreen(Runnable onNewPlaylist, Runnable onOpenPlaylist, 
                        Runnable onOpenSettings, Runnable onOpenDocumentation, Runnable onExit) {
        this.onNewPlaylist = onNewPlaylist;
        this.onOpenPlaylist = onOpenPlaylist;
        this.onOpenSettings = onOpenSettings;
        this.onOpenDocumentation = onOpenDocumentation;
        this.onExit = onExit;
        
        setTitle("Win-Labs");
        setWidth(400);
        setHeight(400);
        setResizable(false);
        
        initializeUI();
    }
    
    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ffffff;");
        
        // Top: Title section
        VBox headerSection = createHeaderSection();
        root.setTop(headerSection);
        
        // Center: Action buttons
        VBox centerSection = createCenterSection();
        root.setCenter(centerSection);
        
        // Bottom: Exit button
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
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Win-Labs");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        
        Label subtitleLabel = new Label("Cue List Manager");
        subtitleLabel.setFont(Font.font("System", 12));
        subtitleLabel.setStyle("-fx-text-fill: #666666;");
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
    
    /**
     * Creates the center section with action buttons and recent playlists.
     */
    private VBox createCenterSection() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(20));
        
        // Action buttons
        Button newPlaylistBtn = createActionButton("New Playlist");
        newPlaylistBtn.setOnAction(e -> {
            onNewPlaylist.run();
            close();
        });
        
        Button openPlaylistBtn = createActionButton("Open Playlist");
        openPlaylistBtn.setOnAction(e -> {
            onOpenPlaylist.run();
            close();
        });
        
        // Recent playlists label
        Label recentLabel = new Label("Recent Playlists");
        recentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        recentLabel.setStyle("-fx-text-fill: #666666;");
        
        // Recent playlists container
        recentPlaylistsContainer = new VBox(8);
        recentPlaylistsContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4;");
        recentPlaylistsContainer.setPadding(new Insets(10));
        
        Label placeholderLabel = new Label("No recent playlists");
        placeholderLabel.setStyle("-fx-text-fill: #999999;");
        recentPlaylistsContainer.getChildren().add(placeholderLabel);
        
        center.getChildren().addAll(
            newPlaylistBtn,
            openPlaylistBtn,
            new Separator(),
            recentLabel,
            recentPlaylistsContainer
        );
        
        VBox.setVgrow(recentPlaylistsContainer, Priority.ALWAYS);
        return center;
    }
    
    /**
     * Creates the footer section with settings, help, and exit buttons.
     */
    private VBox createFooterSection() {
        VBox footer = new VBox(8);
        footer.setPadding(new Insets(12));
        footer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        
        Button settingsBtn = createSmallButton("⚙ Settings");
        settingsBtn.setOnAction(e -> onOpenSettings.run());
        
        Button helpBtn = createSmallButton("? Help");
        helpBtn.setOnAction(e -> onOpenDocumentation.run());
        
        Button exitBtn = createSmallButton("Exit");
        exitBtn.setStyle(exitBtn.getStyle() + "; -fx-text-fill: #cc0000;");
        exitBtn.setOnAction(e -> {
            onExit.run();
            close();
        });
        
        footer.getChildren().addAll(settingsBtn, helpBtn, exitBtn);
        return footer;
    }
    
    /**
     * Creates a large action button.
     */
    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("System", 13));
        button.setStyle("-fx-padding: 10 20; -fx-min-width: 200;");
        button.setMinHeight(35);
        return button;
    }
    
    /**
     * Creates a small footer button.
     */
    private Button createSmallButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("System", 11));
        button.setStyle("-fx-padding: 8 16; -fx-min-width: 100;");
        button.setMinHeight(28);
        return button;
    }
    
    /**
     * Updates the recent playlists display.
     */
    public void updateRecentPlaylists(List<RecentPlaylist> recentPlaylists) {
        recentPlaylistsContainer.getChildren().clear();
        
        if (recentPlaylists.isEmpty()) {
            Label emptyLabel = new Label("No recent playlists");
            emptyLabel.setStyle("-fx-text-fill: #999999;");
            recentPlaylistsContainer.getChildren().add(emptyLabel);
        } else {
            for (RecentPlaylist playlist : recentPlaylists) {
                RecentPlaylistCard card = new RecentPlaylistCard(
                    playlist,
                    () -> {
                        onOpenPlaylist.run();
                        close();
                    },
                    () -> {} // Pin callback
                );
                recentPlaylistsContainer.getChildren().add(card);
            }
        }
    }
    
    /**
     * Applies a theme to this window.
     */
    public void applyTheme(String themeName) {
        String themePath = "/css/" + themeName + "-theme.css";
        try {
            getScene().getStylesheets().clear();
            getScene().getStylesheets().add(getClass().getResource(themePath).toExternalForm());
        } catch (Exception e) {
            System.err.println("Failed to apply theme: " + e.getMessage());
        }
    }
}
