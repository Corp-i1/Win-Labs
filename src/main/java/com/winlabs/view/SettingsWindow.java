package com.winlabs.view;

import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import com.winlabs.service.LoggerService;
import com.winlabs.service.SettingsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

/**
 * Settings window for configuring application and workspace preferences.
 * Provides two main tabs: Application Settings and Workspace Settings.
 */
public class SettingsWindow extends Stage {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private final Settings settings;
    private final SettingsService settingsService;
    private Consumer<Settings> onApply;
    
    // Application settings controls
    private ComboBox<String> themeComboBox;
    private CheckBox autoSaveCheckBox;
    private Spinner<Integer> autoSaveIntervalSpinner;
    
    // Playlist defaults controls
    private Slider masterVolumeSlider;
    private Label volumeLabel;
    private TextField audioFileDirectoryField;
    private Spinner<Double> preWaitDefaultSpinner;
    private Spinner<Double> postWaitDefaultSpinner;
    private CheckBox autoFollowDefaultCheckBox;
    
    // Logging settings controls
    private CheckBox loggingEnabledCheckBox;
    private ComboBox<LogLevel> logLevelComboBox;
    private TextField logDirectoryField;
    private Spinner<Integer> logRotationSizeSpinner;
    private Spinner<Integer> logRetentionDaysSpinner;
    
    /**
     * Creates a new settings window.
     * 
     * @param settings The settings to edit
     * @param settingsService The service for saving settings
     */
    public SettingsWindow(Settings settings, SettingsService settingsService) {
        this.settings = settings;
        this.settingsService = settingsService;
        
        initializeUI();
        loadSettingsIntoControls();
    }
    
    /**
     * Sets a callback to be invoked when settings are applied.
     * 
     * @param onApply The callback function
     */
    public void setOnApply(Consumer<Settings> onApply) {
        this.onApply = onApply;
    }
    
    /**
     * Initializes the UI.
     */
    private void initializeUI() {
        setTitle("Settings");
        setWidth(600);
        setHeight(500);
        initModality(Modality.APPLICATION_MODAL);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Create tab pane with two main sections
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createApplicationSettingsTab(),
            createWorkspaceSettingsTab(),
            createLoggingSettingsTab()
        );
        
        root.setCenter(tabPane);
        
        // Bottom buttons
        HBox buttonBox = createButtonBox();
        root.setBottom(buttonBox);
        
