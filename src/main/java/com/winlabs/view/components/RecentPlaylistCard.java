package com.winlabs.view.components;

import com.winlabs.model.RecentPlaylist;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * UI card component for displaying a recent playlist in the WelcomeScreen.
 * Shows playlist name, path, last opened date, and context menu for pinning.
 */
public class RecentPlaylistCard extends VBox {
    
    private final RecentPlaylist playlist;
    private final Runnable onOpenCallback;
    private final Consumer<String> onTogglePinCallback;
    private Label pinIndicator;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
    
    public RecentPlaylistCard(RecentPlaylist playlist, Runnable onOpen, Consumer<String> onTogglePin) {
        this.playlist = playlist;
        this.onOpenCallback = onOpen;
        this.onTogglePinCallback = onTogglePin;
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Card styling - use CSS classes for theme compatibility
        getStyleClass().add("recent-playlist-card");
        setSpacing(6);
        setPadding(new Insets(8));
        setMinHeight(100);
        setMaxHeight(100);
        setPrefWidth(140);
        setMaxWidth(140);
        
        // Pin indicator (top-right corner)
        pinIndicator = new Label("📌");
        pinIndicator.setFont(Font.font("System", 10));
        pinIndicator.setVisible(playlist.isPinned());
        pinIndicator.setStyle("-fx-padding: 0 0 0 0;");
        
        // Title section with pin indicator
        HBox titleRow = new HBox(5);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(playlist.getPlaylistName());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(35);
        titleLabel.getStyleClass().add("recent-playlist-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        titleRow.getChildren().addAll(titleLabel, pinIndicator);
        
        // Path label
        Label pathLabel = new Label(extractFileName(playlist.getPlaylistPath()));
        pathLabel.setFont(Font.font("System", 8));
        pathLabel.setWrapText(true);
        pathLabel.setMaxHeight(20);
        pathLabel.getStyleClass().add("recent-playlist-path");
        
        // Date label
        String dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(playlist.getLastOpened()));
        Label dateLabel = new Label("Opened: " + dateStr);
        dateLabel.setFont(Font.font("System", 7));
        dateLabel.getStyleClass().add("recent-playlist-date");
        
        // Top section (title row and path)
        VBox topSection = new VBox(2);
        topSection.getChildren().addAll(titleRow, pathLabel);
        
        // Buttons section - only Open button to save space
        Button openButton = new Button("Open");
        openButton.setFont(Font.font("System", 10));
        openButton.setStyle("-fx-padding: 4 8;");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setOnAction(e -> onOpenCallback.run());
        
        // Add all sections
        getChildren().addAll(topSection, dateLabel, openButton);
        VBox.setVgrow(topSection, Priority.ALWAYS);
        
        // Context menu for pin/unpin
        ContextMenu contextMenu = new ContextMenu();
        MenuItem pinMenuItem = new MenuItem(playlist.isPinned() ? "Unpin" : "Pin");
        pinMenuItem.setOnAction(e -> {
            if (onTogglePinCallback != null) {
                try {
                    onTogglePinCallback.accept(playlist.getPlaylistPath());
                    // Update UI after toggling
                    boolean newPinnedState = !playlist.isPinned();
                    playlist.setIsPinned(newPinnedState);
                    pinIndicator.setVisible(newPinnedState);
                    pinMenuItem.setText(newPinnedState ? "Unpin" : "Pin");
                } catch (Exception ex) {
                    System.err.println("Failed to toggle pin: " + ex.getMessage());
                }
            }
        });
        contextMenu.getItems().add(pinMenuItem);
        setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
        
        // Hover effect - use pseudo-class for theme compatibility
        setOnMouseEntered(e -> getStyleClass().add("recent-playlist-card-hover"));
        setOnMouseExited(e -> getStyleClass().remove("recent-playlist-card-hover"));
    }
    
    /**
     * Extracts just the filename from a full path.
     */
    private String extractFileName(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
