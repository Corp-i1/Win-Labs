package com.winlabs.view.components;

import com.winlabs.service.FileSystemService;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Browser view component for displaying files in a flat list.
 * Shows audio files from common locations (home and music directories).
 */
public class BrowserFileView extends ListView<Path> {
    
    private final FileSystemService fileSystemService;
    
    public BrowserFileView(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
        
        // Setup custom cell factory
        setCellFactory(lv -> new BrowserFileCell());
    }
    
    /**
     * Loads audio files into the browser view.
     * Shows files from common locations (home and music directories).
     */
    public void loadFiles() {
        getItems().clear();
        
        // Load from music directory if it exists
        Path musicDir = fileSystemService.getMusicDirectory();
        if (Files.exists(musicDir)) {
            try {
                List<Path> files = fileSystemService.listAudioFilesRecursive(musicDir);
                getItems().addAll(files);
            } catch (IOException e) {
                System.err.println("Failed to load music directory: " + e.getMessage());
            }
        }
        
        // If no files found, try home directory
        if (getItems().isEmpty()) {
            Path homeDir = fileSystemService.getHomeDirectory();
            if (Files.exists(homeDir)) {
                try {
                    List<Path> files = fileSystemService.listAudioFilesRecursive(homeDir);
                    getItems().addAll(files);
                } catch (IOException e) {
                    System.err.println("Failed to load home directory: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets the selected file path.
     */
    public Path getSelectedPath() {
        return getSelectionModel().getSelectedItem();
    }
    
    /**
     * Custom cell renderer for browser view.
     * Uses shared display logic from FileView.
     */
    private static class BrowserFileCell extends ListCell<Path> {
        @Override
        protected void updateItem(Path path, boolean empty) {
            super.updateItem(path, empty);
            
            if (empty || path == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(FileView.getDisplayName(path));
                setStyle(FileView.getPathStyle(path));
            }
        }
    }
}
