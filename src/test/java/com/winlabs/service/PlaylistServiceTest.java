package com.winlabs.service;

import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistServiceTest {
    
    private PlaylistService service;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        service = new PlaylistService();
    }
    
    @Test
    void testCreateNew() {
        Playlist playlist = service.createNew("Test Playlist");
        
        assertNotNull(playlist);
        assertEquals("Test Playlist", playlist.getName());
        assertEquals(0, playlist.size());
    }
    
    @Test
    void testSaveAndLoad() throws IOException {
        // Create a playlist
        Playlist original = new Playlist("My Show");
        
        Cue cue1 = new Cue(1, "Opening Music", "C:/music/opening.mp3");
        cue1.setDuration(180.0);
        cue1.setPreWait(5.0);
        cue1.setPostWait(3.0);
        cue1.setAutoFollow(true);
        
        Cue cue2 = new Cue(2, "Intermission", "C:/music/intermission.wav");
        cue2.setDuration(300.0);
        cue2.setAutoFollow(false);
        
        original.addCue(cue1);
        original.addCue(cue2);
        
        // Save it
        Path playlistFile = tempDir.resolve("test-playlist.json");
        service.save(original, playlistFile.toString());
        
        assertTrue(Files.exists(playlistFile));
        
        // Load it back
        Playlist loaded = service.load(playlistFile.toString());
        
        assertNotNull(loaded);
        assertEquals("My Show", loaded.getName());
        assertEquals(2, loaded.size());
        
        Cue loadedCue1 = loaded.getCue(0);
        assertEquals(1, loadedCue1.getNumber());
        assertEquals("Opening Music", loadedCue1.getName());
        assertEquals("C:/music/opening.mp3", loadedCue1.getFilePath());
        assertEquals(180.0, loadedCue1.getDuration(), 0.001);
        assertEquals(5.0, loadedCue1.getPreWait(), 0.001);
        assertEquals(3.0, loadedCue1.getPostWait(), 0.001);
        assertTrue(loadedCue1.isAutoFollow());
        
        Cue loadedCue2 = loaded.getCue(1);
        assertEquals(2, loadedCue2.getNumber());
        assertEquals("Intermission", loadedCue2.getName());
        assertEquals("C:/music/intermission.wav", loadedCue2.getFilePath());
        assertEquals(300.0, loadedCue2.getDuration(), 0.001);
        assertFalse(loadedCue2.isAutoFollow());
    }
    
    @Test
    void testSaveEmptyPlaylist() throws IOException {
        Playlist playlist = new Playlist("Empty");
        Path playlistFile = tempDir.resolve("empty.json");
        
        service.save(playlist, playlistFile.toString());
        
        assertTrue(Files.exists(playlistFile));
        
        Playlist loaded = service.load(playlistFile.toString());
        assertEquals("Empty", loaded.getName());
        assertEquals(0, loaded.size());
    }
    
    @Test
    void testSaveWithNullPlaylist() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.save(null, "test.json");
        });
    }
    
    @Test
    void testSaveWithNullFilePath() {
        Playlist playlist = new Playlist("Test");
        assertThrows(IllegalArgumentException.class, () -> {
            service.save(playlist, null);
        });
    }
    
    @Test
    void testSaveWithEmptyFilePath() {
        Playlist playlist = new Playlist("Test");
        assertThrows(IllegalArgumentException.class, () -> {
            service.save(playlist, "");
        });
    }
    
    @Test
    void testLoadNonExistentFile() {
        assertThrows(IOException.class, () -> {
            service.load("nonexistent-file.json");
        });
    }
    
    @Test
    void testLoadWithNullFilePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.load(null);
        });
    }
    
    @Test
    void testLoadWithEmptyFilePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.load("");
        });
    }
    
    @Test
    void testSaveSetsFilePath() throws IOException {
        Playlist playlist = new Playlist("Test");
        Path playlistFile = tempDir.resolve("test.json");
        
        assertNull(playlist.getFilePath());
        
        service.save(playlist, playlistFile.toString());
        
        assertEquals(playlistFile.toString(), playlist.getFilePath());
    }
    
    @Test
    void testJsonFormat() throws IOException {
        Playlist playlist = new Playlist("Format Test");
        Cue cue = new Cue(1, "Test Cue", "test.mp3");
        playlist.addCue(cue);
        
        Path playlistFile = tempDir.resolve("format.json");
        service.save(playlist, playlistFile.toString());
        
        String content = Files.readString(playlistFile);
        
        // Verify JSON structure
        assertTrue(content.contains("\"name\""));
        assertTrue(content.contains("\"Format Test\""));
        assertTrue(content.contains("\"version\""));
        assertTrue(content.contains("\"cues\""));
        assertTrue(content.contains("\"Test Cue\""));
    }
}
