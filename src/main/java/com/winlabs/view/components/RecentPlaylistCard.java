package com.winlabs.view.components;

import com.winlabs.model.RecentPlaylist;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * UI card component for displaying a recent playlist in the WelcomeScreen.
 * Shows playlist name, path, last opened date, and pin button.
 */
public class RecentPlaylistCard extends VBox {
    
    private final RecentPlaylist playlist;
    private final Runnable onOpenCallback;
    private final Runnable onPinCallback;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
    
    public RecentPlaylistCard(RecentPlaylist playlist, Runnable onOpen, Runnable onPin) {
        this.playlist = playlist;
        this.onOpenCallback = onOpen;
        this.onPinCallback = onPin;
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Card styling
        setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4;");
        setSpacing(8);
        setPadding(new Insets(12));
        setMinHeight(140);
        setPrefWidth(180);
        setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4; " +
                "-fx-background-color: #f5f5f5; -fx-cursor: hand;");
        
        // Title section
        Label titleLabel = new Label(playlist.getPlaylistName());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setWrapText(true);
        
        // Path label
        Label pathLabel = new Label(extractFileName(playlist.getPlaylistPath()));
        pathLabel.setFont(Font.font("System", 10));
        pathLabel.setStyle("-fx-text-fill: #666666;");
        pathLabel.setWrapText(true);
        
        // Date label
        String dateStr = DATE_FORMATTER.format(Instant.ofEpochMilli(playlist.getLastOpened()));
        Label dateLabel = new Label("Opened: " + dateStr);
        dateLabel.setFont(Font.font("System", 9));
        dateLabel.setStyle("-fx-text-fill: #999999;");
        
        // Top section (title and path)
        VBox topSection = new VBox(4);
        topSection.getChildren().addAll(titleLabel, pathLabel);
        
        // Buttons section
        Button openButton = new Button("Open");
        openButton.setStyle("-fx-padding: 6 16;");
        openButton.setOnAction(e -> onOpenCallback.run());
        
        Button pinButton = new Button(playlist.isPinned() ? "★ Pinned" : "☆ Pin");
        pinButton.setStyle("-fx-padding: 6 16;");
        pinButton.setStyle("-fx-padding: 6 10; -fx-font-size: 12;");
        pinButton.setOnAction(e -> {
            playlist.setIsPinned(!playlist.isPinned());
            pinButton.setText(playlist.isPinned() ? "★ Pinned" : "☆ Pin");
            onPinCallback.run();
        });
        
        HBox buttonBox = new HBox(6);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(openButton, pinButton);
        HBox.setHgrow(openButton, Priority.ALWAYS);
        HBox.setHgrow(pinButton, Priority.ALWAYS);
        openButton.setMaxWidth(Double.MAX_VALUE);
        pinButton.setMaxWidth(Double.MAX_VALUE);
        
        // Add all sections
        getChildren().addAll(topSection, dateLabel);
        VBox.setVgrow(topSection, Priority.ALWAYS);
        getChildren().add(buttonBox);
        
        // Hover effect
        setOnMouseEntered(e -> setStyle("-fx-border-color: #0078d4; -fx-border-width: 2; -fx-border-radius: 4; " +
                "-fx-background-color: #ffffff; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 4; " +
                "-fx-background-color: #f5f5f5; -fx-cursor: hand;"));
    }
    
    /**
     * Extracts just the filename from a full path.
     */
    private String extractFileName(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
