package com.winlabs.view;

import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import com.winlabs.service.LoggerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Log viewer window for displaying and filtering application logs.
 * Provides real-time log display with filtering by level.
 */
public class LogViewer extends Stage {
    
    private static final Logger logger = LoggerFactory.getLogger(LogViewer.class);
    
    private final Settings settings;
    private TextArea logTextArea;
    private ComboBox<String> filterComboBox;
    private ComboBox<String> fileComboBox;
    private Label statusLabel;
    
    public LogViewer(Settings settings) {
        this.settings = settings;
        initializeUI();
        loadLogFiles();
    }
    
    private void initializeUI() {
        setTitle("Win-Labs - Log Viewer");
        setWidth(900);
        setHeight(600);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top: Controls
        root.setTop(createControls());
        
        // Center: Log display
        root.setCenter(createLogDisplay());
        
        // Bottom: Status bar
        root.setBottom(createStatusBar());
        
        Scene scene = new Scene(root);
        setScene(scene);
        
        // Apply theme if settings available
        if (settings != null && settings.getTheme() != null) {
            try {
                String themeFile = "/css/" + settings.getTheme() + "-theme.css";
                scene.getStylesheets().add(getClass().getResource(themeFile).toExternalForm());
            } catch (Exception e) {
                logger.warn("Failed to apply theme to log viewer", e);
            }
        }
    }
    
    private HBox createControls() {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(0, 0, 10, 0));
        controls.setAlignment(Pos.CENTER_LEFT);
        
        // File selector
        Label fileLabel = new Label("Log File:");
        fileComboBox = new ComboBox<>();
        fileComboBox.setPrefWidth(300);
        fileComboBox.setOnAction(e -> loadSelectedLogFile());
        
        // Filter by level
        Label filterLabel = new Label("Filter:");
        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("All", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> applyFilter());
        
        // Refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshLogs());
        
        // Open folder button
        Button openFolderButton = new Button("Open Log Folder");
        openFolderButton.setOnAction(e -> openLogFolder());
        
        // Clear logs button
        Button clearButton = new Button("Clear Display");
        clearButton.setOnAction(e -> logTextArea.clear());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        controls.getChildren().addAll(
            fileLabel, fileComboBox,
            filterLabel, filterComboBox,
            refreshButton, openFolderButton,
            spacer, clearButton
        );
        
        return controls;
    }
    
    private VBox createLogDisplay() {
        VBox container = new VBox();
        VBox.setVgrow(container, Priority.ALWAYS);
        
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(false);
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(logTextArea, Priority.ALWAYS);
        
        container.getChildren().add(logTextArea);
        return container;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10, 0, 0, 0));
        
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: gray;");
        
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }
    
    /**
     * Loads the list of log files into the file selector.
     */
    private void loadLogFiles() {
        try {
            List<File> logFiles = LoggerService.getLogFiles(settings);
            fileComboBox.getItems().clear();
            
            if (logFiles.isEmpty()) {
                statusLabel.setText("No log files found");
                return;
            }
            
            for (File file : logFiles) {
                fileComboBox.getItems().add(file.getName());
            }
            
            // Select the most recent log file (first in list)
            if (!fileComboBox.getItems().isEmpty()) {
                fileComboBox.setValue(fileComboBox.getItems().get(0));
                loadSelectedLogFile();
            }
            
            statusLabel.setText("Found " + logFiles.size() + " log file(s)");
            
        } catch (Exception e) {
            logger.error("Failed to load log files", e);
            statusLabel.setText("Error loading log files");
            showError("Error", "Failed to load log files: " + e.getMessage());
        }
    }
    
    /**
     * Loads the selected log file into the display.
     */
    private void loadSelectedLogFile() {
        String selectedFile = fileComboBox.getValue();
        if (selectedFile == null || selectedFile.isEmpty()) {
            return;
        }
        
        try {
            Path logPath = Paths.get(settings.getLogDirectory(), selectedFile);
            if (!Files.exists(logPath)) {
                statusLabel.setText("Log file not found: " + selectedFile);
                return;
            }
            
            // Read log file
            List<String> lines = Files.readAllLines(logPath);
            logTextArea.setText(String.join("\n", lines));
            
            // Scroll to bottom
            logTextArea.setScrollTop(Double.MAX_VALUE);
            
            statusLabel.setText("Loaded " + lines.size() + " lines from " + selectedFile);
            
        } catch (IOException e) {
            logger.error("Failed to load log file: {}", selectedFile, e);
            statusLabel.setText("Error loading log file");
            showError("Error", "Failed to load log file: " + e.getMessage());
        }
    }
    
    /**
     * Applies the selected filter to the log display.
     */
    private void applyFilter() {
        String selectedFile = fileComboBox.getValue();
        if (selectedFile == null || selectedFile.isEmpty()) {
            return;
        }
        
        String filterLevel = filterComboBox.getValue();
        if ("All".equals(filterLevel)) {
            loadSelectedLogFile(); // Reload without filter
            return;
        }
        
        try {
            Path logPath = Paths.get(settings.getLogDirectory(), selectedFile);
            if (!Files.exists(logPath)) {
                return;
            }
            
            // Read and filter log file
            List<String> lines = Files.readAllLines(logPath);
            List<String> filteredLines = lines.stream()
                .filter(line -> line.contains(filterLevel))
                .collect(Collectors.toList());
            
            logTextArea.setText(String.join("\n", filteredLines));
            
            // Scroll to bottom
            logTextArea.setScrollTop(Double.MAX_VALUE);
            
            statusLabel.setText("Showing " + filteredLines.size() + " " + filterLevel + " lines from " + selectedFile);
            
        } catch (IOException e) {
            logger.error("Failed to filter log file: {}", selectedFile, e);
            statusLabel.setText("Error filtering log file");
        }
    }
    
    /**
     * Refreshes the log display.
     */
    private void refreshLogs() {
        loadLogFiles();
        statusLabel.setText("Logs refreshed");
    }
    
    /**
     * Opens the log folder in the system file explorer.
     */
    private void openLogFolder() {
        try {
            LoggerService.openLogDirectory(settings);
            statusLabel.setText("Opened log folder");
        } catch (Exception e) {
            logger.error("Failed to open log folder", e);
            showError("Error", "Failed to open log folder: " + e.getMessage());
        }
    }
    
    /**
     * Shows an error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Apply theme
        if (settings != null && settings.getTheme() != null) {
            try {
                String themeFile = "/css/" + settings.getTheme() + "-theme.css";
                alert.getDialogPane().getStylesheets().add(
                    getClass().getResource(themeFile).toExternalForm()
                );
            } catch (Exception e) {
                logger.warn("Failed to apply theme to error dialog", e);
            }
        }
        
        alert.showAndWait();
    }
}
