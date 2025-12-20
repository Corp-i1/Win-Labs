package com.winlabs.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.nio.file.Path;

/**
 * File view component that can toggle between tree view and browser view.
 * Tree view provides hierarchical navigation, while browser view shows a flat list.
 */
public class FileView extends BorderPane {
    
    private final TreeFileView treeFileView;
    private final BrowserFileView browserFileView;
    private final ToggleButton viewToggle;
    private boolean isTreeView = true;
    
    public FileView() {
        this.treeFileView = new TreeFileView();
        this.browserFileView = new BrowserFileView();
        
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
}

