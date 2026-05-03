package com.winlabs.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.winlabs.controller.AudioController;
import com.winlabs.controller.CueController;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.model.Playlist;
import com.winlabs.model.PlaylistSettings;
import com.winlabs.model.RecentPlaylist;
import com.winlabs.model.Settings;
import com.winlabs.service.KeyBindingService;
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
import javafx.application.Platform;
import javafx.event.EventHandler;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableRow;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;
//TODO: #71 Add keyboard shortcuts for common actions (go, pause, stop, next cue, etc.) These should be configurable in settings.
//TODO: #72 Add search/filter functionality to file browser
//TODO: #78 Add context menu for opening files/folders
//TODO: #73 Add an inspector panel for editing cue properties
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
    private CueController cueController;
    private PlaylistService playlistService;
    private PlaylistSettingsService playlistSettingsService;
    private SettingsService settingsService;
    private Settings settings;
    private FileView fileView;
    private VBox fileViewContainer;
    private SplitPane splitPane;
    private boolean isFileViewVisible = false;
    
    // Keyboard acceleration state: track last execution time per action to prevent
    // shortcuts from firing multiple times during a single key hold
    private final Map<String, Long> lastActionExecutionTime = new HashMap<>();
    private static final long ACTION_DEBOUNCE_MS = 50; // Minimum ms between same action firings
    // Track accelerators we've added so we can clear them when re-registering
    private final Set<KeyCombination> registeredAccelerators = new java.util.HashSet<>();
    // Event filter attached to the scene to capture key presses before nodes consume them
    private EventHandler<KeyEvent> acceleratorEventFilter = null;
    private boolean conflictWarningShown = false;

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
        this.cueController = new CueController(this.audioController);
        this.playlistService = new PlaylistService();
        this.playlistSettingsService = new PlaylistSettingsService();
        this.settingsService = new SettingsService();
        
        // Load settings
        try {
            this.settings = settingsService.load();
            logger.debug("Settings loaded successfully");
        } catch (IOException e) {
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
     * Handle an accelerator-invoked action. Respects `allowKeyRepeat` and applies
     * per-action debouncing when repeats are disabled.
     */
    private void handleAcceleratorAction(String actionId) {
        if (actionId == null || actionId.isEmpty()) return;

        try {
            logger.debug("Accelerator invoked: {}", actionId);
            boolean allowRepeat = settings.getApplicationSettings().isAllowKeyRepeat();
            long now = System.currentTimeMillis();

            if (!allowRepeat) {
                Long lastExecution = lastActionExecutionTime.get(actionId);
                if (lastExecution != null && (now - lastExecution) < ACTION_DEBOUNCE_MS) {
                    logger.trace("Skipping action '{}' due to debounce ({} ms since last)", actionId, (now - lastExecution));
                    // Skip repeated firing while key is held
                    return;
                }
                lastActionExecutionTime.put(actionId, now);
            }

            logger.info("Executing keyboard action via accelerator: {}", actionId);
            executeKeyboardAction(actionId);
        } catch (Exception e) {
            logger.error("Error handling accelerator action {}: {}", actionId, e.getMessage(), e);
        }
    }

    /**
     * Registers keyboard accelerators from application settings.
     * Parses stored key bindings and registers them on the scene for global key dispatch.
     */
    private void registerKeyboardAccelerators(Scene keyboardAccelScene) {
        if (keyboardAccelScene == null) {
            logger.warn("Cannot register keyboard accelerators: scene is null");
            return;
        }

        // Store for tracking key repeats
        final Map<KeyCombination, String> actionByKeyCombination = new HashMap<>();
        final Map<String, String> configuredActionBindings = settings.getApplicationSettings().getKeyBindings();

        if (configuredActionBindings == null || configuredActionBindings.isEmpty()) {
            logger.debug("No keyboard bindings configured");
            return;
        }

        // Parse all bindings
        for (Map.Entry<String, String> configuredBinding : configuredActionBindings.entrySet()) {
            String actionId = configuredBinding.getKey();
            String bindingStr = configuredBinding.getValue();

            if (bindingStr == null || bindingStr.isEmpty()) {
                continue;
            }

            try {
                KeyCombination binding = KeyBindingService.parseBinding(bindingStr);
                String existingActionId = actionByKeyCombination.get(binding);
                if (existingActionId != null) {
                    logger.warn("Duplicate keyboard shortcut {} for action {} conflicts with existing action {}; ignoring duplicate binding",
                            bindingStr, actionId, existingActionId);
                    continue;
                }
                actionByKeyCombination.put(binding, actionId);
                logger.debug("Registered keyboard shortcut for {}: {}", actionId, bindingStr);
            } catch (IllegalArgumentException e) {
                logger.error("Failed to parse keyboard binding for {}: {} ({})", actionId, bindingStr, e.getMessage());
            }
        }

        if (actionByKeyCombination.isEmpty()) {
            logger.warn("No valid keyboard bindings were registered");
            return;
        }

        // Remove any previously registered accelerators to avoid duplicates
        try {
            for (KeyCombination keyCombination : registeredAccelerators) {
                keyboardAccelScene.getAccelerators().remove(keyCombination);
            }
            logger.debug("Cleared {} previously registered keyboard accelerators", registeredAccelerators.size());
        } catch (Exception ex) {
            logger.warn("Error clearing previous accelerators: {}", ex.getMessage());
        }
        registeredAccelerators.clear();

        // Register global accelerators on the scene so they work regardless of focused node
        for (Map.Entry<KeyCombination, String> actionEntry : actionByKeyCombination.entrySet()) {
            KeyCombination keyCombination = actionEntry.getKey();
            String actionId = actionEntry.getValue();
            keyboardAccelScene.getAccelerators().put(keyCombination, () -> handleAcceleratorAction(actionId));
            registeredAccelerators.add(keyCombination);
            logger.debug("Registered accelerator for action '{}' -> {}", actionId, keyCombination.getName());
        }

        // Also add a capturing-level event filter so we can detect combos before
        // individual controls consume the key event (useful for global shortcuts).
        // Remove previous filter if present.
        try {
            if (acceleratorEventFilter != null) {
                keyboardAccelScene.removeEventFilter(KeyEvent.KEY_PRESSED, acceleratorEventFilter);
            }
        } catch (Exception ex) {
            // ignore
        }

        acceleratorEventFilter = keyEvent -> {
            // Ignore if target is a text input control to not interfere with typing
            if (keyEvent.getTarget() instanceof TextInputControl) {
                return;
            }

            // Check bindings manually (capture phase) and handle matching action
            for (Map.Entry<KeyCombination, String> actionEntry : actionByKeyCombination.entrySet()) {
                try {
                    if (KeyBindingService.matchesKeyEvent(actionEntry.getKey(), keyEvent)) {
                        // Found a match - handle it (debounce applied inside)
                        handleAcceleratorAction(actionEntry.getValue());
                        keyEvent.consume();
                        return;
                    }
                } catch (Exception ex) {
                    // continue checking others
                }
            }
        };

        keyboardAccelScene.addEventFilter(KeyEvent.KEY_PRESSED, acceleratorEventFilter);
    }

    // Old key-press dispatch removed. Global accelerators use
    // `Scene.getAccelerators()` + a capturing event filter and
    // `handleAcceleratorAction()` to manage per-action debouncing.

    /**
     * Executes the action associated with a keyboard shortcut.
     */
    private void executeKeyboardAction(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }

        logger.debug("Executing keyboard action: {}", actionId);

        try {
            switch (actionId) {
                case "go":
                    onGoClicked();
                    break;
                case "pause":
                    onPauseResumeToggle();
                    break;
                case "stop":
                    onStopClicked();
                    break;
                case "next":
                    selectNextCue();
                    break;
                case "previous":
                    selectPreviousCue();
                    break;
                case "add":
                    addNewCue();
                    break;
                case "deleteSelected":
                    deleteSelectedCue();
                    break;
                case "duplicateSelected":
                    handleDuplicateSelectedCues();
                    break;
                default:
                    logger.warn("Unknown keyboard action: {}", actionId);
            }
        } catch (Exception e) {
            logger.error("Error executing keyboard action {}: {}", actionId, e.getMessage(), e);
        }
    }

    private void selectNextCue() {
        if (playlist == null || playlist.size() == 0) {
            return;
        }
        int idx = cueTable.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            cueTable.getSelectionModel().select(0);
            cueTable.scrollTo(0);
            return;
        }
        if (idx < playlist.size() - 1) {
            cueTable.getSelectionModel().select(idx + 1);
            cueTable.scrollTo(idx + 1);
        }
    }

    private void selectPreviousCue() {
        if (playlist == null || playlist.size() == 0) {
            return;
        }
        int idx = cueTable.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            int last = playlist.size() - 1;
            cueTable.getSelectionModel().select(last);
            cueTable.scrollTo(last);
            return;
        }
        if (idx > 0) {
            cueTable.getSelectionModel().select(idx - 1);
            cueTable.scrollTo(idx - 1);
        }
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
        Scene keyboardAccelScene = new Scene(root);
        setScene(keyboardAccelScene);
        
        // Register keyboard shortcuts
        registerKeyboardAccelerators(keyboardAccelScene);
        
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
        Menu editMenu = new Menu("Edit");
        // editMenu.setDisable(true); // Disable until functional
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
        cueTable.setPlaceholder(new Label("No cues in playlist"));
        cueTable.setEditable(true);
        cueTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cueTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        setupColumns();

        cueTable.setRowFactory(createRowFactory());

        return cueTable;
    }

    /**
     * Set up table columns for the cue table.
     */
    private void setupColumns() {
        // Number column
        TableColumn<Cue, Integer> numberCol = new TableColumn<>("No.");
        numberCol.setId("number");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        numberCol.setPrefWidth(50);

        // Name column (editable inline)
        TableColumn<Cue, String> nameCol = new TableColumn<>("Name");
        nameCol.setId("name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(evt -> {
            Cue cue = evt.getRowValue();
            if (cue != null && evt.getNewValue() != null) {
                cue.setName(evt.getNewValue());
                updateStatus("Renamed cue to: " + evt.getNewValue());
            }
        });

        // Duration column
        TableColumn<Cue, Double> durationCol = new TableColumn<>("Duration");
        durationCol.setId("duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setPrefWidth(80);
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
        preWaitCol.setId("preWait");
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
        postWaitCol.setId("postWait");
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
        autoFollowCol.setId("autoFollow");
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

        // File path column - show filename with tooltip for full path
        TableColumn<Cue, String> fileCol = new TableColumn<>("File");
        fileCol.setId("filePath");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        fileCol.setPrefWidth(300);
        fileCol.setCellFactory(col -> new TableCell<Cue, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    try {
                        String fileName = Paths.get(item).getFileName().toString();
                        setText(fileName);
                        Tooltip tip = new Tooltip(item);
                        setTooltip(tip);
                    } catch (Exception ex) {
                        setText(item);
                        setTooltip(new Tooltip(item));
                    }
                }
            }
        });

        cueTable.getColumns().setAll(List.of(numberCol, nameCol, durationCol, preWaitCol, postWaitCol, autoFollowCol, fileCol));

        applyCueTableLayoutFromSettings();
    }

    /**
     * Captures the current cue table layout into playlist settings.
     */
    private void captureCueTableLayoutToSettings() {
        if (cueTable == null || playlistSettings == null) {
            return;
        }

        StringBuilder order = new StringBuilder();
        StringBuilder widths = new StringBuilder();
        boolean first = true;

        for (TableColumn<Cue, ?> column : cueTable.getColumns()) {
            String columnId = column.getId();
            if (columnId == null || columnId.isEmpty()) {
                continue;
            }

            if (!first) {
                order.append(',');
                widths.append(',');
            }

            order.append(columnId);
            widths.append(columnId).append('=').append(column.getWidth());
            first = false;
        }

        playlistSettings.setCueTableColumnOrder(order.toString());
        playlistSettings.setCueTableColumnWidths(widths.toString());
    }

    /**
     * Applies the saved cue table layout from playlist settings.
     */
    private void applyCueTableLayoutFromSettings() {
        if (cueTable == null || playlistSettings == null) {
            return;
        }

        String savedOrder = playlistSettings.getCueTableColumnOrder();
        String savedWidths = playlistSettings.getCueTableColumnWidths();
        if ((savedOrder == null || savedOrder.isEmpty()) && (savedWidths == null || savedWidths.isEmpty())) {
            return;
        }

        Map<String, TableColumn<Cue, ?>> columnsById = new HashMap<>();
        for (TableColumn<Cue, ?> column : cueTable.getColumns()) {
            if (column.getId() != null) {
                columnsById.put(column.getId(), column);
            }
        }

        if (savedWidths != null && !savedWidths.isEmpty()) {
            for (String entry : savedWidths.split(",")) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                TableColumn<Cue, ?> column = columnsById.get(parts[0]);
                if (column != null) {
                    try {
                        column.setPrefWidth(Double.parseDouble(parts[1]));
                    } catch (NumberFormatException ex) {
                        logger.debug("Ignoring invalid stored width for column {}", parts[0]);
                    }
                }
            }
        }

        if (savedOrder != null && !savedOrder.isEmpty()) {
            List<TableColumn<Cue, ?>> orderedColumns = new ArrayList<>();
            for (String id : savedOrder.split(",")) {
                TableColumn<Cue, ?> column = columnsById.get(id);
                if (column != null && !orderedColumns.contains(column)) {
                    orderedColumns.add(column);
                }
            }

            for (TableColumn<Cue, ?> column : cueTable.getColumns()) {
                if (!orderedColumns.contains(column)) {
                    orderedColumns.add(column);
                }
            }

            cueTable.getColumns().setAll(orderedColumns);
        }
    }

    /**
     * Creates a row factory that attaches a context menu and double-click handler.
     */
    private Callback<TableView<Cue>, TableRow<Cue>> createRowFactory() {
        return table -> {
            TableRow<Cue> row = new TableRow<>();
            ContextMenu rowMenu = new ContextMenu();

            MenuItem playItem = new MenuItem("Play");
            playItem.setOnAction(e -> handlePlayCue(row.getItem()));

            MenuItem changeFileItem = new MenuItem("Change File...");
            changeFileItem.setOnAction(e -> handleChangeFile(row.getItem()));

            MenuItem duplicateSelectedItem = new MenuItem("Duplicate Selected");
            duplicateSelectedItem.setOnAction(e -> handleDuplicateSelectedCues());

            MenuItem openFileItem = new MenuItem("Open File Location");
            openFileItem.setOnAction(e -> handleOpenFileLocation(row.getItem()));

            MenuItem deleteSelectedItem = new MenuItem("Delete Selected");
            deleteSelectedItem.setOnAction(e -> handleDeleteSelectedCues());

            MenuItem propertiesItem = new MenuItem("Properties...");
            propertiesItem.setOnAction(e -> handleEditProperties(row.getItem()));

            MenuItem propertiesSelectedItem = new MenuItem("Edit Selected Properties...");
            propertiesSelectedItem.setOnAction(e -> handleEditSelectedProperties());

            rowMenu.getItems().addAll(
                playItem,
                new SeparatorMenuItem(),
                changeFileItem,
                propertiesItem,
                duplicateSelectedItem,
                propertiesSelectedItem,
                new SeparatorMenuItem(),
                openFileItem,
                new SeparatorMenuItem(),
                deleteSelectedItem
            );

            duplicateSelectedItem.disableProperty().bind(Bindings.size(cueTable.getSelectionModel().getSelectedItems()).lessThan(1));
            deleteSelectedItem.disableProperty().bind(Bindings.size(cueTable.getSelectionModel().getSelectedItems()).lessThan(1));
            propertiesSelectedItem.disableProperty().bind(Bindings.size(cueTable.getSelectionModel().getSelectedItems()).lessThan(1));

            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(rowMenu));

            row.setOnContextMenuRequested(event -> {
                if (row.isEmpty()) return;
                if (!row.isSelected()) {
                    cueTable.getSelectionModel().clearAndSelect(row.getIndex());
                }
            });

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handlePlayCue(row.getItem());
                }
            });

            return row;
        };
    }

    // Action handlers extracted for easier testing and separation
    private void handlePlayCue(Cue cue) {
        if (cue == null) return;
        cueTable.getSelectionModel().select(cue);
        cueController.play(cue);
        updateStatus("Playing: " + cue.getName());
    }

    private void handleChangeFile(Cue cue) {
        if (cue == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Audio File");
        File file = chooser.showOpenDialog(this);
        if (file != null) {
            boolean ok = cueController.changeFile(cue, file.toPath());
            if (ok) {
                updateStatus("Changed file for cue: " + cue.getName());
            } else {
                showError("Invalid File", "Selected file is not a supported audio file.");
            }
        }
    }

    private void handleEditProperties(Cue cue) {
        if (cue == null) return;
        List<Cue> single = List.of(cue);
        PropertiesResult res = showCuePropertiesDialog(single);
        if (res == null) return; // cancelled

        applyPropertiesToCue(cue, res);
        updateStatus("Updated properties for: " + cue.getName());
    }

    private void handleEditSelectedProperties() {
        List<Cue> selected = getSelectedCuesSnapshot();
        if (selected.isEmpty()) return;
        PropertiesResult res = showCuePropertiesDialog(selected);
        if (res == null) return;

        for (Cue cue : selected) {
            applyPropertiesToCue(cue, res);
        }
        updateStatus("Applied properties to " + selected.size() + " selected cue(s)");
    }

    /**
     * Shows a dialog allowing editing of cue properties. For multi-edit, if all selected cues share
     * the same value for a property it will be pre-filled; otherwise left blank (skipped).
     * Returns null if dialog was cancelled.
     */
    private PropertiesResult showCuePropertiesDialog(List<Cue> targetCues) {
        Dialog<PropertiesResult> dialog = new Dialog<>();
        dialog.setTitle(targetCues.size() == 1 ? "Cue Properties" : "Edit Properties for Selected Cues");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyThemeToDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        Label durationLabel = new Label("Duration (s):");
        TextField durationField = new TextField();
        grid.add(durationLabel, 0, 1);
        grid.add(durationField, 1, 1);

        Label preLabel = new Label("Pre-Wait (s):");
        TextField preField = new TextField();
        grid.add(preLabel, 0, 2);
        grid.add(preField, 1, 2);

        Label postLabel = new Label("Post-Wait (s):");
        TextField postField = new TextField();
        grid.add(postLabel, 0, 3);
        grid.add(postField, 1, 3);

        Label autoLabel = new Label("Auto-Follow:");
        CheckBox autoBox = new CheckBox();
        grid.add(autoLabel, 0, 4);
        grid.add(autoBox, 1, 4);

        Label fileLabel = new Label("File:");
        TextField fileField = new TextField();
        fileField.setEditable(false);
        Button chooseFileBtn = new Button("Choose...");
        chooseFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Audio File");
            File selectedFile = chooser.showOpenDialog(this);
            if (selectedFile != null) {
                fileField.setText(selectedFile.toPath().toString());
            }
        });
        grid.add(fileLabel, 0, 5);
        grid.add(fileField, 1, 5);
        grid.add(chooseFileBtn, 2, 5);

        // Pre-fill fields when possible
        if (targetCues.size() == 1) {
            Cue c = targetCues.get(0);
            nameField.setText(c.getName());
            durationField.setText(String.valueOf(c.getDuration()));
            preField.setText(String.valueOf(c.getPreWait()));
            postField.setText(String.valueOf(c.getPostWait()));
            autoBox.setSelected(c.isAutoFollow());
            fileField.setText(c.getFilePath());
        } else if (!targetCues.isEmpty()) {
            // For multi-edit, only prefill if all equal
            Cue first = targetCues.get(0);
            boolean allSameName = targetCues.stream().allMatch(x -> x.getName().equals(first.getName()));
            if (allSameName) nameField.setText(first.getName());

            boolean allSameDuration = targetCues.stream().allMatch(x -> Double.compare(x.getDuration(), first.getDuration()) == 0);
            if (allSameDuration) durationField.setText(String.valueOf(first.getDuration()));

            boolean allSamePre = targetCues.stream().allMatch(x -> Double.compare(x.getPreWait(), first.getPreWait()) == 0);
            if (allSamePre) preField.setText(String.valueOf(first.getPreWait()));

            boolean allSamePost = targetCues.stream().allMatch(x -> Double.compare(x.getPostWait(), first.getPostWait()) == 0);
            if (allSamePost) postField.setText(String.valueOf(first.getPostWait()));

            boolean allSameAuto = targetCues.stream().allMatch(x -> x.isAutoFollow() == first.isAutoFollow());
            if (allSameAuto) autoBox.setSelected(first.isAutoFollow());

            boolean allSameFile = targetCues.stream().allMatch(x -> x.getFilePath().equals(first.getFilePath()));
            if (allSameFile) fileField.setText(first.getFilePath());
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                PropertiesResult r = new PropertiesResult();
                r.name = nameField.getText() != null && !nameField.getText().isEmpty() ? nameField.getText() : null;
                try {
                    r.duration = durationField.getText() != null && !durationField.getText().isEmpty() ? Double.parseDouble(durationField.getText()) : null;
                    if (r.duration != null && r.duration < 0) {
                        showError("Invalid value", "Duration must be zero or greater");
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    showError("Invalid value", "Duration must be a number");
                    return null;
                }
                try {
                    r.preWait = preField.getText() != null && !preField.getText().isEmpty() ? Double.parseDouble(preField.getText()) : null;
                    if (r.preWait != null && r.preWait < 0) {
                        showError("Invalid value", "Pre-Wait must be zero or greater");
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    showError("Invalid value", "Pre-Wait must be a number");
                    return null;
                }
                try {
                    r.postWait = postField.getText() != null && !postField.getText().isEmpty() ? Double.parseDouble(postField.getText()) : null;
                    if (r.postWait != null && r.postWait < 0) {
                        showError("Invalid value", "Post-Wait must be zero or greater");
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    showError("Invalid value", "Post-Wait must be a number");
                    return null;
                }
                r.autoFollow = autoBox.isSelected();
                r.filePath = fileField.getText() != null && !fileField.getText().isEmpty() ? fileField.getText() : null;
                return r;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static class PropertiesResult {
        String name;
        Double duration;
        Double preWait;
        Double postWait;
        Boolean autoFollow;
        String filePath;
    }

    private void applyPropertiesToCue(Cue cue, PropertiesResult res) {
        if (cue == null || res == null) return;

        if (res.name != null) cue.setName(res.name);
        if (res.duration != null) cue.setDuration(res.duration);
        if (res.preWait != null) cue.setPreWait(res.preWait);
        if (res.postWait != null) cue.setPostWait(res.postWait);
        if (res.autoFollow != null) cue.setAutoFollow(res.autoFollow);
        if (res.filePath != null) cue.setFilePath(res.filePath);
    }

    private void handleDuplicateSelectedCues() {
        List<Cue> selectedCues = getSelectedCuesSnapshot();
        if (selectedCues.isEmpty()) {
            return;
        }

        List<Cue> duplicates = cueController.duplicateSelected(playlist, selectedCues);
        if (!duplicates.isEmpty()) {
            updateCueCount();
            cueTable.getSelectionModel().clearSelection();
            for (Cue cue : duplicates) {
                cueTable.getSelectionModel().select(cue);
            }
            updateStatus("Duplicated " + duplicates.size() + " selected cues");
        }
    }

    private void handleDeleteSelectedCues() {
        List<Cue> selectedCues = getSelectedCuesSnapshot();
        if (selectedCues.isEmpty()) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Selected Cues");
        confirm.setHeaderText("Delete " + selectedCues.size() + " selected cue(s)?");
        confirm.setContentText("This will remove all currently selected cues from the playlist.");
        applyThemeToDialog(confirm);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                cueController.deleteSelected(playlist, selectedCues);
                updateCueCount();
                cueTable.getSelectionModel().clearSelection();
                updateStatus("Deleted " + selectedCues.size() + " selected cue(s)");
            }
        });
    }

    private List<Cue> getSelectedCuesSnapshot() {
        return new ArrayList<>(cueTable.getSelectionModel().getSelectedItems());
    }

    private void handleOpenFileLocation(Cue cue) {
        if (cue == null) return;
        String fp = cue.getFilePath();
        if (fp != null && !fp.isEmpty()) {
            try {
                cueController.openFileLocation(cue);
            } catch (UnsupportedOperationException ex) {
                showError("Unsupported", "Desktop operations are not supported on this platform.");
            } catch (Exception ex) {
                showError("Failed to open file location", ex.getMessage());
            }
        }
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
        Cue newCue = cueController.createNewCue(playlist);
        if (newCue == null) {
            return;
        }
        updateCueCount();
        updateStatus("Added new cue");
        logger.debug("Added new cue with number {}", newCue.getNumber());
    }
    
    /**
     * Deletes the selected cue from the playlist.
     */
    private void deleteSelectedCue() {
        List<Cue> selectedCues = getSelectedCuesSnapshot();
        if (selectedCues.isEmpty()) {
            return;
        }

        if (selectedCues.size() > 1) {
            handleDeleteSelectedCues();
            return;
        }

        Cue selectedCue = selectedCues.get(0);
        cueController.delete(playlist, selectedCue);
        updateCueCount();
        cueTable.getSelectionModel().clearSelection();
        updateStatus("Deleted cue: " + selectedCue.getName());
    }
    
    /**
     * Handles GO button click.
     * Plays the currently selected cue and advances selection to next cue.
     */
    private void onGoClicked() {
        Cue selectedCue = cueTable.getSelectionModel().getSelectedItem();
        if (selectedCue != null) {
            // Play the selected cue
            cueController.play(selectedCue);
            
            // Immediately advance to next cue
            Cue nextCue = cueController.getNextCue(playlist, selectedCue);
            if (nextCue != null) {
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
        Cue nextCue = cueController.getNextCue(playlist, currentCue);
        if (nextCue != null) {
            cueTable.getSelectionModel().select(nextCue);
            cueController.play(nextCue);
        }
    }
    
    /**
     * Handles state changes from the audio controller.
     */
    private void handleStateChange(PlaybackState state) {
        switch (state) {
            case PLAYING -> {
                // GO button stays enabled for multi-track
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
            }
            case PAUSED -> {
                pauseButton.setText("Resume");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
            }
            case PRE_WAIT -> {
                // During pre-wait, allow pausing the timer
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
            }
            case POST_WAIT -> {
                // During post-wait, allow pausing the timer
                pauseButton.setText("Pause");
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
            }
            case STOPPED -> {
                // GO button stays enabled for multi-track
                pauseButton.setText("Pause");
                pauseButton.setDisable(true);
                stopButton.setDisable(true);
            }
        }
    }
    
    /**
     * Handles double-click on file browser.
     */
    private void handleFileDoubleClick() {
        Path selectedPath = fileView.getSelectedPath();
        if (selectedPath != null && PathUtil.isAudioFile(selectedPath)) {
            Cue newCue = cueController.createCueFromFile(playlist, selectedPath);
            if (newCue != null) {
                updateCueCount();
                updateStatus("Added cue: " + PathUtil.getFileNameWithoutExtension(selectedPath));
            }
        }
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
                applyCueTableLayoutFromSettings();
                
                // Add to recent files
                settings.addRecentFile(filePath.toString());
                try {
                    settingsService.save(settings);
                } catch (IOException ex) {
                    logger.error("Failed to save recent file to settings", ex);
                }
                
                updateCueCount();
                updateStatus("Loaded playlist: " + loadedPlaylist.getName());
            } catch (IOException e) {
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
                captureCueTableLayoutToSettings();
                playlistService.save(playlist, playlist.getFilePath());
                playlistSettingsService.save(currentPlaylistPath, playlistSettings);
                updateStatus("Playlist saved");
            } catch (IOException e) {
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
                captureCueTableLayoutToSettings();
                playlistService.save(playlist, filePath.toString());
                playlistSettings.setPlaylistName(playlist.getName());
                playlistSettingsService.save(filePath, playlistSettings);
                currentPlaylistPath = filePath;
                playlist.setFilePath(filePath.toString());
                updateStatus("Playlist saved as: " + file.getName());
            } catch (IOException e) {
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
            } catch (IOException e) {
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
                    
                    RecentPlaylist singlePlaylist = new RecentPlaylist(playlistName, filePath);
                    singlePlaylist.setIsPinned(true);
                    allPlaylists.add(singlePlaylist);
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
                    
                    RecentPlaylist SinglePlaylist = new RecentPlaylist(playlistName, filePath);
                    SinglePlaylist.setIsPinned(false);
                    allPlaylists.add(SinglePlaylist);
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
        Scene currentScene = getScene();
        currentScene.getStylesheets().clear();
        currentScene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());
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
            this.playlistService = new PlaylistService();
            playlist.clear();
            Playlist loadedPlaylist = playlistService.load(filePath);
            for (Cue cue : loadedPlaylist.getCues()) {
                playlist.addCue(cue);
            }
            playlistInitialized = true;
            updateStatus("Opened playlist: " + filePath);
        } catch (IOException e) {
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
                    } catch (IOException ex) {
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
            } catch (IOException ex) {
                logger.error("Failed to save recent file to settings", ex);
            }
            
            updateCueCount();
            updateStatus("Loaded playlist: " + loadedPlaylist.getName());
            logger.info("Opened recent playlist: {}", filePath);
        } catch (IOException e) {
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
            } catch (IOException e) {
                logger.error("Failed to save settings after clearing recent files", e);
                showError("Failed to clear recent files", e.getMessage());
            }
        }
    }
}
