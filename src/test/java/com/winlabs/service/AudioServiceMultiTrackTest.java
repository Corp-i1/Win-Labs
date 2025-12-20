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
 * Tests for AudioService with multi-track support.
 * Note: Tests focus on service behavior without full MediaPlayer integration.
 */
class AudioServiceMultiTrackTest {
    
    private AudioService singleTrackService;
    private AudioService multiTrackService;
    private Path testAudioFile;
    
    @BeforeEach
    void setUp() throws IOException {
        singleTrackService = new AudioService(false);
        multiTrackService = new AudioService(true);
        
        // Create a temporary test audio file
        testAudioFile = Files.createTempFile("test-audio", ".mp3");
    }
    
    @AfterEach
    void tearDown() {
        if (singleTrackService != null) {
            singleTrackService.dispose();
        }
        
        if (multiTrackService != null) {
            multiTrackService.dispose();
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
    void testServiceModeDetection() {
        assertFalse(singleTrackService.isMultiTrackMode());
        assertTrue(multiTrackService.isMultiTrackMode());
    }
    
    @Test
    void testDefaultServiceIsSingleTrack() {
        AudioService defaultService = new AudioService();
        assertFalse(defaultService.isMultiTrackMode());
        defaultService.dispose();
    }
    
    @Test
    void testPlayTrackInSingleTrackMode() {
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            singleTrackService.playTrack(testAudioFile.toString());
        });
        
        assertTrue(exception.getMessage().contains("multi-track mode"));
    }
    
    @Test
    void testPlayTrackInMultiTrackMode() {
        // Note: Will throw MediaException because test file isn't valid media,
        // but we can verify it attempts multi-track playback
        assertThrows(Exception.class, () -> {
            multiTrackService.playTrack(testAudioFile.toString());
        });
    }
    
    @Test
    void testPlayTrackWithVolume() {
        // Note: Will throw MediaException because test file isn't valid media
        assertThrows(Exception.class, () -> {
            multiTrackService.playTrack(testAudioFile.toString(), 0.5);
        });
    }
    
    @Test
    void testGetTrackInSingleTrackMode() {
        AudioTrack track = singleTrackService.getTrack("any-id");
        assertNull(track);
    }
    
    @Test
    void testGetActiveTracksInSingleTrackMode() {
        List<AudioTrack> tracks = singleTrackService.getActiveTracks();
        assertNotNull(tracks);
        assertEquals(0, tracks.size());
    }
    
    @Test
    void testGetActiveTracksInMultiTrackMode() {
        List<AudioTrack> tracks = multiTrackService.getActiveTracks();
        assertNotNull(tracks);
        assertEquals(0, tracks.size());
    }
    
    @Test
    void testGetActiveTrackCount() {
        assertEquals(0, singleTrackService.getActiveTrackCount());
        assertEquals(0, multiTrackService.getActiveTrackCount());
    }
    
    @Test
    void testStopTrackInMultiTrackMode() {
        // Should not throw even with non-existent track
        assertDoesNotThrow(() -> multiTrackService.stopTrack("non-existent-id"));
    }
    
    @Test
    void testStopAllTracks() {
        // Should work in both modes without throwing
        assertDoesNotThrow(() -> singleTrackService.stopAllTracks());
        assertDoesNotThrow(() -> multiTrackService.stopAllTracks());
    }
    
    @Test
    void testPauseAllTracks() {
        // Should work in both modes without throwing
        assertDoesNotThrow(() -> singleTrackService.pauseAllTracks());
        assertDoesNotThrow(() -> multiTrackService.pauseAllTracks());
    }
    
    @Test
    void testResumeAllTracks() {
        // Should work in both modes without throwing
        assertDoesNotThrow(() -> singleTrackService.resumeAllTracks());
        assertDoesNotThrow(() -> multiTrackService.resumeAllTracks());
    }
    
    @Test
    void testSetVolumeAllTracks() {
        // Should work in both modes without throwing
        assertDoesNotThrow(() -> singleTrackService.setVolumeAllTracks(0.5));
        assertDoesNotThrow(() -> multiTrackService.setVolumeAllTracks(0.5));
        
        // Test boundary values
        assertDoesNotThrow(() -> multiTrackService.setVolumeAllTracks(0.0));
        assertDoesNotThrow(() -> multiTrackService.setVolumeAllTracks(1.0));
    }
    
    @Test
    void testCullUnusedTracks() {
        assertEquals(0, singleTrackService.cullUnusedTracks());
        
        // No tracks have been used yet, so none should be culled
        int culled = multiTrackService.cullUnusedTracks();
        assertEquals(0, culled);
    }
    
    @Test
    void testGetPlayerPool() {
        assertNull(singleTrackService.getPlayerPool());
        assertNotNull(multiTrackService.getPlayerPool());
    }
    
    @Test
    void testDisposeMultiTrackService() {
        AudioPlayerPool pool = multiTrackService.getPlayerPool();
        assertNotNull(pool);
        
        multiTrackService.dispose();
        
        // Pool should have been disposed
        assertEquals(0, pool.getTotalTrackCount());
    }
    
    @Test
    void testBackwardCompatibilityWithSingleTrack() throws Exception {
        // Verify that single-track mode still works with legacy methods
        assertEquals(PlaybackState.STOPPED, singleTrackService.getState());
        assertFalse(singleTrackService.isAudioLoaded());
        assertFalse(singleTrackService.isPlaying());
        
        // Load audio should still work in single-track mode
        // (Will throw MediaException with test file, but confirms method exists)
        assertThrows(Exception.class, () -> {
            singleTrackService.loadAudio(testAudioFile.toString());
        });
    }
    
    @Test
    void testMultiTrackServiceLegacyMethods() {
        // Multi-track service should still support legacy single-track methods
        assertEquals(PlaybackState.STOPPED, multiTrackService.getState());
        assertFalse(multiTrackService.isAudioLoaded());
        assertFalse(multiTrackService.isPlaying());
    }
}
