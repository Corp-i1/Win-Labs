package com.winlabs.view;

import com.winlabs.controller.AudioController;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.model.Playlist;
import com.winlabs.model.PlaylistSettings;
import com.winlabs.model.RecentPlaylist;
import com.winlabs.model.Settings;
import com.winlabs.service.PlaylistService;
import com.winlabs.service.PlaylistSettingsService;
import com.winlabs.service.SettingsService;
import com.winlabs.util.PathUtil;
import com.winlabs.util.TimeUtil;
import com.winlabs.view.components.FileView;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//TODO: Add context menu for file operations (open, delete, properties, etc.)
//TODO: Add drag-and-drop support for adding files to cue list
//TODO: Add keyboard shortcuts for common actions (go, pause, stop, next cue, etc.) These should be configurable in settings.
//TODO: Add search/filter functionality to file browser
//TODO: Add context menu for opening files/folders
//TODO: Add an inspector panel for editing cue properties
//TODO: make the cue table columns reorderable and resizable, and save/load their state with the playlist
//TODO: Add multi-select support for cue table (for batch operations like delete, move, etc.) - make it so that Ctrl+Click and Shift+Click work as expected
//TODO: Add drag-and-drop reordering of cues in the cue table
//TODO: Make sure all specific key presses and actions can be remapped in settings
//TDOO: Add Recent Files menu to quickly access recently opened playlists
//TODO: Make Debug Mode that enables extra logging and features for testing
//TODO: Add Basic Logging functionality to log important events and errors to a file for troubleshooting - Diffrentiate between info, warning, and error logs - adjust what gets reported in the application in settings + log level + log file location + log rotation + log format + log viewer + export logs + toggle logging on/off
/**
 * Main application window for Win-Labs.
 * Contains the cue list, controls, and file browser.
 */
public class MainWindow extends Stage {
    
    private Playlist playlist;
    private PlaylistSettings playlistSettings;
    private Path currentPlaylistPath;
    private boolean playlistInitialized = false; // Track if playlist was intentionally created/loaded
    
    private TableView<Cue> cueTable;
    private Button goButton;
    private Button pauseButton;
    private Button stopButton;
    private Label statusLabel;
    private Label cueCountLabel;
    
    private AudioController audioController;
    private PlaylistService playlistService;
    private PlaylistSettingsService playlistSettingsService;
    private SettingsService settingsService;
    private Settings settings;
    private FileView fileView;
    private VBox fileViewContainer;
    private SplitPane splitPane;
    private boolean isFileViewVisible = false;
    
    public MainWindow() {
        this.playlist = new Playlist();
        this.playlistSettings = new PlaylistSettings();
        this.currentPlaylistPath = null;
        this.audioController = new AudioController();
        this.playlistService = new PlaylistService();
        this.playlistSettingsService = new PlaylistSettingsService();
        this.settingsService = new SettingsService();
        
        // Load settings
        try {
            this.settings = settingsService.load();
        } catch (Exception e) {
            System.err.println("Failed to load settings, using defaults: " + e.getMessage());
            this.settings = new Settings();
        }
        
        setupAudioControllerListeners();
        
        // Apply settings to audio controller
        audioController.setVolume(settings.getMasterVolume());
        
        initializeUI();
    }
    
    /**
     * Sets up listeners for the audio controller.
     */
    private void setupAudioControllerListeners() {
        audioController.setStatusUpdateListener(this::updateStatus);
        audioController.setStateChangeListener(this::handleStateChange);
        audioController.setOnCueCompleteListener(this::playNextCue);
    }
    
    private void initializeUI() {
        // Set window properties
        setTitle("Win-Labs - Cue List Manager");
        setWidth(1200);
        setHeight(800);
        
        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top: Menu bar and toolbar
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createToolbar());
        root.setTop(topContainer);
        
        // Center: Split pane with cue list and file browser
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        
        // Left: Cue list table
        VBox cueListContainer = new VBox();
        Label cueListLabel = new Label("Cue List");
        cueListLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        cueListLabel.setPadding(new Insets(5));
        cueListContainer.getChildren().addAll(cueListLabel, createCueTable());
        VBox.setVgrow(cueTable, Priority.ALWAYS);
        
