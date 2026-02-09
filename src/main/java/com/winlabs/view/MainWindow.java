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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//TODO: #69 Add context menu for file operations (open, delete, properties, etc.)
//TODO: #70 Add drag-and-drop support for adding files to cue list
//TODO: #71 Add keyboard shortcuts for common actions (go, pause, stop, next cue, etc.) These should be configurable in settings.
//TODO: #72 Add search/filter functionality to file browser
//TODO: #78 Add context menu for opening files/folders
//TODO: #73 Add an inspector panel for editing cue properties
//TODO: #74 make the cue table columns reorderable and resizable, and save/load their state with the playlist
//TODO: #75 Add multi-select support for cue table (for batch operations like delete, move, etc.) - make it so that Ctrl+Click and Shift+Click work as expected
//TODO: #76 Add drag-and-drop reordering of cues in the cue table
//TODO: #77 Make sure all specific key presses and actions can be remapped in settings

/**
 * Main application window for Win-Labs.
 * Contains the cue list, controls, and file browser.
 */
public class MainWindow extends Stage {
    
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
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

    private Runnable showDocumentationCallback;
    private Runnable showAboutDialogCallback;
    
    public MainWindow(Runnable showDocumentationCallback, Runnable showAboutDialogCallback) {
        this.showDocumentationCallback = showDocumentationCallback;
        this.showAboutDialogCallback = showAboutDialogCallback;
        logger.info("Initializing MainWindow");
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
            logger.debug("Settings loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load settings, using defaults", e);
            this.settings = new Settings();
        }
        
        setupAudioControllerListeners();
        
        // Apply settings to audio controller
        if (audioController.getAudioService() != null && 
            audioController.getAudioService().getPlayerPool() != null) {
            audioController.getAudioService().getPlayerPool()
                .setVolumeAll(settings.getMasterVolume());
            logger.debug("Applied master volume: {}", settings.getMasterVolume());
        }
        
        initializeUI();
        logger.info("MainWindow initialized successfully");
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
        
        // Recent Files submenu
        Menu recentFilesMenu = new Menu("Recent Files");
        updateRecentFilesMenu(recentFilesMenu);
        
        MenuItem saveItem = new MenuItem("Save Playlist");
        saveItem.setOnAction(e -> savePlaylist());
        
        MenuItem saveAsItem = new MenuItem("Save Playlist As...");
        saveAsItem.setOnAction(e -> savePlaylistAs());
        
