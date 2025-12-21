package com.winlabs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Settings model.
 */
class SettingsTest {
    
    private Settings settings;
    
    @BeforeEach
    void setUp() {
        settings = new Settings();
    }
    
    @Test
    void testDefaultValues() {
        assertEquals("dark", settings.getTheme());
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        assertTrue(settings.isEnableMultiTrackPlayback());
        assertFalse(settings.isAutoSaveEnabled());
        assertEquals(300, settings.getAutoSaveInterval());
    }
    
    @Test
    void testThemeProperty() {
        settings.setTheme("light");
        assertEquals("light", settings.getTheme());
        
        settings.setTheme("rainbow");
        assertEquals("rainbow", settings.getTheme());
    }
    
    @Test
    void testMasterVolumeProperty() {
        settings.setMasterVolume(0.5);
        assertEquals(0.5, settings.getMasterVolume(), 0.001);
        
        // Test clamping to 0.0-1.0 range
        settings.setMasterVolume(1.5);
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        
        settings.setMasterVolume(-0.5);
        assertEquals(0.0, settings.getMasterVolume(), 0.001);
    }
    
    @Test
    void testMultiTrackPlaybackProperty() {
        settings.setEnableMultiTrackPlayback(false);
        assertFalse(settings.isEnableMultiTrackPlayback());
        
        settings.setEnableMultiTrackPlayback(true);
        assertTrue(settings.isEnableMultiTrackPlayback());
    }
    
    @Test
    void testAutoSaveEnabledProperty() {
        settings.setAutoSaveEnabled(true);
        assertTrue(settings.isAutoSaveEnabled());
        
        settings.setAutoSaveEnabled(false);
        assertFalse(settings.isAutoSaveEnabled());
    }
    
    @Test
    void testAutoSaveIntervalProperty() {
        settings.setAutoSaveInterval(600);
        assertEquals(600, settings.getAutoSaveInterval());
        
        // Test minimum value (60 seconds)
        settings.setAutoSaveInterval(30);
        assertEquals(60, settings.getAutoSaveInterval());
    }
    
    @Test
    void testResetToDefaults() {
        // Change all settings
        settings.setTheme("light");
        settings.setMasterVolume(0.5);
        settings.setEnableMultiTrackPlayback(false);
        settings.setAutoSaveEnabled(true);
        settings.setAutoSaveInterval(600);
        
        // Reset to defaults
        settings.resetToDefaults();
        
        // Verify all settings are back to defaults
        assertEquals("dark", settings.getTheme());
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        assertTrue(settings.isEnableMultiTrackPlayback());
        assertFalse(settings.isAutoSaveEnabled());
        assertEquals(300, settings.getAutoSaveInterval());
    }
    
    @Test
    void testPropertyBindings() {
        // Test that property objects are not null
        assertNotNull(settings.themeProperty());
        assertNotNull(settings.masterVolumeProperty());
        assertNotNull(settings.enableMultiTrackPlaybackProperty());
        assertNotNull(settings.autoSaveEnabledProperty());
        assertNotNull(settings.autoSaveIntervalProperty());
    }
}
