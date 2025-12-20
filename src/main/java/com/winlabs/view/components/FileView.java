package com.winlabs.view.components;

import com.winlabs.service.FileSystemService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File view component that can toggle between tree view and browser view.
 * Tree view provides hierarchical navigation, while browser view shows a flat list.
 * Provides shared functionality for both view types.
 */
public class FileView extends BorderPane {
    
    private final FileSystemService fileSystemService;
    private final TreeFileView treeFileView;
    private final BrowserFileView browserFileView;
    private final ToggleButton viewToggle;
    private boolean isTreeView = true;
    
    public FileView() {
        this.fileSystemService = new FileSystemService();
        this.treeFileView = new TreeFileView(fileSystemService);
        this.browserFileView = new BrowserFileView(fileSystemService);
        
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
        setCenter(treeFileView);
    }
    
    /**
     * Toggles between tree view and browser view.
     */
    private void toggleView() {
        isTreeView = !isTreeView;
        
        if (isTreeView) {
            setCenter(treeFileView);
            viewToggle.setText("Browser View");
            viewToggle.setSelected(false);
        } else {
            browserFileView.loadFiles();
            setCenter(browserFileView);
            viewToggle.setText("Tree View");
            viewToggle.setSelected(true);
        }
    }
    
    /**
     * Gets the selected file path from the current view.
     */
    public Path getSelectedPath() {
        if (isTreeView) {
            return treeFileView.getSelectedPath();
        } else {
            return browserFileView.getSelectedPath();
        }
    }
    
    /**
     * Gets the internal tree file view component.
     */
    public TreeFileView getTreeFileView() {
        return treeFileView;
    }
    
    /**
     * Gets the internal browser file view component.
     */
    public BrowserFileView getBrowserFileView() {
        return browserFileView;
    }
    
    /**
     * Gets the shared file system service.
     */
    public FileSystemService getFileSystemService() {
        return fileSystemService;
    }
    
    /**
     * Gets the display name for a path.
     * Common method used by both TreeFileView and BrowserFileView for rendering.
     */
    public static String getDisplayName(Path path) {
        if (path == null) {
            return "";
        }
        if (path.getFileName() == null) {
            // Root directory (e.g., C:\ or /)
            return path.toString();
        }
        return path.getFileName().toString();
    }
    
    /**
     * Gets the CSS style for a path based on whether it's a directory.
     * Common method used by both TreeFileView and BrowserFileView for rendering.
     */
    public static String getPathStyle(Path path) {
        if (path != null && Files.isDirectory(path)) {
            return "-fx-font-weight: bold;";
        }
        return "-fx-font-weight: normal;";
    }
}