        // Right: File browser
        fileViewContainer = new VBox();
        Label fileViewLabel = new Label("File Browser");
        fileViewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        fileViewLabel.setPadding(new Insets(5));
        fileView = new FileView();
        
        // Handle double-click for tree view
        fileView.getTreeFileView().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleFileDoubleClick();
            }
        });
        
        // Handle double-click for browser view
        fileView.getBrowserFileView().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleFileDoubleClick();
            }
        });
        
        fileViewContainer.getChildren().addAll(fileViewLabel, fileView);
        VBox.setVgrow(fileView, Priority.ALWAYS);
        
        // Start with only cue list (file view hidden by default)
        splitPane.getItems().add(cueListContainer);
        root.setCenter(splitPane);
        
        // Bottom: Playback controls and status
        VBox bottomContainer = new VBox(10);
        bottomContainer.getChildren().addAll(createPlaybackControls(), createStatusBar());
        root.setBottom(bottomContainer);
        
        // Create scene
        Scene scene = new Scene(root);
        setScene(scene);
        
        // Apply saved theme or default to dark
        applyThemeFromSettings();
        
        // Sample cues are commented out - remove this when in production
        // Users should start with empty playlist for new playlists
        // addSampleCues();
    }
    
    /**
     * Creates the menu bar.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New Playlist");
        newItem.setOnAction(e -> newPlaylist());
        
        MenuItem openItem = new MenuItem("Open Playlist...");
        openItem.setOnAction(e -> openPlaylist());
        
        MenuItem saveItem = new MenuItem("Save Playlist");
        saveItem.setOnAction(e -> savePlaylist());
        
        MenuItem saveAsItem = new MenuItem("Save Playlist As...");
        saveAsItem.setOnAction(e -> savePlaylistAs());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> close());
        
        fileMenu.getItems().addAll(
            newItem, new SeparatorMenuItem(),
            openItem, saveItem, saveAsItem, new SeparatorMenuItem(),
            exitItem
        );
        
        // Edit menu
        //TODO: Make functional
        Menu editMenu = new Menu("Edit");
        MenuItem addCueItem = new MenuItem("Add Cue");
        MenuItem deleteCueItem = new MenuItem("Delete Cue");
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> openSettings());
        
        editMenu.getItems().addAll(addCueItem, deleteCueItem, new SeparatorMenuItem(), settingsItem);
        
        // Help menu
        //TODO: Make functional
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        MenuItem documentationItem = new MenuItem("Documentation");
        
        helpMenu.getItems().addAll(documentationItem, new SeparatorMenuItem(), aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }
    
    /**
     * Creates the toolbar.
     */
    //TODO: Add More toolbar buttons and functionality
    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        
        Button newCueButton = new Button("New Cue");
        newCueButton.setOnAction(e -> addNewCue());
        
        Button deleteCueButton = new Button("Delete");
        deleteCueButton.setOnAction(e -> deleteSelectedCue());
        
        Button playlistSettingsBtn = new Button("Playlist Settings");
        playlistSettingsBtn.setOnAction(e -> openPlaylistSettings());
        
        Button refreshButton = new Button("Refresh");
        
        toolbar.getItems().addAll(
            newCueButton,
            deleteCueButton,
            new Separator(),
            playlistSettingsBtn,
            refreshButton
        );
        
        return toolbar;
    }
    
    /**
     * Creates the cue list table.
     */
    private TableView<Cue> createCueTable() {
        cueTable = new TableView<>();
        cueTable.setItems(playlist.getCues());
        
        // Number column
        TableColumn<Cue, Integer> numberCol = new TableColumn<>("No.");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        numberCol.setPrefWidth(50);
        
        // Name column
        TableColumn<Cue, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        
        // Duration column
        TableColumn<Cue, Double> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setPrefWidth(80);
        // Format duration as MM:SS
        durationCol.setCellFactory(col -> new TableCell<Cue, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(TimeUtil.formatTime(item));
                }
            }
        });
        
        // Pre-wait column
        TableColumn<Cue, Double> preWaitCol = new TableColumn<>("Pre-Wait");
        preWaitCol.setCellValueFactory(new PropertyValueFactory<>("preWait"));
        preWaitCol.setPrefWidth(80);
        preWaitCol.setCellFactory(col -> new TableCell<Cue, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(TimeUtil.formatTime(item));
                }
            }
        });
        
        // Post-wait column
        TableColumn<Cue, Double> postWaitCol = new TableColumn<>("Post-Wait");
        postWaitCol.setCellValueFactory(new PropertyValueFactory<>("postWait"));
        postWaitCol.setPrefWidth(80);
        postWaitCol.setCellFactory(col -> new TableCell<Cue, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(TimeUtil.formatTime(item));
                }
            }
        });
        
        // Auto-follow column
        TableColumn<Cue, Boolean> autoFollowCol = new TableColumn<>("Auto-Follow");
        autoFollowCol.setCellValueFactory(new PropertyValueFactory<>("autoFollow"));
        autoFollowCol.setPrefWidth(90);
        autoFollowCol.setCellFactory(col -> new TableCell<Cue, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Yes" : "No");
                }
            }
        });
        
        // File path column
        TableColumn<Cue, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        fileCol.setPrefWidth(300);
        
		cueTable.getColumns().addAll(List.of(
			numberCol, nameCol, durationCol, preWaitCol, 
			postWaitCol, autoFollowCol, fileCol
		));
        
        return cueTable;
    }
    
    /**
     * Creates the playback control buttons.
     */
    private HBox createPlaybackControls() {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER);
        
        goButton = new Button("Go");
        goButton.setPrefWidth(100);
        goButton.setOnAction(e -> onGoClicked());
        
        pauseButton = new Button("Pause");
        pauseButton.setPrefWidth(100);
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> onPauseResumeToggle());
        
        stopButton = new Button("Stop");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> onStopClicked());
        
        Button skipButton = new Button("Skip");
        skipButton.setPrefWidth(100);
        //TODO: Implement skip functionality
        
        controls.getChildren().addAll(goButton, pauseButton, stopButton, skipButton);
        return controls;
    }
    
    /**
     * Creates the status bar.
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #2d2d30;");
        
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: white;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        cueCountLabel = new Label("Cues: 0");
        cueCountLabel.setStyle("-fx-text-fill: white;");
        updateCueCount();
        
        // File view toggle button
        Button fileViewToggle = new Button("📁");
        fileViewToggle.setTooltip(new Tooltip("Toggle File View"));
        fileViewToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px;");
        fileViewToggle.setOnAction(e -> toggleFileView());
        
        statusBar.getChildren().addAll(statusLabel, spacer, cueCountLabel, fileViewToggle);
        return statusBar;
    }
    
    /**
     * Toggles the visibility of the file view.
     */
    private void toggleFileView() {
        isFileViewVisible = !isFileViewVisible;
        
        if (isFileViewVisible) {
            // Show file view
            if (!splitPane.getItems().contains(fileViewContainer)) {
                splitPane.getItems().add(fileViewContainer);
                splitPane.setDividerPositions(0.7);
            }
        } else {
            // Hide file view
            splitPane.getItems().remove(fileViewContainer);
        }
    }
    
    /**
     * Adds a new empty cue to the playlist.
     */
    private void addNewCue() {
        int nextNumber = playlist.size() + 1;
        Cue newCue = new Cue(nextNumber, "New Cue", "");
        playlist.addCue(newCue);
        updateCueCount();
        updateStatus("Added new cue");
    }
    
    /**
     * Deletes the selected cue from the playlist.
     */
    private void deleteSelectedCue() {
        Cue selectedCue = cueTable.getSelectionModel().getSelectedItem();
        if (selectedCue != null) {
            playlist.removeCue(selectedCue);
            playlist.renumberCues();
            updateCueCount();
            updateStatus("Deleted cue: " + selectedCue.getName());
        }
    }
    
    /**
     * Handles GO button click.
     * Plays the currently selected cue and advances selection to next cue.
     */
    private void onGoClicked() {
        Cue selectedCue = cueTable.getSelectionModel().getSelectedItem();
        if (selectedCue != null) {
            // Play the selected cue
            audioController.playCue(selectedCue);
            
            // Immediately advance to next cue
            int currentIndex = playlist.getCues().indexOf(selectedCue);
            if (currentIndex >= 0 && currentIndex < playlist.size() - 1) {
                Cue nextCue = playlist.getCue(currentIndex + 1);
                cueTable.getSelectionModel().select(nextCue);
                updateStatus("Playing: " + selectedCue.getName() + " | Next: " + nextCue.getName());
            } else {
                updateStatus("Playing: " + selectedCue.getName() + " (Last cue)");
            }
        } else {
            updateStatus("No cue selected");
        }
    }
    
    /**
     * Handles pause/resume toggle button click.
     * Toggles between pause and resume states.
     */
    private void onPauseResumeToggle() {
        if (audioController.getState() == PlaybackState.PAUSED) {
            audioController.resume();
        } else {
            audioController.pause();
        }
    }
    
    /**
     * Handles stop button click.
     */
    private void onStopClicked() {
        audioController.stop();
    }
    
    /**
     * Plays the next cue in the list (for auto-follow).
     */
    private void playNextCue() {
        Cue currentCue = audioController.getCurrentCue();
        if (currentCue == null) {
            return;
        }
        
        int currentIndex = playlist.getCues().indexOf(currentCue);
        if (currentIndex >= 0 && currentIndex < playlist.size() - 1) {
            Cue nextCue = playlist.getCue(currentIndex + 1);
            cueTable.getSelectionModel().select(nextCue);
            audioController.playCue(nextCue);
        }
    }
    
    /**
     * Handles state changes from the audio controller.
     */
    private void handleStateChange(PlaybackState state) {
        switch (state) {
            case PLAYING:
                // GO button stays enabled for multi-track
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                break;
            case PAUSED:
                pauseButton.setText("Resume");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                break;
            case PRE_WAIT:
                // During pre-wait, allow pausing the timer
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                break;
            case POST_WAIT:
                // During post-wait, allow pausing the timer
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                break;
            case STOPPED:
                // GO button stays enabled for multi-track
                pauseButton.setText("Pause");
                pauseButton.setDisable(true);
                stopButton.setDisable(true);
                break;
        }
    }
    
    /**
     * Handles double-click on file browser.
     */
    private void handleFileDoubleClick() {
        Path selectedPath = fileView.getSelectedPath();
        if (selectedPath != null && PathUtil.isAudioFile(selectedPath)) {
            addCueFromFile(selectedPath);
        }
    }
    
    /**
     * Adds a cue from a file path.
     */
    private void addCueFromFile(Path filePath) {
        int nextNumber = playlist.size() + 1;
        String fileName = PathUtil.getFileNameWithoutExtension(filePath);
        Cue newCue = new Cue(nextNumber, fileName, filePath.toString());
        playlist.addCue(newCue);
        updateCueCount();
        updateStatus("Added cue: " + fileName);
    }
    
    /**
     * Creates a new empty playlist.
     */
    /**
     * Creates a new playlist.
     * Shows confirmation dialog if skipConfirmation is false.
     */
    public void newPlaylist(boolean skipConfirmation) {
        if (!skipConfirmation) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("New Playlist");
            alert.setHeaderText("Create new playlist?");
            alert.setContentText("Any unsaved changes will be lost.");
            applyThemeToDialog(alert);
            
            if (alert.showAndWait().get() != ButtonType.OK) {
                return; // User cancelled
            }
        }
        
        playlist.clear();
        playlist.setName("Untitled Playlist");
        currentPlaylistPath = null;
        playlistSettings.resetToDefaults();
        playlistInitialized = true;
        updateCueCount();
        updateStatus("New playlist created");
    }
    
    /**
     * Creates a new playlist (with confirmation).
     */
    public void newPlaylist() {
        newPlaylist(false);
    }
    
    /**
     * Opens a playlist from a file.
     */
    public void openPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Playlist");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Win-Labs Playlist", "*.wlp"),
            new FileChooser.ExtensionFilter("JSON Playlist", "*.json"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                Path filePath = file.toPath();
                
                // Handle .wlp files by loading the corresponding .json
                if (file.getName().endsWith(".wlp")) {
                    String jsonPath = file.getAbsolutePath().replaceAll("\\.wlp$", ".json");
                    filePath = new File(jsonPath).toPath();
                }
                
                Playlist loadedPlaylist = playlistService.load(filePath.toString());
                playlist.clear();
                playlist.setName(loadedPlaylist.getName());
                playlist.setFilePath(filePath.toString());
                for (Cue cue : loadedPlaylist.getCues()) {
                    playlist.addCue(cue);
                }
                
                // Load playlist settings
                currentPlaylistPath = filePath;
                playlistSettings = playlistSettingsService.load(filePath);
                playlistInitialized = true;
                
                updateCueCount();
                updateStatus("Loaded playlist: " + loadedPlaylist.getName());
            } catch (Exception e) {
                showError("Failed to load playlist", e.getMessage());
            }
        }
    }
    
    /**
     * Saves the current playlist and its settings.
     */
    private void savePlaylist() {
        if (playlist.getFilePath() == null || currentPlaylistPath == null) {
            savePlaylistAs();
        } else {
            try {
                playlistService.save(playlist, playlist.getFilePath());
                playlistSettingsService.save(currentPlaylistPath, playlistSettings);
                updateStatus("Playlist saved");
            } catch (Exception e) {
                showError("Failed to save playlist", e.getMessage());
            }
        }
    }
    
    /**
     * Saves the playlist to a new file.
     */
    private void savePlaylistAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Playlist As");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Playlist", "*.json")
        );
        fileChooser.setInitialFileName(playlist.getName() + ".json");
        
        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try {
                Path filePath = file.toPath();
                playlistService.save(playlist, filePath.toString());
                playlistSettings.setPlaylistName(playlist.getName());
                playlistSettingsService.save(filePath, playlistSettings);
                currentPlaylistPath = filePath;
                playlist.setFilePath(filePath.toString());
                updateStatus("Playlist saved as: " + file.getName());
            } catch (Exception e) {
                showError("Failed to save playlist", e.getMessage());
            }
        }
    }
    
    /**
     * Updates the cue count label.
     */
    private void updateCueCount() {
        cueCountLabel.setText("Cues: " + playlist.size());
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
     * Opens the settings dialog (public version for external callers).
     * Can be called from WelcomeScreen or other windows.
     */
    public void openSettings(Settings settings, SettingsService settingsService) {
        try {
            this.settings = settings;
            this.settingsService = settingsService;
            openSettings();
        } catch (Exception e) {
            showError("Settings Error", "Failed to open settings: " + e.getMessage());
        }
    }
    
    /**
     * Opens the settings dialog (private internal version).
     */
    private void openSettings() {
        SettingsWindow settingsWindow = new SettingsWindow(settings, settingsService);
        // Apply current theme to settings window
        applyThemeToWindow(settingsWindow);
        settingsWindow.setOnApply(updatedSettings -> {
            // Apply theme change to main window
            applyThemeFromSettings();
            // Also update the settings window's theme
            applyThemeToWindow(settingsWindow);
            // Apply audio settings
            audioController.setVolume(settings.getMasterVolume());
            updateStatus("Settings applied");
        });
        settingsWindow.showAndWait();
    }
    
    /**
     * Opens the playlist settings dialog.
     */
    private void openPlaylistSettings() {
        if (currentPlaylistPath != null) {
            PlaylistSettingsWindow playlistSettingsWindow = new PlaylistSettingsWindow(
                this,
                playlistSettings,
                playlistSettingsService,
                currentPlaylistPath
            );
            // Apply theme before showing
            if (playlistSettingsWindow.getDialogPane() != null) {
                String themePath = "/css/" + settings.getTheme() + "-theme.css";
                playlistSettingsWindow.getDialogPane().getStylesheets().clear();
                playlistSettingsWindow.getDialogPane().getStylesheets().add(getClass().getResource(themePath).toExternalForm());
            }
            playlistSettingsWindow.showAndWait();
            updateStatus("Playlist settings updated");
        } else {
            showError("No Playlist Open", "Please open or create a playlist first.");
        }
    }
    
    /**
     * Opens the documentation (placeholder).
     */
    private void openDocumentation() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Documentation");
        alert.setHeaderText("Win-Labs Documentation");
        alert.setContentText("Documentation will be available soon.\n\nVisit https://github.com/Corp-i1/Win-Labs for more information.");
        applyThemeToDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Changes the theme and saves the setting.
     */
    private void changeTheme(String themeName) {
        settings.setTheme(themeName);
        try {
            settingsService.save(settings);
        } catch (Exception e) {
            System.err.println("Failed to save theme setting: " + e.getMessage());
        }
        applyThemeFromSettings();
        updateStatus("Theme changed to " + themeName);
    }
    
    /**
     * Applies the theme from current settings.
     */
    private void applyThemeFromSettings() {
        String themePath = "/css/" + settings.getTheme() + "-theme.css";
        applyTheme(themePath);
    }
    
    /**
     * Applies a theme to the window.
     */
    private void applyTheme(String themePath) {
        Scene scene = getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());
    }
    
    /**
     * Applies the current theme to another window.
     */
    private void applyThemeToWindow(Stage window) {
        if (window.getScene() != null) {
            String themePath = "/css/" + settings.getTheme() + "-theme.css";
            window.getScene().getStylesheets().clear();
            window.getScene().getStylesheets().add(getClass().getResource(themePath).toExternalForm());
        }
    }
    
    /**
     * Applies the current theme to a dialog.
     */
    private void applyThemeToDialog(Dialog<?> dialog) {
        if (dialog.getDialogPane() != null) {
            String themePath = "/css/" + settings.getTheme() + "-theme.css";
            dialog.getDialogPane().getStylesheets().clear();
            dialog.getDialogPane().getStylesheets().add(getClass().getResource(themePath).toExternalForm());
        }
    }
    
    /**
     * Updates the status label.
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * Adds sample cues for testing.
     */
    private void addSampleCues() {
        Cue cue1 = new Cue(1, "Opening Music", "C:/Music/opening.mp3");
        cue1.setDuration(180.0);
        cue1.setPreWait(5.0);
        cue1.setPostWait(3.0);
        cue1.setAutoFollow(true);
        
        Cue cue2 = new Cue(2, "Intermission", "C:/Music/intermission.wav");
        cue2.setDuration(300.0);
        cue2.setAutoFollow(false);
        
        Cue cue3 = new Cue(3, "Closing Music", "C:/Music/closing.mp3");
        cue3.setDuration(240.0);
        cue3.setPreWait(2.0);
        
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);
    }
    
    /**
     * Opens a playlist file by path (for .wlp file argument handling).
     * 
     * @param filePath The path to the playlist file
     */
    public void openPlaylistFile(String filePath) {
        try {
            currentPlaylistPath = Paths.get(filePath);
            PlaylistService playlistService = new PlaylistService();
            playlist.clear();
            Playlist loadedPlaylist = playlistService.load(filePath);
            for (Cue cue : loadedPlaylist.getCues()) {
                playlist.addCue(cue);
            }
            playlistInitialized = true;
            updateStatus("Opened playlist: " + filePath);
        } catch (Exception e) {
            showError("Failed to open playlist", e.getMessage());
            currentPlaylistPath = null;
        }
    }
    
    /**
     * Checks if a playlist has been loaded or created.
     * Returns true if a playlist was intentionally created or loaded.
     * 
     * @return true if a playlist is loaded or created, false otherwise
     */
    public boolean hasPlaylistLoaded() {
        return playlistInitialized;
    }
}
