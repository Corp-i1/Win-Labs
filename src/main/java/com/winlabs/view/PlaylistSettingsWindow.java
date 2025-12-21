package com.winlabs.view;

import com.winlabs.model.PlaylistSettings;
import com.winlabs.service.PlaylistSettingsService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Settings dialog for playlist-specific configuration.
 * Allows users to set volume defaults, audio directory, and cue timing defaults.
 * Accessed from MainWindow when a playlist is open.
 */
public class PlaylistSettingsWindow extends Dialog<Boolean> {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaylistSettingsWindow.class);
    
    private final PlaylistSettings playlistSettings;
    private final PlaylistSettingsService playlistSettingsService;
    private final Path playlistPath;
    
    // UI Controls
    private Slider volumeSlider;
    private Label volumeLabel;
    private TextField audioDirectoryField;
    private Spinner<Double> preWaitSpinner;
    private Spinner<Double> postWaitSpinner;
    private CheckBox autoFollowCheckBox;
    
    public PlaylistSettingsWindow(Window owner, PlaylistSettings playlistSettings, 
                                  PlaylistSettingsService playlistSettingsService, 
                                  Path playlistPath) {
        this.playlistSettings = playlistSettings;
        this.playlistSettingsService = playlistSettingsService;
        this.playlistPath = playlistPath;
        
        logger.info("Opening playlist settings for: {}", playlistPath.getFileName());
        setTitle("Playlist Settings");
        initOwner(owner);
        setResizable(true);
        
        initializeUI();
        loadSettingsIntoControls();
    }
    
    private void initializeUI() {
        // Create tabs for different settings categories
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Audio Settings Tab
        Tab audioTab = new Tab("Audio Settings", createAudioSettingsTab());
        audioTab.setClosable(false);
        
        // Cue Defaults Tab
        Tab cueTab = new Tab("Cue Defaults", createCueDefaultsTab());
        cueTab.setClosable(false);
        
        tabPane.getTabs().addAll(audioTab, cueTab);
        
        // Buttons
        ButtonType applyType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(applyType, cancelType);
        
        // Set result converter
        setResultConverter(buttonType -> {
            if (buttonType == applyType) {
                applySettings();
                return true;
            }
            return false;
        });
        
        getDialogPane().setContent(tabPane);
        getDialogPane().setPrefSize(500, 400);
    }
    
    /**
     * Creates the Audio Settings tab content.
     */
    private VBox createAudioSettingsTab() {
        VBox vbox = new VBox(16);
        vbox.setPadding(new Insets(20));
        
        // Master Volume
        Label volumeTitle = new Label("Master Volume");
        volumeTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        HBox volumeBox = new HBox(10);
        volumeSlider = new Slider(0.0, 1.0, playlistSettings.getMasterVolume());
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(0.1);
        volumeLabel = new Label(String.format("%.0f%%", playlistSettings.getMasterVolume() * 100));
        volumeLabel.setPrefWidth(50);
        
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            volumeLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
        });
        
        HBox.setHgrow(volumeSlider, javafx.scene.layout.Priority.ALWAYS);
        volumeBox.getChildren().addAll(volumeSlider, volumeLabel);
        
        // Audio Directory
        Label dirTitle = new Label("Audio File Directory");
        dirTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        HBox dirBox = new HBox(8);
        audioDirectoryField = new TextField();
        audioDirectoryField.setText(playlistSettings.getAudioFileDirectory());
        audioDirectoryField.setEditable(false);
        
        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> browseDirectory());
        
        HBox.setHgrow(audioDirectoryField, javafx.scene.layout.Priority.ALWAYS);
        dirBox.getChildren().addAll(audioDirectoryField, browseBtn);
        
        vbox.getChildren().addAll(
            volumeTitle,
            volumeBox,
            new Separator(),
            dirTitle,
            dirBox
        );
        
        return vbox;
    }
    
    /**
     * Creates the Cue Defaults tab content.
     */
    private VBox createCueDefaultsTab() {
        VBox vbox = new VBox(16);
        vbox.setPadding(new Insets(20));
        
        // Title
        Label title = new Label("Default Cue Timing");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        
        // Pre-Wait
        Label preWaitLabel = new Label("Pre-Wait (seconds):");
        preWaitSpinner = new Spinner<>(0.0, 60.0, playlistSettings.getDefaultPreWait(), 0.1);
        preWaitSpinner.setEditable(true);
        preWaitSpinner.setPrefWidth(150);
        
        // Post-Wait
        Label postWaitLabel = new Label("Post-Wait (seconds):");
        postWaitSpinner = new Spinner<>(0.0, 60.0, playlistSettings.getDefaultPostWait(), 0.1);
        postWaitSpinner.setEditable(true);
        postWaitSpinner.setPrefWidth(150);
        
        // Auto-Follow
        Label autoFollowLabel = new Label("Auto-Follow:");
        autoFollowCheckBox = new CheckBox("Enable auto-follow by default");
        autoFollowCheckBox.setSelected(playlistSettings.isDefaultAutoFollow());
        
        grid.add(preWaitLabel, 0, 0);
        grid.add(preWaitSpinner, 1, 0);
        grid.add(postWaitLabel, 0, 1);
        grid.add(postWaitSpinner, 1, 1);
        grid.add(autoFollowLabel, 0, 2);
        grid.add(autoFollowCheckBox, 1, 2);
        
        vbox.getChildren().addAll(title, grid);
        
        return vbox;
    }
    
    /**
     * Loads settings into UI controls.
     */
    private void loadSettingsIntoControls() {
        volumeSlider.setValue(playlistSettings.getMasterVolume());
        volumeLabel.setText(String.format("%.0f%%", playlistSettings.getMasterVolume() * 100));
        audioDirectoryField.setText(playlistSettings.getAudioFileDirectory());
        preWaitSpinner.getValueFactory().setValue(playlistSettings.getDefaultPreWait());
        postWaitSpinner.getValueFactory().setValue(playlistSettings.getDefaultPostWait());
        autoFollowCheckBox.setSelected(playlistSettings.isDefaultAutoFollow());
    }
    
    /**
     * Applies settings and saves them.
     */
    private void applySettings() {
        playlistSettings.setMasterVolume(volumeSlider.getValue());
        playlistSettings.setAudioFileDirectory(audioDirectoryField.getText());
        playlistSettings.setDefaultPreWait(preWaitSpinner.getValue());
        playlistSettings.setDefaultPostWait(postWaitSpinner.getValue());
        playlistSettings.setDefaultAutoFollow(autoFollowCheckBox.isSelected());
        
        // Save to .wlp file
        try {
            logger.info("Applying and saving playlist settings");
            playlistSettingsService.save(playlistPath, playlistSettings);
        } catch (Exception e) {
            logger.error("Failed to save playlist settings: {}", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save playlist settings: " + e.getMessage());
            alert.setTitle("Error");
            alert.showAndWait();
        }
    }
    
    /**
     * Opens a directory chooser for selecting the audio file directory.
     */
    private void browseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Audio File Directory");
        
        String currentDir = audioDirectoryField.getText();
        if (!currentDir.isEmpty()) {
            directoryChooser.setInitialDirectory(new File(currentDir));
        }
        
        File selectedDir = directoryChooser.showDialog(getDialogPane().getScene().getWindow());
        if (selectedDir != null) {
            audioDirectoryField.setText(selectedDir.getAbsolutePath());
        }
    }
}
