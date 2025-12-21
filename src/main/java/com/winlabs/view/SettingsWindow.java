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
 * Settings window for configuring application preferences.
 * Provides tabs for appearance, audio, and general settings.
 */
public class SettingsWindow extends Stage {
    
    private final Settings settings;
    private final SettingsService settingsService;
    private Consumer<Settings> onApply;
    
    // Appearance controls
    private ComboBox<String> themeComboBox;
    
    // Audio controls
    private Slider masterVolumeSlider;
    private Label volumeLabel;
    private CheckBox multiTrackCheckBox;
    
    // General controls
    private CheckBox autoSaveCheckBox;
    private Spinner<Integer> autoSaveIntervalSpinner;
    
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
        
        // Create tab pane
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createAppearanceTab(),
            createAudioTab(),
            createGeneralTab()
        );
        
        root.setCenter(tabPane);
        
        // Bottom buttons
        HBox buttonBox = createButtonBox();
        root.setBottom(buttonBox);
        
        Scene scene = new Scene(root);
        setScene(scene);
    }
    
    /**
     * Creates the appearance settings tab.
     */
    private Tab createAppearanceTab() {
        Tab tab = new Tab("Appearance");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Theme selection
        Label themeLabel = new Label("Theme:");
        themeLabel.setStyle("-fx-font-weight: bold;");
        
        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("dark", "light", "rainbow");
        themeComboBox.setPrefWidth(200);
        
        HBox themeBox = new HBox(10, new Label("Select theme:"), themeComboBox);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label themeNote = new Label("Changes will be applied when you click OK or Apply.");
        themeNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        content.getChildren().addAll(themeLabel, themeBox, themeNote);
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * Creates the audio settings tab.
     */
    private Tab createAudioTab() {
        Tab tab = new Tab("Audio");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Master volume
        Label volumeHeading = new Label("Master Volume:");
        volumeHeading.setStyle("-fx-font-weight: bold;");
        
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
        
        // Multi-track playback
        Label multiTrackHeading = new Label("Playback Options:");
        multiTrackHeading.setStyle("-fx-font-weight: bold;");
        
        multiTrackCheckBox = new CheckBox("Enable multi-track playback");
        Label multiTrackNote = new Label("When enabled, allows playing multiple cues simultaneously (up to 20 tracks).");
        multiTrackNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        multiTrackNote.setWrapText(true);
        
        content.getChildren().addAll(
            volumeHeading, volumeBox,
            new Separator(),
            multiTrackHeading, multiTrackCheckBox, multiTrackNote
        );
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * Creates the general settings tab.
     */
    private Tab createGeneralTab() {
        Tab tab = new Tab("General");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Auto-save
        Label autoSaveHeading = new Label("Auto-Save:");
        autoSaveHeading.setStyle("-fx-font-weight: bold;");
        
        autoSaveCheckBox = new CheckBox("Enable automatic playlist saving");
        
        HBox intervalBox = new HBox(10);
        intervalBox.setAlignment(Pos.CENTER_LEFT);
        Label intervalLabel = new Label("Save interval (seconds):");
        
        autoSaveIntervalSpinner = new Spinner<>(60, 3600, 300, 30);
        autoSaveIntervalSpinner.setPrefWidth(100);
        autoSaveIntervalSpinner.setEditable(true);
        autoSaveIntervalSpinner.disableProperty().bind(autoSaveCheckBox.selectedProperty().not());
        
        intervalBox.getChildren().addAll(intervalLabel, autoSaveIntervalSpinner);
        
        Label autoSaveNote = new Label("Automatically saves the current playlist at regular intervals.");
        autoSaveNote.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        autoSaveNote.setWrapText(true);
        
        content.getChildren().addAll(
            autoSaveHeading, autoSaveCheckBox, intervalBox, autoSaveNote
        );
        
        tab.setContent(content);
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
        // Appearance
        themeComboBox.setValue(settings.getTheme());
        
        // Audio
        masterVolumeSlider.setValue(settings.getMasterVolume());
        volumeLabel.setText(String.format("%d%%", (int)(settings.getMasterVolume() * 100)));
        multiTrackCheckBox.setSelected(settings.isEnableMultiTrackPlayback());
        
        // General
        autoSaveCheckBox.setSelected(settings.isAutoSaveEnabled());
        autoSaveIntervalSpinner.getValueFactory().setValue(settings.getAutoSaveInterval());
    }
    
    /**
     * Applies the current settings from the UI controls.
     */
    private void applySettings() {
        // Update settings model
        settings.setTheme(themeComboBox.getValue());
        settings.setMasterVolume(masterVolumeSlider.getValue());
        settings.setEnableMultiTrackPlayback(multiTrackCheckBox.isSelected());
        settings.setAutoSaveEnabled(autoSaveCheckBox.isSelected());
        settings.setAutoSaveInterval(autoSaveIntervalSpinner.getValue());
        
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