        MenuItem backToWelcomeItem = new MenuItem("Back to Welcome Screen");
        backToWelcomeItem.setOnAction(e -> showWelcomeScreen());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> close());
        
        fileMenu.getItems().addAll(
            newItem, new SeparatorMenuItem(),
            openItem, recentFilesMenu, new SeparatorMenuItem(),
            saveItem, saveAsItem, new SeparatorMenuItem(),
            backToWelcomeItem, new SeparatorMenuItem(),
            exitItem
        );
        
        // Edit menu
        //TODO: Make functional
        Menu editMenu = new Menu("Edit");
        editMenu.setDisable(true); // Disable until functional
        MenuItem addCueItem = new MenuItem("Add Cue");
        addCueItem.setOnAction(e -> addNewCue());
        MenuItem deleteCueItem = new MenuItem("Delete Cue");
        deleteCueItem.setOnAction(e -> deleteSelectedCue());
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> openSettings());
        
        editMenu.getItems().addAll(addCueItem, deleteCueItem, new SeparatorMenuItem(), settingsItem);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem documentationItem = new MenuItem("Documentation");
        documentationItem.setOnAction(e -> showDocumentation());
        MenuItem viewLogsItem = new MenuItem("View Logs...");
        viewLogsItem.setOnAction(e -> openLogViewer());
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().addAll(
            documentationItem, 
            new SeparatorMenuItem(), 
            viewLogsItem,
            new SeparatorMenuItem(),
            aboutItem
        );
        
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
        
        controls.getChildren().addAll(goButton, pauseButton, stopButton);
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
        Button fileViewToggle = new Button("FILE VIEW"); // Placeholder text
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
        logger.debug("Added new cue with number {}", nextNumber);
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
        logger.info("Creating new playlist (skipConfirmation={})", skipConfirmation);
        if (!skipConfirmation) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("New Playlist");
            alert.setHeaderText("Create new playlist?");
            alert.setContentText("Any unsaved changes will be lost.");
            applyThemeToDialog(alert);
            
            if (alert.showAndWait().get() != ButtonType.OK) {
                logger.debug("New playlist creation cancelled by user");
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
        logger.info("New playlist created successfully");
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
                
                // Add to recent files
                settings.addRecentFile(filePath.toString());
                try {
                    settingsService.save(settings);
                } catch (Exception ex) {
                    logger.error("Failed to save recent file to settings", ex);
                }
                
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
        logger.error("Error dialog shown: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyThemeToDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Opens the log viewer window.
     */
    private void openLogViewer() {
        try {
            logger.info("Opening log viewer");
            LogViewer logViewer = new LogViewer(settings);
            logViewer.show();
        } catch (Exception e) {
            logger.error("Failed to open log viewer", e);
            showError("Error", "Failed to open log viewer: " + e.getMessage());
        }
    }
    
    /**
     * Shows the documentation.
     */
    private void showDocumentation() {
        if (showDocumentationCallback != null) {
            showDocumentationCallback.run();
        }
    }
    
    /**
     * Shows the about dialog.
     */
    private void showAboutDialog() {
        if (showAboutDialogCallback != null) {
            showAboutDialogCallback.run();
        }
    }
    
    /**
     * Shows the welcome screen and hides this main window.
     */
    public void showWelcomeScreen() {
        final WelcomeScreen[] welcomeScreenRef = new WelcomeScreen[1];
        
        welcomeScreenRef[0] = new WelcomeScreen(
            () -> {
                // On new playlist
                if (welcomeScreenRef[0] != null) {
                    welcomeScreenRef[0].closeWelcomeScreen();
                }
                this.show();
                this.newPlaylist(true);
            },
            () -> {
                // On open playlist
                if (welcomeScreenRef[0] != null) {
                    welcomeScreenRef[0].closeWelcomeScreen();
                }
                this.show();
                this.openPlaylist();
            },
            () -> {
                // On settings
                this.openSettings(settings, settingsService);
            },
            () -> {
                // On documentation
                showDocumentation();
            },
            () -> {
                // On about
                showAboutDialog();
            },
            () -> {
                // On exit
                if (welcomeScreenRef[0] != null) {
                    welcomeScreenRef[0].closeWelcomeScreen();
                }
                System.exit(0);
            }
        );
        
        welcomeScreenRef[0].applyTheme(settings.getTheme());
        
        // Set callback for opening recent playlists
        welcomeScreenRef[0].setOnOpenRecentPlaylist((filePath) -> {
            welcomeScreenRef[0].closeWelcomeScreen();
            this.show();
            this.openRecentFile(filePath);
        });
        
        // Set callback for toggling pin status
        welcomeScreenRef[0].setOnTogglePin((filePath) -> {
            try {
                boolean nowPinned = settings.togglePinned(filePath);
                settingsService.save(settings);
                logger.info("Toggled pin for {}: {}", filePath, nowPinned);
                
                // Refresh the welcome screen to update the display
                welcomeScreenRef[0].close();
                showWelcomeScreen();
            } catch (Exception e) {
                logger.error("Failed to toggle pin for {}", filePath, e);
                showError("Failed to toggle pin", e.getMessage());
            }
        });
        
        // Build combined list: pinned playlists + recent playlists
        List<RecentPlaylist> allPlaylists = new ArrayList<>();
        
        // First, add all pinned playlists
        Set<String> pinnedFiles = settings.getPinnedPlaylists();
        logger.info("Loading pinned playlists: {}", pinnedFiles != null ? pinnedFiles.size() : 0);
        if (pinnedFiles != null) {
            for (String filePath : pinnedFiles) {
                try {
                    Path path = Paths.get(filePath);
                    String fileName = path.getFileName().toString();
                    String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                    
                    RecentPlaylist playlist = new RecentPlaylist(playlistName, filePath);
                    playlist.setIsPinned(true);
                    allPlaylists.add(playlist);
                    logger.debug("Added pinned playlist: {}", filePath);
                } catch (Exception e) {
                    logger.warn("Failed to add pinned playlist: {}", filePath, e);
                }
            }
        }
        
        // Then, add recent (non-pinned) playlists
        List<String> recentFiles = settings.getRecentFiles();
        logger.info("Loading recent files: {}", recentFiles != null ? recentFiles.size() : 0);
        if (recentFiles != null) {
            for (String filePath : recentFiles) {
                // Skip if already added as pinned
                if (pinnedFiles != null && pinnedFiles.contains(filePath)) {
                    continue;
                }
                
                try {
                    Path path = Paths.get(filePath);
                    String fileName = path.getFileName().toString();
                    String playlistName = fileName.replaceFirst("[.][^.]+$", "");
                    
                    RecentPlaylist playlist = new RecentPlaylist(playlistName, filePath);
                    playlist.setIsPinned(false);
                    allPlaylists.add(playlist);
                } catch (Exception e) {
                    logger.warn("Failed to add recent playlist: {}", filePath, e);
                }
            }
        }
        
        welcomeScreenRef[0].updateRecentPlaylists(allPlaylists);
        
        this.hide();
        welcomeScreenRef[0].show();
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
            if (audioController.getAudioService() != null && 
                audioController.getAudioService().getPlayerPool() != null) {
                audioController.getAudioService().getPlayerPool()
                    .setVolumeAll(settings.getMasterVolume());
            }
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
    
    /**
     * Updates the Recent Files submenu with the current list of recent files.
     * 
     * @param recentFilesMenu The Recent Files menu to update
     */
    private void updateRecentFilesMenu(Menu recentFilesMenu) {
        recentFilesMenu.getItems().clear();
        
        List<String> recentFiles = settings.getRecentFiles();
        
        if (recentFiles == null || recentFiles.isEmpty()) {
            MenuItem emptyItem = new MenuItem("(No recent files)");
            emptyItem.setDisable(true);
            recentFilesMenu.getItems().add(emptyItem);
            return;
        }
        
        // Add menu items for each recent file
        for (String filePath : recentFiles) {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            
            MenuItem fileItem = new MenuItem(fileName);
            fileItem.setOnAction(e -> openRecentFile(filePath));
            
            // Set tooltip with full path
            Tooltip tooltip = new Tooltip(filePath);
            Tooltip.install(fileItem.getGraphic(), tooltip);
            
            recentFilesMenu.getItems().add(fileItem);
        }
        
        // Add separator and "Clear Recent Files" option
        if (!recentFiles.isEmpty()) {
            recentFilesMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clearItem = new MenuItem("Clear Recent Files");
            clearItem.setOnAction(e -> clearRecentFiles());
            recentFilesMenu.getItems().add(clearItem);
        }
    }
    
    /**
     * Opens a recent file from the recent files list.
     * 
     * @param filePath The absolute path to the playlist file
     */
    private void openRecentFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            // Check if file exists
            if (!Files.exists(path)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File Not Found");
                alert.setHeaderText("Recent file not found");
                alert.setContentText("The file no longer exists:\\n" + filePath + "\\n\\nRemove from recent files?");
                applyThemeToDialog(alert);
                
                ButtonType removeButton = new ButtonType("Remove");
                ButtonType cancelButton = ButtonType.CANCEL;
                alert.getButtonTypes().setAll(removeButton, cancelButton);
                
                if (alert.showAndWait().get() == removeButton) {
                    settings.removeRecentFile(filePath);
                    try {
                        settingsService.save(settings);
                    } catch (Exception ex) {
                        logger.error("Failed to save settings after removing recent file", ex);
                    }
                }
                return;
            }
            
            // Load the playlist
            Playlist loadedPlaylist = playlistService.load(filePath);
            playlist.clear();
            playlist.setName(loadedPlaylist.getName());
            playlist.setFilePath(filePath);
            for (Cue cue : loadedPlaylist.getCues()) {
                playlist.addCue(cue);
            }
            
            // Load playlist settings
            currentPlaylistPath = path;
            playlistSettings = playlistSettingsService.load(path);
            playlistInitialized = true;
            
            // Move to front of recent files list
            settings.addRecentFile(filePath);
            try {
                settingsService.save(settings);
            } catch (Exception ex) {
                logger.error("Failed to save recent file to settings", ex);
            }
            
            updateCueCount();
            updateStatus("Loaded playlist: " + loadedPlaylist.getName());
            logger.info("Opened recent playlist: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to open recent file: {}", filePath, e);
            showError("Failed to open playlist", e.getMessage());
        }
    }
    
    /**
     * Clears all recent files from the list.
     */
    private void clearRecentFiles() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Recent Files");
        alert.setHeaderText("Clear all recent files?");
        alert.setContentText("This will remove all recent files from the list.");
        applyThemeToDialog(alert);
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            settings.clearRecentFiles();
            try {
                settingsService.save(settings);
                updateStatus("Recent files cleared");
                logger.info("Recent files list cleared");
            } catch (Exception e) {
                logger.error("Failed to save settings after clearing recent files", e);
                showError("Failed to clear recent files", e.getMessage());
            }
        }
    }
}
