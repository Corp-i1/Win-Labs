package com.winlabs.service;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.PlaybackState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AudioService multi-track functionality.
 * Note: Tests focus on service behavior without full MediaPlayer integration.
 */
class AudioServiceMultiTrackTest {
    
    private AudioService audioService;
    private Path testAudioFile;
    
    @BeforeEach
    void setUp() throws IOException {
        audioService = new AudioService();
        
        // Create a temporary test audio file
        testAudioFile = Files.createTempFile("test-audio", ".mp3");
    }
    
    @AfterEach
    void tearDown() {
        if (audioService != null) {
            audioService.dispose();
        }
        
        if (testAudioFile != null && Files.exists(testAudioFile)) {
            deleteTestFileWithRetry();
        }
    }
    
    /**
     * Attempt to delete the temporary test file, retrying for a limited time
     * to allow file handles to be released on slower systems.
     */
    private void deleteTestFileWithRetry() {
        final long timeoutMillis = 5000L; // overall timeout for cleanup
        final long pollIntervalMillis = 50L;
        final long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            try {
                Files.deleteIfExists(testAudioFile);
                if (!Files.exists(testAudioFile)) {
                    return;
                }
            } catch (IOException e) {
                // Ignore and retry until timeout
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // If we get here, the file still exists; schedule deletion on JVM exit.
        testAudioFile.toFile().deleteOnExit();
    }
    
    @Test
    void testPlayTrack() {
        // Note: Will throw MediaException because test file isn't valid media,
        // but we can verify it attempts playback
        assertThrows(Exception.class, () -> {
            audioService.playTrack(testAudioFile.toString());
        });
    }
    
    @Test
    void testPlayTrackWithVolume() {
        // Note: Will throw MediaException because test file isn't valid media
        assertThrows(Exception.class, () -> {
            audioService.playTrack(testAudioFile.toString(), 0.5);
        });
    }
    
    @Test
    void testGetActiveTracks() {
        List<AudioTrack> tracks = audioService.getActiveTracks();
        assertNotNull(tracks);
        assertEquals(0, tracks.size());
    }
    
    @Test
    void testGetActiveTrackCount() {
        assertEquals(0, audioService.getActiveTrackCount());
    }
    
    @Test
    void testStopTrack() {
        // Should not throw even with non-existent track
        assertDoesNotThrow(() -> audioService.stopTrack("non-existent-id"));
    }
    
    @Test
    void testStopAllTracks() {
        assertDoesNotThrow(() -> audioService.stopAllTracks());
    }
    
    @Test
    void testPauseAllTracks() {
        assertDoesNotThrow(() -> audioService.pauseAllTracks());
    }
    
    @Test
    void testResumeAllTracks() {
        assertDoesNotThrow(() -> audioService.resumeAllTracks());
    }
    
    @Test
    void testSetVolumeAllTracks() {
        assertDoesNotThrow(() -> audioService.setVolumeAllTracks(0.5));
        
        // Test boundary values
        assertDoesNotThrow(() -> audioService.setVolumeAllTracks(0.0));
        assertDoesNotThrow(() -> audioService.setVolumeAllTracks(1.0));
    }
    
    @Test
    void testCullUnusedTracks() {
        // No tracks have been used yet, so none should be culled
        int culled = audioService.cullUnusedTracks();
        assertEquals(0, culled);
    }
    
    @Test
    void testAutoCulling() {
        // Multi-track mode should have auto-culling enabled after initialization
        assertTrue(audioService.isAutoCullEnabled());
    }
    
    @Test
    void testEnableAutoCulling() {
        audioService.disableAutoCulling();
        assertFalse(audioService.isAutoCullEnabled());
        
        audioService.enableAutoCulling();
        assertTrue(audioService.isAutoCullEnabled());
    }
    
    @Test
    void testDisableAutoCulling() {
        assertTrue(audioService.isAutoCullEnabled());
        
        audioService.disableAutoCulling();
        assertFalse(audioService.isAutoCullEnabled());
    }
    
    @Test
    void testGetPlayerPool() {
        assertNotNull(audioService.getPlayerPool());
    }
    
    @Test
    void testDispose() {
        AudioPlayerPool pool = audioService.getPlayerPool();
        assertNotNull(pool);
        
        audioService.dispose();
        
        // Pool should have been disposed
        assertEquals(0, pool.getTotalTrackCount());
    }
}