        Scene scene = new Scene(root);
        setScene(scene);
    }
    
    /**
     * Creates the application settings tab.
     * Contains theme, auto-save, and other global settings.
     */
    private Tab createApplicationSettingsTab() {
        Tab tab = new Tab("Application Settings");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Appearance section
        Label appearanceLabel = new Label("Appearance:");
        appearanceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label themeLabel = new Label("Theme:");
        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("dark", "light", "rainbow");
        themeComboBox.setPrefWidth(200);
        
        HBox themeBox = new HBox(10, themeLabel, themeComboBox);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        
        // Auto-save section
        Label autoSaveLabel = new Label("Auto-Save:");
        autoSaveLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        autoSaveLabel.setPadding(new Insets(10, 0, 0, 0));
        
        autoSaveCheckBox = new CheckBox("Enable auto-save");
        Label autoSaveNote = new Label("Automatically saves the playlist at regular intervals.");
        autoSaveNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        Label intervalLabel = new Label("Interval (seconds):");
        autoSaveIntervalSpinner = new Spinner<>(60, 3600, 300, 60);
        autoSaveIntervalSpinner.setPrefWidth(150);
        
        HBox intervalBox = new HBox(10, intervalLabel, autoSaveIntervalSpinner);
        intervalBox.setAlignment(Pos.CENTER_LEFT);
        
        content.getChildren().addAll(
            appearanceLabel, themeBox, new Separator(),
            autoSaveLabel, autoSaveCheckBox, autoSaveNote, intervalBox
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(0));
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    /**
     * Creates the playlist defaults tab.
     * Contains default values for pre-wait, post-wait, audio, volume, and workspace-specific settings.
     */
    private Tab createWorkspaceSettingsTab() {
        Tab tab = new Tab("Playlist Defaults");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Cue defaults section
        Label cueDefaultsLabel = new Label("Cue Defaults:");
        cueDefaultsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Pre-wait default
        Label preWaitLabel = new Label("Pre-wait (seconds):");
        preWaitDefaultSpinner = new Spinner<>(0.0, 300.0, settings.getPreWaitDefault(), 0.5);
        preWaitDefaultSpinner.setPrefWidth(100);
        HBox preWaitBox = new HBox(10, preWaitLabel, preWaitDefaultSpinner);
        preWaitBox.setAlignment(Pos.CENTER_LEFT);
        
        // Post-wait default
        Label postWaitLabel = new Label("Post-wait (seconds):");
        postWaitDefaultSpinner = new Spinner<>(0.0, 300.0, settings.getPostWaitDefault(), 0.5);
        postWaitDefaultSpinner.setPrefWidth(100);
        HBox postWaitBox = new HBox(10, postWaitLabel, postWaitDefaultSpinner);
        postWaitBox.setAlignment(Pos.CENTER_LEFT);
        
        // Auto-follow default
        Label autoFollowLabel = new Label("Auto-follow default:");
        autoFollowDefaultCheckBox = new CheckBox();
        autoFollowDefaultCheckBox.setSelected(settings.isAutoFollowDefault());
        HBox autoFollowBox = new HBox(10, autoFollowLabel, autoFollowDefaultCheckBox);
        autoFollowBox.setAlignment(Pos.CENTER_LEFT);
        
        // Audio section
        Label audioLabel = new Label("Audio:");
        audioLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        audioLabel.setPadding(new Insets(10, 0, 0, 0));
        
        // Master volume
        Label volumeHeading = new Label("Master Volume:");
        masterVolumeSlider = new Slider(0, 1, 1);
        masterVolumeSlider.setShowTickLabels(true);
        masterVolumeSlider.setShowTickMarks(true);
        masterVolumeSlider.setMajorTickUnit(0.25);
        masterVolumeSlider.setBlockIncrement(0.1);
        
        volumeLabel = new Label("100%");
        volumeLabel.setMinWidth(50);
        
        masterVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            volumeLabel.setText(String.format("%d%%", (int)(newVal.doubleValue() * 100)));
        });
        
        HBox volumeBox = new HBox(10, masterVolumeSlider, volumeLabel);
        volumeBox.setAlignment(Pos.CENTER_LEFT);
        
        // File handling section
        Label fileLabel = new Label("File Handling:");
        fileLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        fileLabel.setPadding(new Insets(10, 0, 0, 0));
        
        Label dirLabel = new Label("Audio file directory:");
        audioFileDirectoryField = new TextField();
        audioFileDirectoryField.setPrefWidth(400);
        audioFileDirectoryField.setEditable(true);
        
        HBox dirBox = new HBox(10, dirLabel, audioFileDirectoryField);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dirNote = new Label("Default directory for browsing audio files.");
        dirNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        content.getChildren().addAll(
            cueDefaultsLabel, preWaitBox, postWaitBox, autoFollowBox,
            new Separator(),
            audioLabel, volumeHeading, volumeBox,
            new Separator(),
            fileLabel, dirBox, dirNote
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(0));
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    /**
     * Creates the logging settings tab.
     * Contains logging enable/disable, log level, directory, rotation, and retention settings.
     */
    private Tab createLoggingSettingsTab() {
        Tab tab = new Tab("Logging");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Logging enable/disable
        Label loggingLabel = new Label("Logging:");
        loggingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        loggingEnabledCheckBox = new CheckBox("Enable application logging");
        Label loggingNote = new Label("Logs important events and errors for troubleshooting.");
        loggingNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        // Log level
        Label logLevelLabel = new Label("Log Level:");
        logLevelComboBox = new ComboBox<>();
        logLevelComboBox.getItems().addAll(LogLevel.values());
        logLevelComboBox.setPrefWidth(150);
        HBox logLevelBox = new HBox(10, logLevelLabel, logLevelComboBox);
        logLevelBox.setAlignment(Pos.CENTER_LEFT);
        
        Label logLevelNote = new Label("Controls the verbosity of logging (ERROR = least, TRACE = most).");
        logLevelNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        // Log directory
        Label logDirLabel = new Label("Log Directory:");
        logDirectoryField = new TextField();
        logDirectoryField.setPrefWidth(300);
        Button browseDirButton = new Button("Browse...");
        browseDirButton.setOnAction(e -> browseLogDirectory());
        
        HBox logDirBox = new HBox(10, logDirectoryField, browseDirButton);
        logDirBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(logDirectoryField, Priority.ALWAYS);
        
        VBox logDirSection = new VBox(5, logDirLabel, logDirBox);
        
        // Log rotation
        Label rotationLabel = new Label("Log Rotation:");
        rotationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        rotationLabel.setPadding(new Insets(10, 0, 0, 0));
        
        Label rotationSizeLabel = new Label("Rotation Size (MB):");
        logRotationSizeSpinner = new Spinner<>(1, 100, 10, 1);
        logRotationSizeSpinner.setPrefWidth(100);
        HBox rotationSizeBox = new HBox(10, rotationSizeLabel, logRotationSizeSpinner);
        rotationSizeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label rotationNote = new Label("Maximum size of each log file before rotation.");
        rotationNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        // Log retention
        Label retentionLabel = new Label("Retention Days:");
        logRetentionDaysSpinner = new Spinner<>(1, 365, 5, 1);
        logRetentionDaysSpinner.setPrefWidth(100);
        HBox retentionBox = new HBox(10, retentionLabel, logRetentionDaysSpinner);
        retentionBox.setAlignment(Pos.CENTER_LEFT);
        
        Label retentionNote = new Label("Number of days to keep old log files.");
        retentionNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        // View logs button
        Button viewLogsButton = new Button("View Logs");
        viewLogsButton.setOnAction(e -> openLogViewer());
        
        Button openFolderButton = new Button("Open Log Folder");
        openFolderButton.setOnAction(e -> openLogFolder());
        
        HBox logsButtonBox = new HBox(10, viewLogsButton, openFolderButton);
        logsButtonBox.setPadding(new Insets(10, 0, 0, 0));
        
        content.getChildren().addAll(
            loggingLabel, loggingEnabledCheckBox, loggingNote,
            new Separator(),
            logLevelBox, logLevelNote,
            new Separator(),
            logDirSection,
            new Separator(),
            rotationLabel, rotationSizeBox, rotationNote,
            retentionBox, retentionNote,
            new Separator(),
            logsButtonBox
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(0));
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    /**
     * Creates the button box at the bottom of the window.
     */
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button okButton = new Button("OK");
        okButton.setPrefWidth(80);
        okButton.setOnAction(e -> {
            applySettings();
            close();
        });
        
        Button applyButton = new Button("Apply");
        applyButton.setPrefWidth(80);
        applyButton.setOnAction(e -> applySettings());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> close());
        
        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(e -> resetToDefaults());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        buttonBox.getChildren().addAll(resetButton, spacer, okButton, applyButton, cancelButton);
        return buttonBox;
    }
    
    /**
     * Loads current settings into the UI controls.
     */
    private void loadSettingsIntoControls() {
        // Application settings
        themeComboBox.setValue(settings.getTheme());
        autoSaveCheckBox.setSelected(settings.isAutoSaveEnabled());
        autoSaveIntervalSpinner.getValueFactory().setValue(settings.getAutoSaveInterval());
        
        // Playlist defaults
        preWaitDefaultSpinner.getValueFactory().setValue(settings.getPreWaitDefault());
        postWaitDefaultSpinner.getValueFactory().setValue(settings.getPostWaitDefault());
        autoFollowDefaultCheckBox.setSelected(settings.isAutoFollowDefault());
        masterVolumeSlider.setValue(settings.getMasterVolume());
        volumeLabel.setText(String.format("%d%%", (int)(settings.getMasterVolume() * 100)));
        audioFileDirectoryField.setText(settings.getAudioFileDirectory());
        
        // Logging settings
        loggingEnabledCheckBox.setSelected(settings.isLoggingEnabled());
        logLevelComboBox.setValue(settings.getLogLevel());
        logDirectoryField.setText(settings.getLogDirectory());
        logRotationSizeSpinner.getValueFactory().setValue(settings.getLogRotationSizeMB());
        logRetentionDaysSpinner.getValueFactory().setValue(settings.getLogRetentionDays());
    }
    
    /**
     * Applies the current settings from the UI controls.
     */
    private void applySettings() {
        // Update settings model
        settings.setTheme(themeComboBox.getValue());
        settings.setMasterVolume(masterVolumeSlider.getValue());
        settings.setAutoSaveEnabled(autoSaveCheckBox.isSelected());
        settings.setAutoSaveInterval(autoSaveIntervalSpinner.getValue());
        settings.setAudioFileDirectory(audioFileDirectoryField.getText());
        settings.setPreWaitDefault(preWaitDefaultSpinner.getValue());
        settings.setPostWaitDefault(postWaitDefaultSpinner.getValue());
        settings.setAutoFollowDefault(autoFollowDefaultCheckBox.isSelected());
        
        // Update logging settings
        settings.setLoggingEnabled(loggingEnabledCheckBox.isSelected());
        settings.setLogLevel(logLevelComboBox.getValue());
        settings.setLogDirectory(logDirectoryField.getText());
        settings.setLogRotationSizeMB(logRotationSizeSpinner.getValue());
        settings.setLogRetentionDays(logRetentionDaysSpinner.getValue());
        
        // Save to file
        try {
            settingsService.save(settings);
            // Reconfigure logging with new settings
            LoggerService.configureLogging(settings);
        } catch (Exception e) {
            showError("Failed to save settings", e.getMessage());
        }
        
        // Notify callback
        if (onApply != null) {
            onApply.accept(settings);
        }
    }
    
    /**
     * Resets all settings to default values.
     */
    private void resetToDefaults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset all settings to defaults?");
        alert.setContentText("This will discard all current settings.");
        applyThemeToDialog(alert);
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            settings.resetToDefaults();
            loadSettingsIntoControls();
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
        applyThemeToDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Opens a directory chooser for selecting the log directory.
     */
    private void browseLogDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Log Directory");
        
        // Set initial directory if valid
        try {
            File currentDir = new File(logDirectoryField.getText());
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
        } catch (Exception e) {
            // Ignore, use default
        }
        
        File selected = chooser.showDialog(this);
        if (selected != null) {
            logDirectoryField.setText(selected.getAbsolutePath());
        }
    }
    
    /**
     * Opens the log viewer window.
     */
    private void openLogViewer() {
        try {
            LogViewer logViewer = new LogViewer(settings);
            logViewer.show();
        } catch (Exception e) {
            showError("Error", "Failed to open log viewer: " + e.getMessage());
        }
    }
    
    /**
     * Opens the log folder in the system file explorer.
     */
    private void openLogFolder() {
        try {
            LoggerService.openLogDirectory(settings);
        } catch (Exception e) {
            showError("Error", "Failed to open log folder: " + e.getMessage());
        }
    }
    
    /**
     * Applies the current theme to a dialog.
     */
    private void applyThemeToDialog(Dialog<?> dialog) {
        if (dialog.getDialogPane() != null && getScene() != null) {
            // Copy stylesheets from the settings window to the dialog
            dialog.getDialogPane().getStylesheets().clear();
            dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
    }
}
