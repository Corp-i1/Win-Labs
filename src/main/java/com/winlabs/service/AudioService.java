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
     * Plays an audio file on a new track (multi-track mode only).
     * Returns the track ID for managing the playback.
     * <p>
     * This method requires the AudioService to be in multi-track mode. If called
     * when not in multi-track mode, an IllegalStateException will be thrown.
     * 
     * @param filePath Path to the audio file
     * @return Track ID for the new playback
     * @throws IllegalStateException if not in multi-track mode
     * @throws Exception if audio loading fails
     */
    public String playTrack(String filePath) throws Exception {
        if (!multiTrackMode) {
            throw new IllegalStateException("Multi-track playback requires multi-track mode");
        }
        
        AudioTrack track = playerPool.acquireTrack(filePath);
        track.play();
        return track.getTrackId();
    }
    
    /**
     * Plays an audio file on a new track with specified volume (multi-track mode only).
     * <p>
     * This method requires the AudioService to be in multi-track mode. If called
     * when not in multi-track mode, an IllegalStateException will be thrown.
     * 
     * @param filePath Path to the audio file
     * @param volume Volume level (0.0 to 1.0)
     * @return Track ID for the new playback
     * @throws IllegalStateException if not in multi-track mode
     * @throws Exception if audio loading fails
     */
    public String playTrack(String filePath, double volume) throws Exception {
        if (!multiTrackMode) {
            throw new IllegalStateException("Multi-track playback requires multi-track mode");
        }
        
        AudioTrack track = playerPool.acquireTrack(filePath);
        track.setVolume(volume);
        track.play();
        return track.getTrackId();
    }
    
    /**
     * Gets a specific track by ID (multi-track mode only).
     * 
     * @param trackId The track ID
     * @return The AudioTrack, or null if not found
     */
    public AudioTrack getTrack(String trackId) {
        if (!multiTrackMode) {
            return null;
        }
        return playerPool.getTrack(trackId);
    }
    
    /**
     * Gets all currently active tracks (multi-track mode only).
     * 
     * @return List of active tracks
     */
    public List<AudioTrack> getActiveTracks() {
        if (!multiTrackMode) {
            throw new IllegalStateException("Active tracks only available in multi-track mode");
        }
        return playerPool.getActiveTracks();
    }
    
    /**
     * Gets the count of active tracks (multi-track mode only).
     */
    public int getActiveTrackCount() {
        if (!multiTrackMode) {
            return 0;
        }
        return playerPool.getActiveTrackCount();
    }
    
    /**
     * Stops a specific track by ID (multi-track mode only).
     * 
     * @param trackId The track ID to stop
     */
    public void stopTrack(String trackId) {
        if (multiTrackMode) {
            playerPool.forceReleaseTrack(trackId);
        }
    }
    
    /**
     * Stops all active tracks (multi-track mode only).
     */
    public void stopAllTracks() {
        if (multiTrackMode) {
            playerPool.stopAll();
        }
    }
    
    /**
     * Pauses all active tracks (multi-track mode only).
     */
    public void pauseAllTracks() {
        if (multiTrackMode) {
            playerPool.pauseAll();
        }
    }
    
    /**
     * Resumes all paused tracks (multi-track mode only).
     */
    public void resumeAllTracks() {
        if (multiTrackMode) {
            playerPool.resumeAll();
        }
    }
    
    /**
     * Sets the volume for all tracks (multi-track mode only).
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolumeAllTracks(double volume) {
        if (multiTrackMode) {
            playerPool.setVolumeAll(volume);
        }
    }
    
    /**
     * Culls unused tracks from the pool (multi-track mode only).
     * This helps manage memory by disposing of tracks that haven't been used recently.
     * 
     * @return Number of tracks culled
     */
    public int cullUnusedTracks() {
        if (!multiTrackMode) {
            return 0;
        }
        return playerPool.cullUnusedTracks();
    }
    
    /**
     * Gets the audio player pool (multi-track mode only).
     * Use with caution - direct pool manipulation can affect service state.
     * 
     * @return The AudioPlayerPool, or null if not in multi-track mode
     */
    public AudioPlayerPool getPlayerPool() {
        return playerPool;
    }
}
