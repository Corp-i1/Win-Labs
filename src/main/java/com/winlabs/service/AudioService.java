package com.winlabs.service;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.PlaybackState;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

//TODO: Add support for playlists, 
// TODO: gapless playback,
// TODO: equalizer settings, 
//TODO: Crossfade between tracks


/**
 * Service for audio playback using JavaFX MediaPlayer.
 * Supports both single-track (legacy) and multi-track playback modes.
 * 
 * Single-track mode: Traditional single MediaPlayer for simple playback.
 * Multi-track mode: Uses AudioPlayerPool for simultaneous overlapping audio.
 */
public class AudioService {
    
    // Single-track mode fields (legacy support)
    private MediaPlayer mediaPlayer;
    private PlaybackState state;
    private String currentFilePath;
    private Consumer<PlaybackState> stateChangeListener;
    private Consumer<Duration> progressListener;
    
    // Multi-track mode fields
    private AudioPlayerPool playerPool;
    private boolean multiTrackMode;
    
    /**
     * Creates a new AudioService in single-track mode (default).
     */
    public AudioService() {
        this(false);
    }
    
    /**
     * Creates a new AudioService with specified mode.
     * 
     * @param multiTrackMode If true, enables multi-track playback support
     */
    public AudioService(boolean multiTrackMode) {
        this.state = PlaybackState.STOPPED;
        this.currentFilePath = null;
        this.multiTrackMode = multiTrackMode;
        
        if (multiTrackMode) {
            this.playerPool = new AudioPlayerPool();
            this.playerPool.prewarm();
        }
    }
    
    /**
     * Loads an audio file from the given path.
     * Disposes of any currently playing media.
     */
    public void loadAudio(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        
        // Dispose of existing player
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
        
        // Create new media player
        String mediaUrl = path.toUri().toString();
        Media media = new Media(mediaUrl);
        mediaPlayer = new MediaPlayer(media);
        currentFilePath = filePath;
        
        // Set up listeners
        setupMediaPlayerListeners();
        
        setState(PlaybackState.STOPPED);
    }
    
    /**
     * Sets up event listeners for the media player.
     */
    private void setupMediaPlayerListeners() {
        // Update progress during playback
        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            if (progressListener != null) {
                progressListener.accept(newValue);
            }
        });
        
        // Handle end of media
        mediaPlayer.setOnEndOfMedia(() -> {
            setState(PlaybackState.STOPPED);
            mediaPlayer.seek(Duration.ZERO);
        });
        
        // Handle errors
        mediaPlayer.setOnError(() -> {
            System.err.println("Media error: " + mediaPlayer.getError().getMessage());
            setState(PlaybackState.STOPPED);
        });
    }
    
    /**
     * Starts or resumes audio playback.
     */
    public void play() {
        if (mediaPlayer == null) {
            throw new IllegalStateException("No audio loaded");
        }
        
        mediaPlayer.play();
        setState(PlaybackState.PLAYING);
    }
    
    /**
     * Pauses audio playback.
     */
    public void pause() {
        if (mediaPlayer == null) {
            return;
        }
        
        mediaPlayer.pause();
        setState(PlaybackState.PAUSED);
    }
    
    /**
     * Stops audio playback and returns to the beginning.
     */
    public void stop() {
        if (mediaPlayer == null) {
            return;
        }
        
        mediaPlayer.stop();
        mediaPlayer.seek(Duration.ZERO);
        setState(PlaybackState.STOPPED);
    }
    
    /**
     * Sets the playback volume (0.0 to 1.0).
     */
    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
        }
    }
    
    /**
     * Gets the current playback volume (0.0 to 1.0).
     */
    public double getVolume() {
        return mediaPlayer != null ? mediaPlayer.getVolume() : 0.5;
    }
    
    /**
     * Seeks to a specific time in the audio.
     */
    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
        }
    }
    
    /**
     * Gets the current playback time in seconds.
     */
    public double getCurrentTime() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentTime().toSeconds();
        }
        return 0.0;
    }
    
    /**
     * Gets the total duration of the current audio in seconds.
     */
    public double getDuration() {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            return mediaPlayer.getTotalDuration().toSeconds();
        }
        return 0.0;
    }
    
    /**
     * Gets the current playback state.
     */
    public PlaybackState getState() {
        return state;
    }
    
    /**
     * Gets the current file path.
     */
    public String getCurrentFilePath() {
        return currentFilePath;
    }
    
    /**
     * Checks if audio is currently loaded.
     */
    public boolean isAudioLoaded() {
        return mediaPlayer != null;
    }
    
    /**
     * Checks if audio is currently playing.
     */
    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }
    
    /**
     * Sets a listener for state changes.
     */
    public void setStateChangeListener(Consumer<PlaybackState> listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Sets a listener for progress updates.
     */
    public void setProgressListener(Consumer<Duration> listener) {
        this.progressListener = listener;
    }
    
    /**
     * Updates the state and notifies listeners.
     */
    private void setState(PlaybackState newState) {
        this.state = newState;
        if (stateChangeListener != null) {
            stateChangeListener.accept(newState);
        }
    }
    
    /**
     * Disposes of the media player and releases resources.
     */
    public void dispose() {
        if (multiTrackMode && playerPool != null) {
            playerPool.dispose();
        }
        
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        currentFilePath = null;
        setState(PlaybackState.STOPPED);
    }
    
    // ========== Multi-track Mode Methods ==========
    
    /**
     * Checks if multi-track mode is enabled.
     */
    public boolean isMultiTrackMode() {
        return multiTrackMode;
    }
    
    /**
     * Culls unused tracks from the pool (multi-track mode only).
     * This helps manage memory by disposing of tracks that haven't been used recently.
     * Note: Automatic culling is enabled by default when the pool is prewarmed.
     * 
     * @return Number of tracks culled, or 0 if not in multi-track mode
     */
    public int cullUnusedTracks() {
        return (multiTrackMode && playerPool != null) ? playerPool.cullUnusedTracks() : 0;
    }
    
    /**
     * Enables automatic periodic culling of unused tracks (multi-track mode only).
     * When enabled, the pool automatically removes tracks that haven't been used recently.
     */
    public void enableAutoCulling() {
        if (multiTrackMode && playerPool != null) {
            playerPool.enableAutoCulling();
        }
    }
    
    /**
     * Disables automatic periodic culling of unused tracks (multi-track mode only).
     */
    public void disableAutoCulling() {
        if (multiTrackMode && playerPool != null) {
            playerPool.disableAutoCulling();
        }
    }
    
    /**
     * Checks if automatic culling is currently enabled (multi-track mode only).
     * 
     * @return true if auto-culling is enabled, false otherwise
     */
    public boolean isAutoCullEnabled() {
        return (multiTrackMode && playerPool != null) && playerPool.isAutoCullEnabled();
    }
    
    /**
     * Gets the audio player pool for direct access to multi-track operations.
     * Use this to perform track-level operations like acquiring, pausing, stopping, etc.
     * 
     * @return The AudioPlayerPool, or null if not in multi-track mode
     */
    public AudioPlayerPool getPlayerPool() {
        return playerPool;
    }
}
