package com.winlabs.service;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.PlaybackState;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    
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
            logger.error("Media playback error: {}", mediaPlayer.getError().getMessage());
            System.err.println("Media error: " + mediaPlayer.getError().getMessage());
            setState(PlaybackState.STOPPED);
        });
    }
    
    /**
     * Starts or resumes audio playback.
     */
    public void play() {
        if (mediaPlayer == null) {
            logger.error("Attempted to play with no audio loaded");
            throw new IllegalStateException("No audio loaded");
        }
        
        logger.debug("Starting playback");
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
        if (volume < 0.0) {
            logger.warn("Volume value {} is below minimum (0.0), will be clamped", volume);
        } else if (volume > 1.0) {
            logger.warn("Volume value {} is above maximum (1.0), will be clamped", volume);
        }

        double clampedVolume = Math.max(0.0, Math.min(1.0, volume));

        if (mediaPlayer != null) {
            double oldVolume = mediaPlayer.getVolume();
            if (Double.compare(oldVolume, clampedVolume) != 0) {
                mediaPlayer.setVolume(clampedVolume);
                logger.debug("Volume changed from {} to {}", oldVolume, clampedVolume);
            }
        } else {
            logger.warn("MediaPlayer is null, cannot set volume to {}", clampedVolume);
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
        logger.trace("seek() method entry");
        logger.debug("Attempting to seek to {} seconds", seconds);
        logger.trace("Seek parameter value: {}", seconds);
        
        if (seconds < 0.0) {
            logger.warn("Seek value {} is negative, this may cause issues", seconds);
        }
        
        logger.trace("Checking if mediaPlayer exists");
        if (mediaPlayer != null) {
            logger.debug("MediaPlayer exists, proceeding with seek");
            double currentTime = mediaPlayer.getCurrentTime().toSeconds();
            logger.trace("Current playback time before seek: {} seconds", currentTime);
            
            Duration totalDuration = mediaPlayer.getTotalDuration();
            if (totalDuration != null) {
                double totalSeconds = totalDuration.toSeconds();
                logger.trace("Total duration: {} seconds", totalSeconds);
                if (seconds > totalSeconds) {
                    logger.warn("Seek position {} exceeds total duration {}", seconds, totalSeconds);
                }
            } else {
                logger.trace("Total duration is null, cannot validate seek position");
            }
            
            logger.debug("Creating Duration object for {} seconds", seconds);
            Duration seekDuration = Duration.seconds(seconds);
            logger.trace("Duration object created: {}", seekDuration);
            
            logger.debug("Calling seek on MediaPlayer with duration: {}", seekDuration);
            mediaPlayer.seek(seekDuration);
            
            double newTime = mediaPlayer.getCurrentTime().toSeconds();
            logger.trace("Current playback time after seek: {} seconds", newTime);
            logger.info("Seeked from {} to {} seconds", currentTime, newTime);
            logger.debug("Seek operation completed successfully");
        } else {
            logger.warn("MediaPlayer is null, cannot seek");
            logger.debug("Attempted seek position: {} seconds, but no media player available", seconds);
        }
        
        logger.trace("seek() method exit");
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
