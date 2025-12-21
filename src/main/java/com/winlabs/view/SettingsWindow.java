package com.winlabs.view;

import com.winlabs.model.Settings;
import com.winlabs.service.SettingsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Settings window for configuring application and workspace preferences.
 * Provides two main tabs: Application Settings and Workspace Settings.
 */
public class SettingsWindow extends Stage {
    
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
            createWorkspaceSettingsTab()
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
        
        // Save to file
        try {
            settingsService.save(settings);
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
