package com.winlabs.view;

import com.winlabs.controller.AudioController;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.model.Playlist;
import com.winlabs.service.PlaylistService;
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
import java.util.List;

//TODO: Add context menu for file operations (open, delete, properties, etc.)
//TODO: Add drag-and-drop support for adding files to cue list
//TODO: Add keyboard shortcuts for common actions (play, pause, stop, next cue, etc.) These should be configurable in settings.
//TODO: Make Settings Functional
//TODO: Add search/filter functionality to file browser
//TODO: Add context menu for opening files/folders
/**
 * Main application window for Win-Labs.
 * Contains the cue list, controls, and file browser.
 */
public class MainWindow extends Stage {
    
    private Playlist playlist;
    private TableView<Cue> cueTable;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Label statusLabel;
    private Label cueCountLabel;
    
    private AudioController audioController;
    private PlaylistService playlistService;
    private FileView fileView;
    private VBox fileViewContainer;
    private SplitPane splitPane;
    private boolean isFileViewVisible = false;
    
    public MainWindow() {
        this.playlist = new Playlist();
        this.audioController = new AudioController();
        this.playlistService = new PlaylistService();
        
        setupAudioControllerListeners();
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
        
        // Apply default dark theme
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        setScene(scene);
        
        // Add sample data for testing
        // TODO: Make so that this is only in debug mode
        addSampleCues();
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
        
        editMenu.getItems().addAll(addCueItem, deleteCueItem, new SeparatorMenuItem(), settingsItem);
        
        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem darkThemeItem = new MenuItem("Dark Theme");
        darkThemeItem.setOnAction(e -> applyTheme("/css/dark-theme.css"));
        
        MenuItem lightThemeItem = new MenuItem("Light Theme");
        lightThemeItem.setOnAction(e -> applyTheme("/css/light-theme.css"));
        //TODO: Fix Rainbow theme CSS
        MenuItem rainbowThemeItem = new MenuItem("Rainbow Theme");
        rainbowThemeItem.setOnAction(e -> applyTheme("/css/rainbow-theme.css"));
        
        viewMenu.getItems().addAll(darkThemeItem, lightThemeItem, rainbowThemeItem);
        
        // Help menu
        //TODO: Make functional
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        MenuItem documentationItem = new MenuItem("Documentation");
        
        helpMenu.getItems().addAll(documentationItem, new SeparatorMenuItem(), aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
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
        
        Button refreshButton = new Button("Refresh");
        
        toolbar.getItems().addAll(
            newCueButton,
            deleteCueButton,
            new Separator(),
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
        
        playButton = new Button("Play");
        playButton.setPrefWidth(100);
        playButton.setOnAction(e -> onGoClicked());
        
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
        
        controls.getChildren().addAll(playButton, pauseButton, stopButton, skipButton);
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
            case STOPPED:
            case PRE_WAIT:
            case POST_WAIT:
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
    private void newPlaylist() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("New Playlist");
        alert.setHeaderText("Create new playlist?");
        alert.setContentText("Any unsaved changes will be lost.");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            playlist.clear();
            playlist.setName("Untitled Playlist");
            playlist.setFilePath(null);
            updateCueCount();
            updateStatus("New playlist created");
        }
    }
    
    /**
     * Opens a playlist from a file.
     */
    private void openPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Playlist");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Playlist", "*.json")
        );
        
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                Playlist loadedPlaylist = playlistService.load(file.getAbsolutePath());
                playlist.clear();
                playlist.setName(loadedPlaylist.getName());
                playlist.setFilePath(loadedPlaylist.getFilePath());
                for (Cue cue : loadedPlaylist.getCues()) {
                    playlist.addCue(cue);
                }
                updateCueCount();
                updateStatus("Loaded playlist: " + loadedPlaylist.getName());
            } catch (Exception e) {
                showError("Failed to load playlist", e.getMessage());
            }
        }
    }
    
    /**
     * Saves the current playlist.
     */
    private void savePlaylist() {
        if (playlist.getFilePath() == null) {
            savePlaylistAs();
        } else {
            try {
                playlistService.save(playlist, playlist.getFilePath());
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
                playlistService.save(playlist, file.getAbsolutePath());
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
        alert.showAndWait();
    }
    
    /**
     * Applies a theme to the window.
     */
    private void applyTheme(String themePath) {
        Scene scene = getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());
        updateStatus("Theme changed");
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
}
