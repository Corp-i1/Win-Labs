package com.winlabs.integration;

import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;
import com.winlabs.service.PlaylistService;
import com.winlabs.util.PathUtil;
import com.winlabs.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify multiple components working together.
 */
class PlaylistIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testCompletePlaylistWorkflow() throws IOException {
        // Create a playlist with multiple cues
        Playlist playlist = new Playlist("Integration Test Show");
        
        // Add cues with various properties
        Cue cue1 = new Cue(1, "Opening", "C:/audio/opening.mp3");
        cue1.setDuration(TimeUtil.parseTime("03:00"));
        cue1.setPreWait(5.0);
        cue1.setAutoFollow(true);
        
        Cue cue2 = new Cue(2, "Intermission", "C:/audio/intermission.wav");
        cue2.setDuration(TimeUtil.parseTime("05:30"));
        cue2.setPostWait(3.0);
        cue2.setAutoFollow(true);
        
        Cue cue3 = new Cue(3, "Finale", "C:/audio/finale.aac");
        cue3.setDuration(TimeUtil.parseTime("04:15"));
        cue3.setAutoFollow(false);
        
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);
        
        // Verify playlist state
        assertEquals(3, playlist.size());
        
        // Save to file
        PlaylistService service = new PlaylistService();
        Path playlistFile = tempDir.resolve("integration-test.json");
        service.save(playlist, playlistFile.toString());
        
        // Load it back
        Playlist loadedPlaylist = service.load(playlistFile.toString());
        
        // Verify all data preserved
        assertEquals("Integration Test Show", loadedPlaylist.getName());
        assertEquals(3, loadedPlaylist.size());
        
        // Verify first cue
        Cue loaded1 = loadedPlaylist.getCue(0);
        assertEquals("Opening", loaded1.getName());
        assertTrue(PathUtil.isAudioFile(loaded1.getFilePath()));
        assertEquals(180.0, loaded1.getDuration(), 0.001);
        assertEquals(5.0, loaded1.getPreWait(), 0.001);
        assertTrue(loaded1.isAutoFollow());
        
        // Verify formatted time
        String formatted = TimeUtil.formatTime(loaded1.getDuration());
        assertEquals("03:00", formatted);
        
        // Test renumbering
        loadedPlaylist.removeCue(1); // Remove intermission
        assertEquals(2, loadedPlaylist.size());
        
        loadedPlaylist.renumberCues();
        assertEquals(1, loadedPlaylist.getCue(0).getNumber());
        assertEquals(2, loadedPlaylist.getCue(1).getNumber());
        
        // Save modified playlist
        Path modifiedFile = tempDir.resolve("modified.json");
        service.save(loadedPlaylist, modifiedFile.toString());
        
        // Load and verify
        Playlist finalPlaylist = service.load(modifiedFile.toString());
        assertEquals(2, finalPlaylist.size());
        assertEquals("Opening", finalPlaylist.getCue(0).getName());
        assertEquals("Finale", finalPlaylist.getCue(1).getName());
    }
    
    @Test
    void testPlaylistWithUtilities() {
        Playlist playlist = new Playlist("Utility Test");
        
        // Create cues using utility functions
        for (int i = 1; i <= 5; i++) {
            String filename = "track" + i + ".mp3";
            assertTrue(PathUtil.isAudioFile(filename));
            
            Cue cue = new Cue(i, "Track " + i, "C:/music/" + filename);
            cue.setDuration(TimeUtil.parseTime("02:30"));
            playlist.addCue(cue);
        }
        
        assertEquals(5, playlist.size());
        
        // Verify all cues have correct format
        for (Cue cue : playlist.getCues()) {
            String timeStr = TimeUtil.formatTime(cue.getDuration());
            assertEquals("02:30", timeStr);
            
            String ext = PathUtil.getFileExtension(Path.of(cue.getFilePath()));
            assertEquals(".mp3", ext);
        }
    }
    
    @Test
    void testEmptyPlaylistRoundTrip() throws IOException {
        PlaylistService service = new PlaylistService();
        
        // Create empty playlist
        Playlist empty = service.createNew("Empty Test");
        assertEquals(0, empty.size());
        
        // Save and load
        Path file = tempDir.resolve("empty.json");
        service.save(empty, file.toString());
        
        Playlist loaded = service.load(file.toString());
        assertEquals("Empty Test", loaded.getName());
        assertEquals(0, loaded.size());
    }
    
    @Test
    void testComplexTimeFormatting() {
        Cue cue = new Cue();
        
        // Test various durations
        double[] durations = {0.0, 30.5, 60.0, 125.75, 3665.0};
        String[] expected = {"00:00", "00:30", "01:00", "02:05", "61:05"};
        
        for (int i = 0; i < durations.length; i++) {
            cue.setDuration(durations[i]);
            String formatted = TimeUtil.formatTime(cue.getDuration());
            assertEquals(expected[i], formatted, 
                "Duration " + durations[i] + " should format as " + expected[i]);
        }
    }
    
    @Test
    void testAudioFileValidation() {
        String[] validFiles = {
            "song.mp3", "audio.wav", "music.aiff", "track.aac",
            "sound.ogg", "audio.flac", "song.m4a", "music.wma"
        };
        
        String[] invalidFiles = {
            "document.txt", "image.jpg", "video.mp4", "data.csv"
        };
        
        for (String file : validFiles) {
            Cue cue = new Cue(1, "Test", "C:/audio/" + file);
            assertTrue(PathUtil.isAudioFile(cue.getFilePath()),
                file + " should be recognized as audio");
        }
        
        for (String file : invalidFiles) {
            assertFalse(PathUtil.isAudioFile(file),
                file + " should NOT be recognized as audio");
        }
    }
}
