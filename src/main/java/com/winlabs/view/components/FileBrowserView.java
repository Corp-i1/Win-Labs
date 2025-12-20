package com.winlabs.view.components;

import com.winlabs.service.FileSystemService;
import com.winlabs.util.PathUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * File browser component that can toggle between tree view and list view.
 * Replaces FileTreeView with added list view functionality.
 */
public class FileBrowserView extends BorderPane {
    
    private final FileSystemService fileSystemService;
    private final FileTreeView fileTreeView;
    private final ListView<Path> fileListView;
    private final ToggleButton viewToggle;
    private boolean isTreeView = true;
    
    public FileBrowserView() {
        this.fileSystemService = new FileSystemService();
        this.fileTreeView = new FileTreeView();
        this.fileListView = new ListView<>();
        
        // Setup list view
        setupListView();
        
        // Create toggle button
        viewToggle = new ToggleButton("List View");
        viewToggle.setSelected(false); // Start with tree view
        viewToggle.setOnAction(e -> toggleView());
        
        // Create header with toggle button
        HBox header = new HBox(10);
        header.setPadding(new Insets(5));
        header.setAlignment(Pos.CENTER_RIGHT);
        header.getChildren().add(viewToggle);
        
        // Set initial view
        setTop(header);
        setCenter(fileTreeView);
    }
    
    /**
     * Sets up the list view with custom cell factory.
     */
    private void setupListView() {
        fileListView.setCellFactory(lv -> new ListCell<Path>() {
            @Override
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);
                
                if (empty || path == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String displayName;
                    if (path.getFileName() == null) {
                        displayName = path.toString();
                    } else {
                        displayName = path.getFileName().toString();
                    }
                    
                    setText(displayName);
                    
                    if (Files.isDirectory(path)) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });
    }
    
    /**
     * Toggles between tree view and list view.
     */
    private void toggleView() {
        isTreeView = !isTreeView;
        
        if (isTreeView) {
            setCenter(fileTreeView);
            viewToggle.setText("List View");
            viewToggle.setSelected(false);
        } else {
            loadListView();
            setCenter(fileListView);
            viewToggle.setText("Tree View");
            viewToggle.setSelected(true);
        }
    }
    
    /**
     * Loads files into the list view.
     * Shows files from common locations (home and music directories).
     */
    private void loadListView() {
        fileListView.getItems().clear();
        
        // Load from music directory if it exists
        Path musicDir = fileSystemService.getMusicDirectory();
        if (Files.exists(musicDir)) {
            try {
                List<Path> files = fileSystemService.listAudioFilesRecursive(musicDir);
                fileListView.getItems().addAll(files);
            } catch (IOException e) {
                System.err.println("Failed to load music directory: " + e.getMessage());
            }
        }
        
        // If no files found, try home directory
        if (fileListView.getItems().isEmpty()) {
            Path homeDir = fileSystemService.getHomeDirectory();
            if (Files.exists(homeDir)) {
                try {
                    List<Path> files = fileSystemService.listAudioFilesRecursive(homeDir);
                    fileListView.getItems().addAll(files);
                } catch (IOException e) {
                    System.err.println("Failed to load home directory: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets the selected file path from the current view.
     */
    public Path getSelectedPath() {
        if (isTreeView) {
            return fileTreeView.getSelectedPath();
        } else {
            return fileListView.getSelectionModel().getSelectedItem();
        }
    }
    
    /**
     * Gets the internal tree view component.
     */
    public FileTreeView getTreeView() {
        return fileTreeView;
    }
    
    /**
     * Gets the internal list view component.
     */
    public ListView<Path> getListView() {
        return fileListView;
    }
}
