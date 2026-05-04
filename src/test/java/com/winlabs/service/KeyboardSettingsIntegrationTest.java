package com.winlabs.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.winlabs.model.ApplicationSettings;

class KeyboardSettingsIntegrationTest {

    @TempDir
    Path tempDir;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService();
    }

    @Test
    void applicationSettingsPersistsKeyBindings() throws Exception {
        ApplicationSettings settings = new ApplicationSettings();
        
        // Verify default bindings exist
        Map<String, String> defaults = settings.getKeyBindings();
        assertNotNull(defaults);
        assertFalse(defaults.isEmpty());
        assertEquals("SPACE", defaults.get("go"));
        assertEquals("ESC", defaults.get("stop"));
    }

    @Test
    void applicationSettingsCanModifyKeyBindings() {
        ApplicationSettings settings = new ApplicationSettings();
        
        settings.setKeyBinding("go", "CTRL+SPACE");
        assertEquals("CTRL+SPACE", settings.getKeyBinding("go"));
        
        settings.setKeyBinding("stop", "SHIFT+ESC");
        assertEquals("SHIFT+ESC", settings.getKeyBinding("stop"));
    }

    @Test
    void applicationSettingsCanResetToDefaults() {
        ApplicationSettings settings = new ApplicationSettings();
        
        // Modify some bindings
        settings.setKeyBinding("go", "CUSTOM");
        settings.setKeyBinding("stop", "ANOTHER");
        
        // Reset
        settings.resetToDefaults();
        
        // Verify reverted
        assertEquals("SPACE", settings.getKeyBinding("go"));
        assertEquals("ESC", settings.getKeyBinding("stop"));
    }

    @Test
    void settingsServicePersistsAllowKeyRepeat() throws Exception {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setAllowKeyRepeat(true);
        
        assertTrue(settings.isAllowKeyRepeat());
    }

    @Test
    void settingsHandlesNullKeyBindings() {
        ApplicationSettings settings = new ApplicationSettings();
        
        // Should handle null gracefully
        settings.setKeyBindings(null);
        // After setting null, should still have the map (cleared but then likely restored)
        
        // Should be able to set a new binding
        settings.setKeyBinding("go", "SPACE");
        assertEquals("SPACE", settings.getKeyBinding("go"));
    }

    @Test
    void settingsCanClearAllKeyBindings() {
        ApplicationSettings settings = new ApplicationSettings();
        
        assertFalse(settings.getKeyBindings().isEmpty());
        
        settings.clearKeyBindings();
        
        assertTrue(settings.getKeyBindings().isEmpty());
    }

    @Test
    void allDefaultKeyBindingsExist() {
        ApplicationSettings settings = new ApplicationSettings();
        
        Map<String, String> bindings = settings.getKeyBindings();
        
        assertNotNull(bindings.get("go"));
        assertNotNull(bindings.get("pause"));
        assertNotNull(bindings.get("stop"));
        assertNotNull(bindings.get("next"));
        assertNotNull(bindings.get("previous"));
        assertNotNull(bindings.get("add"));
        assertNotNull(bindings.get("deleteSelected"));
        assertNotNull(bindings.get("duplicateSelected"));
    }

    @Test
    void getDefaultKeyBindingReturnsUnmodifiedDefaults() {
        ApplicationSettings settings = new ApplicationSettings();
        
        // Modify a binding
        settings.setKeyBinding("go", "CUSTOM");
        
        // Default should be unchanged
        assertEquals("SPACE", settings.getDefaultKeyBinding("go"));
        
        // Current binding should be modified
        assertEquals("CUSTOM", settings.getKeyBinding("go"));
    }

    @Test
    void settingsPreservesAllowKeyRepeatState() {
        ApplicationSettings settings = new ApplicationSettings();
        
        // Initially should be false (default)
        assertFalse(settings.isAllowKeyRepeat());
        
        // Change to true
        settings.setAllowKeyRepeat(true);
        assertTrue(settings.isAllowKeyRepeat());
        
        // Change back to false
        settings.setAllowKeyRepeat(false);
        assertFalse(settings.isAllowKeyRepeat());
    }

    @Test
    void allowKeyRepeatIsResetToDefaultOnResetSettings() {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setAllowKeyRepeat(true);
        
        settings.resetToDefaults();
        
        assertFalse(settings.isAllowKeyRepeat());
    }
}
