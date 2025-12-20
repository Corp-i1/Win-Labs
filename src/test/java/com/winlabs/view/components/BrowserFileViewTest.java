package com.winlabs.view.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BrowserFileView component.
 * Note: These tests verify the component structure without running the JavaFX application.
 */
public class BrowserFileViewTest {
    
    @Test
    public void testBrowserFileViewClassExists() {
        // Verify the class exists and can be referenced
        Class<?> clazz = BrowserFileView.class;
        assertNotNull(clazz, "BrowserFileView class should exist");
        assertEquals("BrowserFileView", clazz.getSimpleName());
    }
    
    @Test
    public void testBrowserFileViewInheritsFromListView() {
        // Verify it extends ListView
        assertTrue(javafx.scene.control.ListView.class.isAssignableFrom(BrowserFileView.class),
            "BrowserFileView should extend ListView");
    }
    
    @Test
    public void testBrowserFileViewHasRequiredMethods() throws Exception {
        // Verify the required methods exist
        assertNotNull(BrowserFileView.class.getMethod("getSelectedPath"),
            "getSelectedPath method should exist");
        assertNotNull(BrowserFileView.class.getMethod("loadFiles"),
            "loadFiles method should exist");
    }
}
