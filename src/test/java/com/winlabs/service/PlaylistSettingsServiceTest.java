package com.winlabs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.winlabs.model.PlaylistSettings;

class PlaylistSettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripsPlaylistSettings() throws Exception {
        PlaylistSettingsService service = new PlaylistSettingsService();
        PlaylistSettings settings = new PlaylistSettings();
        settings.setMasterVolume(0.65);
        settings.setAudioFileDirectory("C:/audio");
        settings.setDefaultPreWait(1.5);
        settings.setDefaultPostWait(2.25);
        settings.setDefaultAutoFollow(true);
        settings.setPlaylistName("Show A");
        settings.setLastModified(123456789L);
        settings.setIsPinned(true);
        settings.setCueTableColumnOrder("name,number,filePath");
        settings.setCueTableColumnWidths("name=220.0,number=60.0,filePath=340.0");

        Path playlistPath = tempDir.resolve("show.json");
        Files.writeString(playlistPath, "{}");

        service.save(playlistPath, settings);
        assertTrue(service.exists(playlistPath));

        PlaylistSettings loaded = service.load(playlistPath);
        assertEquals(0.65, loaded.getMasterVolume());
        assertEquals("C:/audio", loaded.getAudioFileDirectory());
        assertEquals(1.5, loaded.getDefaultPreWait());
        assertEquals(2.25, loaded.getDefaultPostWait());
        assertTrue(loaded.isDefaultAutoFollow());
        assertEquals("Show A", loaded.getPlaylistName());
        assertEquals(123456789L, loaded.getLastModified());
        assertTrue(loaded.isPinned());
        assertEquals("name,number,filePath", loaded.getCueTableColumnOrder());
        assertEquals("name=220.0,number=60.0,filePath=340.0", loaded.getCueTableColumnWidths());
    }

    @Test
    void loadReturnsDefaultsWhenNoWlpExists() throws Exception {
        PlaylistSettingsService service = new PlaylistSettingsService();
        Path playlistPath = tempDir.resolve("missing.json");
        Files.writeString(playlistPath, "{}");

        PlaylistSettings settings = service.load(playlistPath);

        assertEquals(1.0, settings.getMasterVolume());
        assertEquals("", settings.getCueTableColumnOrder());
        assertEquals("", settings.getCueTableColumnWidths());
        assertFalse(settings.isPinned());
    }
}
