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
 * Uses AudioPlayerPool for simultaneous multi-track playback.
 * Supports up to 20 concurrent audio tracks.
 */
public class AudioService {
    
    private AudioPlayerPool playerPool;
    private double currentVolume = 1.0; // Current volume level for all tracks
    
    /**
     * Creates a new AudioService with multi-track playback enabled.
     */
    public AudioService() {
        this.playerPool = new AudioPlayerPool();
        this.playerPool.prewarm();
    }
    
    /**
     * Disposes of the player pool and releases resources.
     */
    public void dispose() {
        if (playerPool != null) {
            playerPool.dispose();
        }
    }
    
    /**
     * Plays an audio file on a new track.
     * Returns the track ID for managing the playback.
     * 
     * @param filePath Path to the audio file
     * @return Track ID for the new playback
     * @throws Exception if audio loading fails
     */
    public String playTrack(String filePath) throws Exception {
        AudioTrack track = playerPool.acquireTrack(filePath);
        track.play();
        return track.getTrackId();
    }
    
    /**
     * Plays an audio file on a new track with specified volume.
     * 
     * @param filePath Path to the audio file
     * @param volume Volume level (0.0 to 1.0)
     * @return Track ID for the new playback
     * @throws Exception if audio loading fails
     */
    public String playTrack(String filePath, double volume) throws Exception {
        AudioTrack track = playerPool.acquireTrack(filePath);
        track.setVolume(volume);
        track.play();
        return track.getTrackId();
    }
    
    /**
     * Gets a specific track by ID.
     * 
     * @param trackId The track ID
     * @return The AudioTrack, or null if not found
     */
    public AudioTrack getTrack(String trackId) {
        return playerPool.getTrack(trackId);
    }
    
    /**
     * Gets all currently active tracks.
     * 
     * @return List of active tracks
     */
    public List<AudioTrack> getActiveTracks() {
        return playerPool.getActiveTracks();
    }
    
    /**
     * Gets the count of active tracks.
     */
    public int getActiveTrackCount() {
        return playerPool.getActiveTrackCount();
    }
    
    /**
     * Stops a specific track by ID.
     * 
     * @param trackId The track ID to stop
     */
    public void stopTrack(String trackId) {
        playerPool.forceReleaseTrack(trackId);
    }
    
    /**
     * Stops all active tracks.
     */
    public void stopAllTracks() {
        playerPool.stopAll();
    }
    
    /**
     * Pauses all active tracks.
     */
    public void pauseAllTracks() {
        playerPool.pauseAll();
    }
    
    /**
     * Resumes all paused tracks.
     */
    public void resumeAllTracks() {
        playerPool.resumeAll();
    }
    
    /**
     * Sets the volume for all tracks.
     * Also stores the current volume for future tracks.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolumeAllTracks(double volume) {
        currentVolume = Math.max(0.0, Math.min(1.0, volume));
        playerPool.setVolumeAll(currentVolume);
    }
    
    /**
     * Culls unused tracks from the pool.
     * This helps manage memory by disposing of tracks that haven't been used recently.
     * Note: Automatic culling is enabled by default when the pool is prewarmed.
     * 
     * @return Number of tracks culled
     */
    public int cullUnusedTracks() {
        return playerPool.cullUnusedTracks();
    }
    
    /**
     * Enables automatic periodic culling of unused tracks.
     * When enabled, the pool automatically removes tracks that haven't been used recently.
     */
    public void enableAutoCulling() {
        playerPool.enableAutoCulling();
    }
    
    /**
     * Disables automatic periodic culling of unused tracks.
     */
    public void disableAutoCulling() {
        playerPool.disableAutoCulling();
    }
    
    /**
     * Checks if automatic culling is currently enabled.
     */
    public boolean isAutoCullEnabled() {
        return playerPool.isAutoCullEnabled();
    }
    
    /**
     * Gets the audio player pool.
     * Use with caution - direct pool manipulation can affect service state.
     * 
     * @return The AudioPlayerPool
     */
    public AudioPlayerPool getPlayerPool() {
        return playerPool;
    }
}
