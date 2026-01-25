package com.winlabs.view.components;

import com.winlabs.service.FileSystemService;
import com.winlabs.util.PathUtil;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//TODO: #68 Make Scope adjustable (e.g., show only audio files, show all files, show only in same directory as cue list, adjust default scope shown)

/**
 * Tree view component for browsing the file system.
 * Shows directories and audio files with lazy loading.
 * Note: This component is now wrapped by FileView which provides browser/tree toggle functionality.
 */
public class TreeFileView extends TreeView<Path> {
    
    private static final Logger logger = LoggerFactory.getLogger(TreeFileView.class);
    private final FileSystemService fileSystemService;
    
    public TreeFileView(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
        
        // Create root node
        TreeItem<Path> rootItem = new TreeItem<>(null);
        rootItem.setExpanded(true);
        setRoot(rootItem);
        setShowRoot(false);
        
        // Load file system roots (drives on Windows, / on Unix)
        loadFileSystemRoots();
        
        // Custom cell factory to display file/folder names
        setCellFactory(tv -> new TreeFileCell());
    }
    
    /**
     * Loads the file system roots into the tree.
     */
    private void loadFileSystemRoots() {
        List<Path> roots = fileSystemService.getFileSystemRoots();
        for (Path root : roots) {
            TreeItem<Path> item = createTreeItem(root);
            getRoot().getChildren().add(item);
        }
        
        // Also add common directories
        Path homeDir = fileSystemService.getHomeDirectory();
        Path musicDir = fileSystemService.getMusicDirectory();
        
        if (Files.exists(homeDir)) {
            TreeItem<Path> homeItem = createTreeItem(homeDir);
            getRoot().getChildren().add(0, homeItem);
        }
        
        if (Files.exists(musicDir)) {
            TreeItem<Path> musicItem = createTreeItem(musicDir);
            getRoot().getChildren().add(1, musicItem);
        }
    }
    
    /**
     * Creates a tree item for a path with lazy loading.
     */
    private TreeItem<Path> createTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        
        // Add a dummy child if it's a directory (enables expand arrow)
        if (Files.isDirectory(path)) {
            item.getChildren().add(new TreeItem<>(null));
        }
        
        // Lazy load children when expanded
        item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded && !item.getChildren().isEmpty() && item.getChildren().get(0).getValue() == null) {
                loadChildren(item);
            }
        });
        
        return item;
    }
    
    /**
     * Loads the children of a tree item.
     */
    private void loadChildren(TreeItem<Path> parentItem) {
        Path parentPath = parentItem.getValue();
        
        // Clear dummy child
        parentItem.getChildren().clear();
        
        try {
            List<Path> children = fileSystemService.listAll(parentPath);
            
            for (Path child : children) {
                // Show all directories and audio files
                if (Files.isDirectory(child) || PathUtil.isAudioFile(child)) {
                    TreeItem<Path> childItem = createTreeItem(child);
                    parentItem.getChildren().add(childItem);
                }
            }
        } catch (IOException e) {
            // Failed to load children (permission issue, etc.)
            logger.error("Failed to load children for {}: {}", parentPath, e.getMessage(), e);
        }
    }
    
    /**
     * Custom tree cell to display file/folder names nicely.
     * Uses shared display logic from FileView.
     */
    private static class TreeFileCell extends TreeCell<Path> {
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
    
    /**
     * Gets the selected file path.
     */
    public Path getSelectedPath() {
        TreeItem<Path> selectedItem = getSelectionModel().getSelectedItem();
        return selectedItem != null ? selectedItem.getValue() : null;
    }
}
