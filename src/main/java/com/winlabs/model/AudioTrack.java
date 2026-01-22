package com.winlabs.model;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a single audio track in a multi-track playback system.
 * Each track has its own MediaPlayer and independent playback state.
 */
public class AudioTrack {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioTrack.class);
    private final String trackId;
    private MediaPlayer mediaPlayer;
    private PlaybackState state;
    private String filePath;
    private Consumer<AudioTrack> onEndListener;
    private Consumer<Duration> progressListener;
    private boolean isPooled;
    private long lastUsedTimestamp;
    
    public AudioTrack() {
        this.trackId = UUID.randomUUID().toString();
        this.state = PlaybackState.STOPPED;
        this.isPooled = false;
        this.lastUsedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the unique track identifier.
     */
    public String getTrackId() {
        return trackId;
    }
    
    /**
     * Gets the media player for this track.
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    
    /**
     * Sets the media player for this track.
     */
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        if (mediaPlayer != null) {
            setupMediaPlayerListeners();
        }
    }
    
    /**
     * Gets the current playback state.
     */
    public PlaybackState getState() {
        return state;
    }
    
    /**
     * Sets the playback state.
     */
    public void setState(PlaybackState state) {
        this.state = state;
        this.lastUsedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the file path of the loaded audio.
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Sets the file path of the loaded audio.
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Checks if this track is currently pooled (available for reuse).
     */
    public boolean isPooled() {
        return isPooled;
    }
    
    /**
     * Sets the pooled status of this track.
     */
    public void setPooled(boolean pooled) {
        this.isPooled = pooled;
    }
    
    /**
     * Gets the timestamp when this track was last used.
     */
    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }
    
    /**
     * Sets a listener for when playback ends.
     */
    public void setOnEndListener(Consumer<AudioTrack> listener) {
        this.onEndListener = listener;
    }
    
    /**
     * Gets the current end listener.
     */
    public Consumer<AudioTrack> getOnEndListener() {
        return onEndListener;
    }
    
    /**
     * Sets a listener for progress updates.
     */
    public void setProgressListener(Consumer<Duration> listener) {
        this.progressListener = listener;
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
            if (onEndListener != null) {
                onEndListener.accept(this);
            }
        });
        
        // Handle errors
        mediaPlayer.setOnError(() -> {
            logger.error("Media error on track {}: {}", trackId, 
                       mediaPlayer.getError().getMessage());
            setState(PlaybackState.STOPPED);
        });
    }
    
    /**
     * Starts or resumes audio playback.
     */
    public void play() {
        if (mediaPlayer == null) {
            throw new IllegalStateException("No audio loaded on track " + trackId);
        }
        mediaPlayer.play();
        setState(PlaybackState.PLAYING);
    }
    
    /**
     * Pauses audio playback.
     */
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            setState(PlaybackState.PAUSED);
        }
    }
    
    /**
     * Stops audio playback and returns to the beginning.
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO);
            setState(PlaybackState.STOPPED);
        }
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
     * Checks if audio is currently playing.
     */
    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }
    
    /**
     * Disposes of the media player and releases resources.
     */
    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        filePath = null;
        setState(PlaybackState.STOPPED);
    }
    
    /**
     * Resets the track for reuse from the pool.
     */
    public void reset() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO);
        }
        setState(PlaybackState.STOPPED);
        this.lastUsedTimestamp = System.currentTimeMillis();
    }
}
