package com.winlabs.service;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.PlaybackState;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages a pool of MediaPlayer instances for multi-track audio playback.
 * Handles pre-warming, pooling, and automatic culling of unused players
 * to minimize latency and resource usage.
 */
public class AudioPlayerPool {
    
    private static final int DEFAULT_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final long CULL_TIMEOUT_MS = 30000; // 30 seconds
    
    private final CopyOnWriteArrayList<AudioTrack> availableTracks;
    private final ConcurrentHashMap<String, AudioTrack> activeTracks;
    private final int initialPoolSize;
    private final int maxPoolSize;
    
    public AudioPlayerPool() {
        this(DEFAULT_POOL_SIZE, MAX_POOL_SIZE);
    }
    
    public AudioPlayerPool(int initialPoolSize, int maxPoolSize) {
        this.initialPoolSize = Math.max(1, initialPoolSize);
        this.maxPoolSize = Math.max(this.initialPoolSize, maxPoolSize);
        this.availableTracks = new CopyOnWriteArrayList<>();
        this.activeTracks = new ConcurrentHashMap<>();
    }
    
    /**
     * Pre-warms the pool by creating initial track instances.
     * This should be called during initialization to avoid delays on first playback.
     */
    public void prewarm() {
        for (int i = 0; i < initialPoolSize; i++) {
            AudioTrack track = new AudioTrack();
            track.setPooled(true);
            availableTracks.add(track);
        }
    }
    
    /**
     * Acquires an audio track from the pool for playback.
     * Creates a new track if the pool is empty and under max size.
     * 
     * @param filePath Path to the audio file to load
     * @return An AudioTrack ready for playback
     * @throws Exception if the audio file cannot be loaded
     */
    public AudioTrack acquireTrack(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        
        // Try to get an available track from the pool
        AudioTrack track = null;
        if (!availableTracks.isEmpty()) {
            track = availableTracks.remove(0);
        } else if (getTotalTrackCount() < maxPoolSize) {
            // Create a new track if under max size
            track = new AudioTrack();
        } else {
            throw new IllegalStateException(
                "Cannot acquire track: pool exhausted (max " + maxPoolSize + " tracks)");
        }
        
        // Load the audio file
        String mediaUrl = path.toUri().toString();
        Media media = new Media(mediaUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        
        track.setMediaPlayer(mediaPlayer);
        track.setFilePath(filePath);
        track.setPooled(false);
        
        // Set up listener to return track to pool when playback ends
        track.setOnEndListener(this::releaseTrack);
        
        // Add to active tracks
        activeTracks.put(track.getTrackId(), track);
        
        return track;
    }
    
    /**
     * Releases a track back to the pool after playback.
     * The track is reset and made available for reuse.
     * 
     * @param track The track to release
     */
    public void releaseTrack(AudioTrack track) {
        if (track == null) {
            return;
        }
        
        // Remove from active tracks
        activeTracks.remove(track.getTrackId());
        
        // Reset the track
        track.reset();
        track.setPooled(true);
        
        // Return to pool if not over initial size
        if (availableTracks.size() < initialPoolSize) {
            availableTracks.add(track);
        } else {
            // Dispose if pool is full
            track.dispose();
        }
    }
    
    /**
     * Forces a track to be released back to the pool immediately.
     * Stops playback if active.
     * 
     * @param trackId The ID of the track to release
     */
    public void forceReleaseTrack(String trackId) {
        AudioTrack track = activeTracks.get(trackId);
        if (track != null) {
            track.stop();
            releaseTrack(track);
        }
    }
    
    /**
     * Gets a track by its ID.
     * 
     * @param trackId The track ID
     * @return The track, or null if not found
     */
    public AudioTrack getTrack(String trackId) {
        return activeTracks.get(trackId);
    }
    
    /**
     * Gets all currently active tracks.
     * 
     * @return List of active tracks
     */
    public List<AudioTrack> getActiveTracks() {
        return new ArrayList<>(activeTracks.values());
    }
    
    /**
     * Gets the count of active tracks.
     */
    public int getActiveTrackCount() {
        return activeTracks.size();
    }
    
    /**
     * Gets the count of available (pooled) tracks.
     */
    public int getAvailableTrackCount() {
        return availableTracks.size();
    }
    
    /**
     * Gets the total count of all tracks (active + available).
     */
    public int getTotalTrackCount() {
        return activeTracks.size() + availableTracks.size();
    }
    
    /**
     * Culls unused tracks from the pool that have exceeded the timeout.
     * This helps manage memory by disposing of tracks that haven't been used recently.
     * 
     * @return Number of tracks culled
     */
    public int cullUnusedTracks() {
        long now = System.currentTimeMillis();
        int culled = 0;
        
        List<AudioTrack> tracksToRemove = availableTracks.stream()
            .filter(track -> (now - track.getLastUsedTimestamp()) > CULL_TIMEOUT_MS)
            .collect(Collectors.toList());
        
        for (AudioTrack track : tracksToRemove) {
            availableTracks.remove(track);
            track.dispose();
            culled++;
        }
        
        return culled;
    }
    
    /**
     * Stops all active tracks immediately.
     */
    public void stopAll() {
        for (AudioTrack track : activeTracks.values()) {
            track.stop();
        }
    }
    
    /**
     * Pauses all active tracks.
     */
    public void pauseAll() {
        for (AudioTrack track : activeTracks.values()) {
            if (track.isPlaying()) {
                track.pause();
            }
        }
    }
    
    /**
     * Resumes all paused tracks.
     */
    public void resumeAll() {
        for (AudioTrack track : activeTracks.values()) {
            if (track.getState() == PlaybackState.PAUSED) {
                track.play();
            }
        }
    }
    
    /**
     * Sets the volume for all tracks.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolumeAll(double volume) {
        double clampedVolume = Math.max(0.0, Math.min(1.0, volume));
        
        for (AudioTrack track : activeTracks.values()) {
            track.setVolume(clampedVolume);
        }
        
        for (AudioTrack track : availableTracks) {
            track.setVolume(clampedVolume);
        }
    }
    
    /**
     * Disposes of all tracks and releases all resources.
     * The pool should not be used after calling this method.
     */
    public void dispose() {
        // Dispose active tracks
        for (AudioTrack track : activeTracks.values()) {
            track.dispose();
        }
        activeTracks.clear();
        
        // Dispose available tracks
        for (AudioTrack track : availableTracks) {
            track.dispose();
        }
        availableTracks.clear();
    }
}
