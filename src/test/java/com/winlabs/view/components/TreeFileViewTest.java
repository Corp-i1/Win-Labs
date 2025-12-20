package com.winlabs.view.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TreeFileView component.
 * Note: These tests verify the component structure without running the JavaFX application.
 */
public class TreeFileViewTest {
    
    @Test
    public void testTreeFileViewClassExists() {
        // Verify the class exists and can be referenced
        Class<?> clazz = TreeFileView.class;
        assertNotNull(clazz, "TreeFileView class should exist");
        assertEquals("TreeFileView", clazz.getSimpleName());
    }
    
    @Test
    public void testTreeFileViewInheritsFromTreeView() {
        // Verify it extends TreeView
        assertTrue(javafx.scene.control.TreeView.class.isAssignableFrom(TreeFileView.class),
            "TreeFileView should extend TreeView");
    }
    
    @Test
    public void testTreeFileViewHasRequiredMethods() throws Exception {
        // Verify the required methods exist
        assertNotNull(TreeFileView.class.getMethod("getSelectedPath"),
            "getSelectedPath method should exist");
    }
}
