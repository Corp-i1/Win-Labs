package com.winlabs.view.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileView component.
 * Note: These tests verify the component structure without running the JavaFX application.
 */
public class FileViewTest {
    
    @Test
    public void testFileViewClassExists() {
        // Verify the class exists and can be referenced
        Class<?> clazz = FileView.class;
        assertNotNull(clazz, "FileView class should exist");
        assertEquals("FileView", clazz.getSimpleName());
    }
    
    @Test
    public void testFileViewInheritsFromBorderPane() {
        // Verify it extends BorderPane
        assertTrue(javafx.scene.layout.BorderPane.class.isAssignableFrom(FileView.class),
            "FileView should extend BorderPane");
    }
    
    @Test
    public void testFileViewHasRequiredMethods() throws Exception {
        // Verify the required methods exist
        assertNotNull(FileView.class.getMethod("getSelectedPath"),
            "getSelectedPath method should exist");
        assertNotNull(FileView.class.getMethod("getTreeFileView"),
            "getTreeFileView method should exist");
        assertNotNull(FileView.class.getMethod("getBrowserFileView"),
            "getBrowserFileView method should exist");
    }
}
