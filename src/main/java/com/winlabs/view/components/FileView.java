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
 * File view component that can toggle between tree view and browser view.
 * Tree view provides hierarchical navigation, while browser view shows a flat list.
 */
public class FileView extends BorderPane {
    
    private final FileSystemService fileSystemService;
    private final FileTreeView fileTreeView;
    private final ListView<Path> fileBrowserView;
    private final ToggleButton viewToggle;
    private boolean isTreeView = true;
    
    public FileView() {
        this.fileSystemService = new FileSystemService();
        this.fileTreeView = new FileTreeView();
        this.fileBrowserView = new ListView<>();
        
        // Setup browser view
        setupBrowserView();
        
        // Create toggle button
        viewToggle = new ToggleButton("Browser View");
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
     * Sets up the browser view with custom cell factory.
     */
    private void setupBrowserView() {
        fileBrowserView.setCellFactory(lv -> new ListCell<Path>() {
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
     * Toggles between tree view and browser view.
     */
    private void toggleView() {
        isTreeView = !isTreeView;
        
        if (isTreeView) {
            setCenter(fileTreeView);
            viewToggle.setText("Browser View");
            viewToggle.setSelected(false);
        } else {
            loadBrowserView();
            setCenter(fileBrowserView);
            viewToggle.setText("Tree View");
            viewToggle.setSelected(true);
        }
    }
    
    /**
     * Loads files into the browser view.
     * Shows files from common locations (home and music directories).
     */
    private void loadBrowserView() {
        fileBrowserView.getItems().clear();
        
        // Load from music directory if it exists
        Path musicDir = fileSystemService.getMusicDirectory();
        if (Files.exists(musicDir)) {
            try {
                List<Path> files = fileSystemService.listAudioFilesRecursive(musicDir);
                fileBrowserView.getItems().addAll(files);
            } catch (IOException e) {
                System.err.println("Failed to load music directory: " + e.getMessage());
            }
        }
        
        // If no files found, try home directory
        if (fileBrowserView.getItems().isEmpty()) {
            Path homeDir = fileSystemService.getHomeDirectory();
            if (Files.exists(homeDir)) {
                try {
                    List<Path> files = fileSystemService.listAudioFilesRecursive(homeDir);
                    fileBrowserView.getItems().addAll(files);
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
            return fileBrowserView.getSelectionModel().getSelectedItem();
        }
    }
    
    /**
     * Gets the internal tree view component.
     */
    public FileTreeView getTreeView() {
        return fileTreeView;
    }
    
    /**
     * Gets the internal browser view component.
     */
    public ListView<Path> getBrowserView() {
        return fileBrowserView;
    }
}
