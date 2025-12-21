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
    private Path appSettingsPath;
    private Path workspaceSettingsPath;
    
    @BeforeEach
    void setUp() {
        settingsService = new SettingsService();
        appSettingsPath = settingsService.getAppSettingsPath();
        workspaceSettingsPath = settingsService.getWorkspaceSettingsPath();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up test settings files
        if (Files.exists(appSettingsPath)) {
            Files.delete(appSettingsPath);
        }
        if (Files.exists(workspaceSettingsPath)) {
            Files.delete(workspaceSettingsPath);
        }
    }
    
    @Test
    void testSaveAndLoad() throws IOException {
        // Create settings with custom values
        Settings settings = new Settings();
        settings.setTheme("light");
        settings.setMasterVolume(0.75);
        settings.setAutoSaveEnabled(true);
        settings.setAutoSaveInterval(600);
        
        // Save settings
        settingsService.save(settings);
        
        // Verify files were created
        assertTrue(Files.exists(appSettingsPath));
        assertTrue(Files.exists(workspaceSettingsPath));
        
        // Load settings
        Settings loadedSettings = settingsService.load();
        
        // Verify loaded settings match saved settings
        assertEquals("light", loadedSettings.getTheme());
        assertEquals(0.75, loadedSettings.getMasterVolume(), 0.001);
        assertTrue(loadedSettings.isAutoSaveEnabled());
        assertEquals(600, loadedSettings.getAutoSaveInterval());
    }
    
    @Test
    void testLoadDefaultsWhenFileDoesNotExist() throws IOException {
        // Ensure settings files don't exist
        if (Files.exists(appSettingsPath)) {
            Files.delete(appSettingsPath);
        }
        if (Files.exists(workspaceSettingsPath)) {
            Files.delete(workspaceSettingsPath);
        }
        
        // Load settings (should return defaults)
        Settings settings = settingsService.load();
        
        // Verify default values
        assertEquals("dark", settings.getTheme());
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        assertFalse(settings.isAutoSaveEnabled());
        assertEquals(300, settings.getAutoSaveInterval());
    }
    
    @Test
    void testSaveCreatesFile() throws IOException {
        // Ensure settings files don't exist
        if (Files.exists(appSettingsPath)) {
            Files.delete(appSettingsPath);
        }
        if (Files.exists(workspaceSettingsPath)) {
            Files.delete(workspaceSettingsPath);
        }
        
        assertFalse(Files.exists(appSettingsPath));
        assertFalse(Files.exists(workspaceSettingsPath));
        
        // Save settings
        Settings settings = new Settings();
        settingsService.save(settings);
        
        // Verify both files were created
        assertTrue(Files.exists(appSettingsPath));
        assertTrue(Files.exists(workspaceSettingsPath));
    }
    
    @Test
    void testDeleteSettings() throws IOException {
        // Create settings files
        Settings settings = new Settings();
        settingsService.save(settings);
        assertTrue(Files.exists(appSettingsPath));
        assertTrue(Files.exists(workspaceSettingsPath));
        
        // Delete settings
        settingsService.deleteSettings();
        
        // Verify both files were deleted
        assertFalse(Files.exists(appSettingsPath));
        assertFalse(Files.exists(workspaceSettingsPath));
    }
    
    @Test
    void testDeleteSettingsWhenFileDoesNotExist() {
        // This should not throw an exception
        assertDoesNotThrow(() -> settingsService.deleteSettings());
    }
    
    @Test
    void testGetSettingsPath() {
        Path appPath = settingsService.getAppSettingsPath();
        Path workspacePath = settingsService.getWorkspaceSettingsPath();
        
        assertNotNull(appPath);
        assertNotNull(workspacePath);
        
        assertTrue(appPath.toString().contains(".winlabs"));
        assertTrue(appPath.toString().endsWith("app-settings.json"));
        
        assertTrue(workspacePath.toString().contains(".winlabs"));
        assertTrue(workspacePath.toString().endsWith("workspace-settings.json"));
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
