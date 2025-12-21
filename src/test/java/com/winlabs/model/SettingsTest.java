package com.winlabs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        settings.setAutoSaveEnabled(true);
        settings.setAutoSaveInterval(600);
        
        // Reset to defaults
        settings.resetToDefaults();
        
        // Verify all settings are back to defaults
        assertEquals("dark", settings.getTheme());
        assertEquals(1.0, settings.getMasterVolume(), 0.001);
        assertFalse(settings.isAutoSaveEnabled());
        assertEquals(300, settings.getAutoSaveInterval());
    }
    
    @Test
    void testPropertyBindings() {
        // Test that settings delegates work correctly
        ApplicationSettings appSettings = settings.getApplicationSettings();
        WorkspaceSettings workspaceSettings = settings.getWorkspaceSettings();
        
        assertNotNull(appSettings);
        assertNotNull(workspaceSettings);
        
        // Verify property objects exist
        assertNotNull(appSettings.themeProperty());
        assertNotNull(appSettings.autoSaveEnabledProperty());
        assertNotNull(appSettings.autoSaveIntervalProperty());
        assertNotNull(workspaceSettings.masterVolumeProperty());
        assertNotNull(workspaceSettings.lastPlaylistPathProperty());
        assertNotNull(workspaceSettings.audioFileDirectoryProperty());
        assertNotNull(workspaceSettings.lastPlaylistPathProperty());
        assertNotNull(workspaceSettings.audioFileDirectoryProperty());
    }
    
    @Test
    void testRecentFilesDefault() {
        List<String> recentFiles = settings.getRecentFiles();
        assertNotNull(recentFiles);
        assertTrue(recentFiles.isEmpty());
    }
    
    @Test
    void testAddRecentFile() {
        String file1 = "/path/to/playlist1.json";
        String file2 = "/path/to/playlist2.json";
        
        settings.addRecentFile(file1);
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertEquals(file1, recentFiles.get(0));
        
        settings.addRecentFile(file2);
        recentFiles = settings.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertEquals(file2, recentFiles.get(0)); // Most recent first
        assertEquals(file1, recentFiles.get(1));
    }
    
    @Test
    void testAddRecentFileMovesToFront() {
        String file1 = "/path/to/playlist1.json";
        String file2 = "/path/to/playlist2.json";
        
        settings.addRecentFile(file1);
        settings.addRecentFile(file2);
        
        // Re-add file1 - it should move to front
        settings.addRecentFile(file1);
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertEquals(file1, recentFiles.get(0)); // Now first
        assertEquals(file2, recentFiles.get(1));
    }
    
    @Test
    void testAddRecentFileIgnoresNullAndEmpty() {
        settings.addRecentFile(null);
        settings.addRecentFile("");
        
        List<String> recentFiles = settings.getRecentFiles();
        assertTrue(recentFiles.isEmpty());
    }
    
    @Test
    void testAddRecentFileMaxLimit() {
        // Add 15 files (max is 10)
        for (int i = 0; i < 15; i++) {
            settings.addRecentFile("/path/to/playlist" + i + ".json");
        }
        
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(10, recentFiles.size());
        
        // Most recent (14) should be first
        assertEquals("/path/to/playlist14.json", recentFiles.get(0));
        // Oldest kept (5) should be last
        assertEquals("/path/to/playlist5.json", recentFiles.get(9));
    }
    
    @Test
    void testRemoveRecentFile() {
        String file1 = "/path/to/playlist1.json";
        String file2 = "/path/to/playlist2.json";
        
        settings.addRecentFile(file1);
        settings.addRecentFile(file2);
        
        settings.removeRecentFile(file1);
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertEquals(file2, recentFiles.get(0));
    }
    
    @Test
    void testClearRecentFiles() {
        settings.addRecentFile("/path/to/playlist1.json");
        settings.addRecentFile("/path/to/playlist2.json");
        settings.addRecentFile("/path/to/playlist3.json");
        
        settings.clearRecentFiles();
        List<String> recentFiles = settings.getRecentFiles();
        assertTrue(recentFiles.isEmpty());
    }
    
    @Test
    void testSetRecentFiles() {
        List<String> files = List.of(
            "/path/to/playlist1.json",
            "/path/to/playlist2.json",
            "/path/to/playlist3.json"
        );
        
        settings.setRecentFiles(files);
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(3, recentFiles.size());
        assertEquals(files.get(0), recentFiles.get(0));
        assertEquals(files.get(1), recentFiles.get(1));
        assertEquals(files.get(2), recentFiles.get(2));
    }
    
    @Test
    void testSetRecentFilesEnforcesLimit() {
        // Create a list with 15 files
        List<String> files = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            files.add("/path/to/playlist" + i + ".json");
        }
        
        settings.setRecentFiles(files);
        List<String> recentFiles = settings.getRecentFiles();
        assertEquals(10, recentFiles.size());
        
        // First 10 files should be kept
        assertEquals("/path/to/playlist0.json", recentFiles.get(0));
        assertEquals("/path/to/playlist9.json", recentFiles.get(9));
    }
    
    @Test
    void testResetToDefaultsClearsRecentFiles() {
        settings.addRecentFile("/path/to/playlist1.json");
        settings.addRecentFile("/path/to/playlist2.json");
        
        settings.resetToDefaults();
        
        List<String> recentFiles = settings.getRecentFiles();
        assertTrue(recentFiles.isEmpty());
    }
    
    // Pinned Playlists Tests
    
    @Test
    void testTogglePinned() {
        String filePath = "/path/to/playlist1.json";
        
        // Add to recent first
        settings.addRecentFile(filePath);
        assertTrue(settings.getRecentFiles().contains(filePath));
        
        // First toggle should pin the playlist and remove from recent
        boolean isPinned = settings.togglePinned(filePath);
        assertTrue(isPinned);
        assertTrue(settings.isPinned(filePath));
        assertFalse(settings.getRecentFiles().contains(filePath));
        
        // Second toggle should unpin the playlist and add back to recent
        isPinned = settings.togglePinned(filePath);
        assertFalse(isPinned);
        assertFalse(settings.isPinned(filePath));
        assertTrue(settings.getRecentFiles().contains(filePath));
    }
    
    @Test
    void testPinnedPlaylistsNotAddedToRecent() {
        String filePath = "/path/to/playlist1.json";
        
        // Pin the playlist first
        settings.togglePinned(filePath);
        assertTrue(settings.isPinned(filePath));
        
        // Try to add as recent - should be ignored
        settings.addRecentFile(filePath);
        assertFalse(settings.getRecentFiles().contains(filePath));
        assertTrue(settings.isPinned(filePath)); // Still pinned
    }
    
    @Test
    void testPinnedPlaylistsDontCountAgainstRecentLimit() {
        // Pin 3 playlists
        settings.togglePinned("/path/to/pinned1.json");
        settings.togglePinned("/path/to/pinned2.json");
        settings.togglePinned("/path/to/pinned3.json");
        
        // Add 10 recent files (should not conflict with pinned)
        for (int i = 0; i < 10; i++) {
            settings.addRecentFile("/path/to/recent" + i + ".json");
        }
        
        // Should have 3 pinned + 10 recent = 13 total
        assertEquals(3, settings.getPinnedPlaylists().size());
        assertEquals(10, settings.getRecentFiles().size());
    }
    
    @Test
    void testIsPinnedWithNullFilePath() {
        assertFalse(settings.isPinned(null));
    }
    
    @Test
    void testTogglePinnedWithNullFilePath() {
        boolean result = settings.togglePinned(null);
        assertFalse(result);
    }
    
    @Test
    void testTogglePinnedWithEmptyFilePath() {
        boolean result = settings.togglePinned("");
        assertFalse(result);
    }
    
    @Test
    void testGetPinnedPlaylists() {
        settings.togglePinned("/path/to/playlist1.json");
        settings.togglePinned("/path/to/playlist2.json");
        settings.togglePinned("/path/to/playlist3.json");
        
        java.util.Set<String> pinnedPlaylists = settings.getPinnedPlaylists();
        assertEquals(3, pinnedPlaylists.size());
        assertTrue(pinnedPlaylists.contains("/path/to/playlist1.json"));
        assertTrue(pinnedPlaylists.contains("/path/to/playlist2.json"));
        assertTrue(pinnedPlaylists.contains("/path/to/playlist3.json"));
    }
    
    @Test
    void testSetPinnedPlaylists() {
        java.util.Set<String> playlists = new java.util.HashSet<>();
        playlists.add("/path/to/playlist1.json");
        playlists.add("/path/to/playlist2.json");
        
        settings.setPinnedPlaylists(playlists);
        
        assertTrue(settings.isPinned("/path/to/playlist1.json"));
        assertTrue(settings.isPinned("/path/to/playlist2.json"));
        assertFalse(settings.isPinned("/path/to/playlist3.json"));
    }
    
    @Test
    void testClearPinnedPlaylists() {
        settings.togglePinned("/path/to/playlist1.json");
        settings.togglePinned("/path/to/playlist2.json");
        
        settings.clearPinnedPlaylists();
        
        assertFalse(settings.isPinned("/path/to/playlist1.json"));
        assertFalse(settings.isPinned("/path/to/playlist2.json"));
        assertTrue(settings.getPinnedPlaylists().isEmpty());
    }
    
    @Test
    void testResetToDefaultsClearsPinnedPlaylists() {
        settings.togglePinned("/path/to/playlist1.json");
        settings.togglePinned("/path/to/playlist2.json");
        
        settings.resetToDefaults();
        
        assertTrue(settings.getPinnedPlaylists().isEmpty());
        assertFalse(settings.isPinned("/path/to/playlist1.json"));
    }
}
