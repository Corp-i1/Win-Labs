package com.winlabs.service;

import com.winlabs.model.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SettingsService.
 */
class SettingsServiceTest {
    
    private SettingsService settingsService;
    private Path settingsPath;
    
    @BeforeEach
    void setUp() {
        settingsService = new SettingsService();
        settingsPath = settingsService.getSettingsPath();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up test settings file
        if (Files.exists(settingsPath)) {
            settingsService.deleteSettings();
        }
    }
    
    @Test
    void testSaveAndLoad() throws IOException {
        // Create settings with custom values
        Settings settings = new Settings();
        settings.setTheme("light");
        settings.setMasterVolume(0.75);
        settings.setEnableMultiTrackPlayback(false);
        settings.setAutoSaveEnabled(true);
        settings.setAutoSaveInterval(600);
        
        // Save settings
        settingsService.save(settings);
        
        // Verify file was created
        assertTrue(Files.exists(settingsPath));
        
        // Load settings
        Settings loadedSettings = settingsService.load();
        
        // Verify loaded settings match saved settings
        assertEquals("light", loadedSettings.getTheme());
        assertEquals(0.75, loadedSettings.getMasterVolume(), 0.001);
        assertFalse(loadedSettings.isEnableMultiTrackPlayback());
        assertTrue(loadedSettings.isAutoSaveEnabled());
        assertEquals(600, loadedSettings.getAutoSaveInterval());
    }
    
    @Test
    void testLoadDefaultsWhenFileDoesNotExist() throws IOException {
        // Ensure settings file doesn't exist
        if (Files.exists(settingsPath)) {
            settingsService.deleteSettings();
        }
        
        // Load settings (should return defaults)
        Settings settings = settingsService.load();
        
        // Verify default values
        assertEquals("dark", settings.getTheme());
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        assertTrue(settings.isEnableMultiTrackPlayback());
        assertFalse(settings.isAutoSaveEnabled());
        assertEquals(300, settings.getAutoSaveInterval());
    }
    
    @Test
    void testSaveCreatesFile() throws IOException {
        // Ensure settings file doesn't exist
        if (Files.exists(settingsPath)) {
            settingsService.deleteSettings();
        }
        
        assertFalse(Files.exists(settingsPath));
        
        // Save settings
        Settings settings = new Settings();
        settingsService.save(settings);
        
        // Verify file was created
        assertTrue(Files.exists(settingsPath));
    }
    
    @Test
    void testDeleteSettings() throws IOException {
        // Create settings file
        Settings settings = new Settings();
        settingsService.save(settings);
        assertTrue(Files.exists(settingsPath));
        
        // Delete settings
        settingsService.deleteSettings();
        
        // Verify file was deleted
        assertFalse(Files.exists(settingsPath));
    }
    
    @Test
    void testDeleteSettingsWhenFileDoesNotExist() {
        // This should not throw an exception
        assertDoesNotThrow(() -> settingsService.deleteSettings());
    }
    
    @Test
    void testGetSettingsPath() {
        Path path = settingsService.getSettingsPath();
        assertNotNull(path);
        assertTrue(path.toString().contains(".winlabs"));
        assertTrue(path.toString().endsWith("settings.json"));
    }
    
    @Test
    void testSaveAndLoadMultipleTimes() throws IOException {
        // First save
        Settings settings1 = new Settings();
        settings1.setTheme("light");
        settingsService.save(settings1);
        
        // Second save (overwrite)
        Settings settings2 = new Settings();
        settings2.setTheme("rainbow");
        settings2.setMasterVolume(0.5);
        settingsService.save(settings2);
        
        // Load and verify latest settings
        Settings loaded = settingsService.load();
        assertEquals("rainbow", loaded.getTheme());
        assertEquals(0.5, loaded.getMasterVolume(), 0.001);
    }
    
    @Test
    void testLoadWithPartialData() throws IOException {
        // Save full settings
        Settings settings = new Settings();
        settings.setTheme("light");
        settings.setMasterVolume(0.8);
        settingsService.save(settings);
        
        // Load and verify defaults are used for missing fields
        Settings loaded = settingsService.load();
        assertNotNull(loaded);
        assertEquals("light", loaded.getTheme());
        assertEquals(0.8, loaded.getMasterVolume(), 0.001);
    }
}
