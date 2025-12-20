package com.winlabs.service;

import com.winlabs.model.PlaybackState;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

//TODO: HIGH PRIORITY: Multi track playback support, e.g multiple media players for overlapping sounds should instance objects as needed and cull them when not needed to calculate how many are needed and spin them up before hand to avoid large pauses between intended playback time and actual playbacks
//TODO: Add support for playlists, 
// TODO: gapless playback,
// TODO: equalizer settings, 
//TODO: Crossfade between tracks


/**
 * Service for audio playback using JavaFX MediaPlayer.
 * Handles loading, playing, pausing, and stopping audio files.
 */
public class AudioService {
    
    private MediaPlayer mediaPlayer;
    private PlaybackState state;
    private String currentFilePath;
    private Consumer<PlaybackState> stateChangeListener;
    private Consumer<Duration> progressListener;
    
    public AudioService() {
        this.state = PlaybackState.STOPPED;
        this.currentFilePath = null;
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
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        currentFilePath = null;
        setState(PlaybackState.STOPPED);
    }
}
