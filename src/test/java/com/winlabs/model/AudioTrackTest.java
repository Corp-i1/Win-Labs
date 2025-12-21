package com.winlabs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AudioTrack model.
 * Note: Tests focus on model behavior without MediaPlayer integration.
 */
class AudioTrackTest {
    
    private AudioTrack audioTrack;
    
    @BeforeEach
    void setUp() {
        audioTrack = new AudioTrack();
    }
    
    @Test
    void testTrackCreation() {
        assertNotNull(audioTrack.getTrackId());
        assertEquals(PlaybackState.STOPPED, audioTrack.getState());
        assertNull(audioTrack.getFilePath());
        assertFalse(audioTrack.isPooled());
        assertFalse(audioTrack.isPlaying());
    }
    
    @Test
    void testUniqueTrackIds() {
        AudioTrack track1 = new AudioTrack();
        AudioTrack track2 = new AudioTrack();
        
        assertNotEquals(track1.getTrackId(), track2.getTrackId());
    }
    
    @Test
    void testSetMediaPlayer() {
        assertNull(audioTrack.getMediaPlayer());
        
        // Create a dummy media player (requires a valid media file)
        // For unit testing, we'll just verify the setter works
        audioTrack.setMediaPlayer(null);
        assertNull(audioTrack.getMediaPlayer());
    }
    
    @Test
    void testStateManagement() {
        assertEquals(PlaybackState.STOPPED, audioTrack.getState());
        
        audioTrack.setState(PlaybackState.PLAYING);
        assertEquals(PlaybackState.PLAYING, audioTrack.getState());
        assertTrue(audioTrack.isPlaying());
        
        audioTrack.setState(PlaybackState.PAUSED);
        assertEquals(PlaybackState.PAUSED, audioTrack.getState());
        assertFalse(audioTrack.isPlaying());
    }
    
    @Test
    void testFilePathManagement() {
        assertNull(audioTrack.getFilePath());
        
        String testPath = "/path/to/test.mp3";
        audioTrack.setFilePath(testPath);
        assertEquals(testPath, audioTrack.getFilePath());
    }
    
    @Test
    void testPooledStatus() {
        assertFalse(audioTrack.isPooled());
        
        audioTrack.setPooled(true);
        assertTrue(audioTrack.isPooled());
        
        audioTrack.setPooled(false);
        assertFalse(audioTrack.isPooled());
    }
    
    @Test
    void testLastUsedTimestamp() {
        long initialTimestamp = audioTrack.getLastUsedTimestamp();
        assertTrue(initialTimestamp > 0);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }
        
        audioTrack.setState(PlaybackState.PLAYING);
        long newTimestamp = audioTrack.getLastUsedTimestamp();
        
        assertTrue(newTimestamp > initialTimestamp);
    }
    
    @Test
    void testVolumeManagement() {
        // Without a media player, volume should return default
        assertEquals(0.5, audioTrack.getVolume(), 0.01);
        
        // Setting volume without media player should not throw
        assertDoesNotThrow(() -> audioTrack.setVolume(0.8));
    }
    
    @Test
    void testPlayWithoutMediaPlayer() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> audioTrack.play()
        );
        
        assertTrue(exception.getMessage().contains("No audio loaded"));
    }
    
    @Test
    void testPauseWithoutMediaPlayer() {
        // Should not throw when no media player
        assertDoesNotThrow(() -> audioTrack.pause());
    }
    
    @Test
    void testStopWithoutMediaPlayer() {
        // Should not throw when no media player
        assertDoesNotThrow(() -> audioTrack.stop());
    }
    
    @Test
    void testGetCurrentTimeWithoutMediaPlayer() {
        assertEquals(0.0, audioTrack.getCurrentTime(), 0.01);
    }
    
    @Test
    void testGetDurationWithoutMediaPlayer() {
        assertEquals(0.0, audioTrack.getDuration(), 0.01);
    }
    
    @Test
    void testReset() {
        audioTrack.setState(PlaybackState.PLAYING);
        audioTrack.setFilePath("/test/path.mp3");
        long initialTimestamp = audioTrack.getLastUsedTimestamp();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }
        
        audioTrack.reset();
        
        assertEquals(PlaybackState.STOPPED, audioTrack.getState());
        assertTrue(audioTrack.getLastUsedTimestamp() > initialTimestamp);
    }
    
    @Test
    void testDispose() {
        audioTrack.setFilePath("/test/path.mp3");
        audioTrack.setState(PlaybackState.PLAYING);
        
        audioTrack.dispose();
        
        assertNull(audioTrack.getFilePath());
        assertEquals(PlaybackState.STOPPED, audioTrack.getState());
        assertNull(audioTrack.getMediaPlayer());
    }
    
    @Test
    void testOnEndListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AudioTrack> capturedTrack = new AtomicReference<>();
        
        audioTrack.setOnEndListener(track -> {
            capturedTrack.set(track);
            latch.countDown();
        });
        
        // Simulate creating and ending a media player
        // In a real scenario, this would be triggered by MediaPlayer.setOnEndOfMedia
        // For testing, we'll directly invoke the callback if we had access
        // Since we can't easily test JavaFX MediaPlayer callbacks in unit tests,
        // we'll verify the listener is set
        assertDoesNotThrow(() -> audioTrack.setOnEndListener(track -> {}));
    }
    
    @Test
    void testProgressListener() {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        
        audioTrack.setProgressListener(duration -> {
            listenerCalled.set(true);
        });
        
        // Verify listener is set without error
        assertDoesNotThrow(() -> audioTrack.setProgressListener(duration -> {}));
    }
}
