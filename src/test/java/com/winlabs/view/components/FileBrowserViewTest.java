package com.winlabs.view.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileBrowserView component.
 * Note: These tests verify the component structure without running the JavaFX application.
 */
public class FileBrowserViewTest {
    
    @Test
    public void testFileBrowserViewClassExists() {
        // Verify the class exists and can be referenced
        Class<?> clazz = FileBrowserView.class;
        assertNotNull(clazz, "FileBrowserView class should exist");
        assertEquals("FileBrowserView", clazz.getSimpleName());
    }
    
    @Test
    public void testFileBrowserViewInheritsFromBorderPane() {
        // Verify it extends BorderPane
        assertTrue(javafx.scene.layout.BorderPane.class.isAssignableFrom(FileBrowserView.class),
            "FileBrowserView should extend BorderPane");
    }
    
    @Test
    public void testFileBrowserViewHasRequiredMethods() throws Exception {
        // Verify the required methods exist
        assertNotNull(FileBrowserView.class.getMethod("getSelectedPath"),
            "getSelectedPath method should exist");
        assertNotNull(FileBrowserView.class.getMethod("getTreeView"),
            "getTreeView method should exist");
        assertNotNull(FileBrowserView.class.getMethod("getListView"),
            "getListView method should exist");
    }
}
