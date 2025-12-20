package com.winlabs.controller;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.service.AudioService;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for managing audio playback logic.
 * Handles play, pause, stop, and auto-follow functionality.
 */
public class AudioController {
    
    private final AudioService audioService;
    private Cue currentCue;
    private String currentTrackId; // Track ID for current cue playback
    private Consumer<String> statusUpdateListener;
    private Consumer<PlaybackState> stateChangeListener;
    private Runnable onCueCompleteListener;
    
    private PauseTransition preWaitTimer;
    private PauseTransition postWaitTimer;
    
    public AudioController() {
        this.audioService = new AudioService(true); // Enable multi-track mode
        setupAudioServiceListeners();
    }
    
    /**
     * Sets up listeners for the audio service.
     * In multi-track mode, listeners are set per-track in startPlayback.
     */
    private void setupAudioServiceListeners() {
        // Multi-track mode uses per-track listeners instead of service-level listeners
    }
    
    /**
     * Plays a cue.
     */
    public void playCue(Cue cue) {
        if (cue == null) {
            updateStatus("No cue to play");
            return;
        }
        
        currentCue = cue;
        String filePath = cue.getFilePath();
        
        if (filePath == null || filePath.isEmpty()) {
            updateStatus("Cue has no audio file");
            return;
        }
        
        try {
            // Check for pre-wait
            double preWait = cue.getPreWait();
            if (preWait > 0) {
                updateStatus(String.format("Pre-wait: %.1fs for %s", preWait, cue.getName()));
                startPreWait(preWait, () -> startPlayback(cue, filePath));
            } else {
                startPlayback(cue, filePath);
            }
        } catch (Exception e) {
            updateStatus("Error playing cue: " + e.getMessage());
        }
    }
    
    /**
     * Starts the actual audio playback.
     */
    private void startPlayback(Cue cue, String filePath) {
        try {
            // Get the track before playing to set up listeners
            // This avoids a race condition with very short audio files
            var track = audioService.getPlayerPool().acquireTrack(filePath);
            currentTrackId = track.getTrackId();
            
            // Set up completion listener for this track
            // Store the pool's original listener to chain them
            var poolListener = track.getOnEndListener();
            track.setOnEndListener(audioTrack -> {
                // Track has finished playing
                currentState = PlaybackState.STOPPED;
                if (stateChangeListener != null) {
                    stateChangeListener.accept(currentState);
                }
                handleCueComplete();
                
                // Call the pool's listener to properly release the track
                if (poolListener != null) {
                    poolListener.accept(audioTrack);
                }
            });
            
            // Now start playback
            track.play();
            
            if (stateChangeListener != null) {
                stateChangeListener.accept(getState());
            }
            
            updateStatus("Playing: " + cue.getName());
        } catch (Exception e) {
            updateStatus("Error loading audio: " + e.getMessage());
        }
    }
    
    /**
     * Starts the pre-wait timer.
     */
    private void startPreWait(double seconds, Runnable onComplete) {
        if (preWaitTimer != null) {
            preWaitTimer.stop();
        }
        
        preWaitTimer = new PauseTransition(Duration.seconds(seconds));
        preWaitTimer.setOnFinished(e -> onComplete.run());
        preWaitTimer.play();
    }
    
    /**
     * Handles cue completion and post-wait/auto-follow logic.
     */
    private void handleCueComplete() {
        if (currentCue == null) {
            return;
        }
        
        updateStatus("Cue complete: " + currentCue.getName());
        
        double postWait = currentCue.getPostWait();
        boolean autoFollow = currentCue.isAutoFollow();
        
        if (postWait > 0 && autoFollow) {
            // Wait, then trigger next cue
            updateStatus(String.format("Post-wait: %.1fs", postWait));
            startPostWait(postWait, () -> {
                if (onCueCompleteListener != null) {
                    onCueCompleteListener.run();
                }
            });
        } else if (autoFollow) {
            // No post-wait, trigger next cue immediately
            if (onCueCompleteListener != null) {
                onCueCompleteListener.run();
            }
        }
        
        currentCue = null;
    }
    
    /**
     * Starts the post-wait timer.
     */
    private void startPostWait(double seconds, Runnable onComplete) {
        if (postWaitTimer != null) {
            postWaitTimer.stop();
        }
        
        postWaitTimer = new PauseTransition(Duration.seconds(seconds));
        postWaitTimer.setOnFinished(e -> onComplete.run());
        postWaitTimer.play();
    }
    
    /**
     * Pauses the current playback.
     */
    public void pause() {
        audioService.pauseAllTracks();
        
        // Pause any active timers
        if (preWaitTimer != null) {
            preWaitTimer.pause();
        }
        if (postWaitTimer != null) {
            postWaitTimer.pause();
        }
        
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }
        
        updateStatus("Paused");
    }
    
    /**
     * Resumes playback.
     */
    public void resume() {
        audioService.resumeAllTracks();
        
        // Resume any paused timers
        if (preWaitTimer != null && preWaitTimer.getStatus() == javafx.animation.Animation.Status.PAUSED) {
            preWaitTimer.play();
        }
        if (postWaitTimer != null && postWaitTimer.getStatus() == javafx.animation.Animation.Status.PAUSED) {
            postWaitTimer.play();
        }
        
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }
        
        updateStatus("Resumed");
    }
    
    /**
     * Stops the current playback.
     */
    public void stop() {
        audioService.stopAllTracks();
        
        // Stop and clear timers
        if (preWaitTimer != null) {
            preWaitTimer.stop();
            preWaitTimer = null;
        }
        if (postWaitTimer != null) {
            postWaitTimer.stop();
            postWaitTimer = null;
        }
        
        currentCue = null;
        currentTrackId = null;
        
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }
        
        updateStatus("Stopped");
    }
    
    /**
     * Gets the current playback state.
     * In multi-track mode, this reflects the actual state of active tracks and running timers.
     */
    public PlaybackState getState() {
        // Get active tracks and their states
        List<AudioTrack> activeTracks = audioService.getActiveTracks();
        
        // If we have active tracks, determine state based on them
        if (!activeTracks.isEmpty()) {
            // If any track is playing, overall state is PLAYING
            boolean hasPlaying = activeTracks.stream()
                .anyMatch(track -> track.getState() == PlaybackState.PLAYING);
            if (hasPlaying) {
                return PlaybackState.PLAYING;
            }
            
            // If all tracks are paused (and at least one exists), state is PAUSED
            boolean allPaused = activeTracks.stream()
                .allMatch(track -> track.getState() == PlaybackState.PAUSED);
            if (allPaused) {
                return PlaybackState.PAUSED;
            }
            
            // Otherwise, tracks exist but are in other states (e.g., STOPPED)
            // Fall through to check timer states
        }
        
        // No active tracks or tracks are stopped - check timer states
        // Store timer statuses to avoid repeated method calls
        javafx.animation.Animation.Status preWaitStatus = 
            (preWaitTimer != null) ? preWaitTimer.getStatus() : null;
        javafx.animation.Animation.Status postWaitStatus = 
            (postWaitTimer != null) ? postWaitTimer.getStatus() : null;
        
        // Check if we're in a wait state (pre-wait or post-wait)
        if (preWaitStatus == javafx.animation.Animation.Status.RUNNING) {
            return PlaybackState.PRE_WAIT;
        }
        if (postWaitStatus == javafx.animation.Animation.Status.RUNNING) {
            return PlaybackState.POST_WAIT;
        }
        
        // If timers are paused, we're in a paused state
        if (preWaitStatus == javafx.animation.Animation.Status.PAUSED ||
            postWaitStatus == javafx.animation.Animation.Status.PAUSED) {
            return PlaybackState.PAUSED;
        }
        
        // No active tracks and no timers running - state is STOPPED
        return PlaybackState.STOPPED;
    }
    
    /**
     * Gets the current cue being played.
     */
    public Cue getCurrentCue() {
        return currentCue;
    }
    
    /**
     * Sets the volume (0.0 to 1.0).
     */
    public void setVolume(double volume) {
        audioService.setVolume(volume);
    }
    
    /**
     * Gets the current volume.
     */
    public double getVolume() {
        return audioService.getVolume();
    }
    
    /**
     * Gets the current playback time in seconds.
     */
    public double getCurrentTime() {
        return audioService.getCurrentTime();
    }
    
    /**
     * Gets the total duration in seconds.
     */
    public double getDuration() {
        return audioService.getDuration();
    }
    
    /**
     * Sets a listener for status updates.
     */
    public void setStatusUpdateListener(Consumer<String> listener) {
        this.statusUpdateListener = listener;
    }
    
    /**
     * Sets a listener for state changes.
     */
    public void setStateChangeListener(Consumer<PlaybackState> listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Sets a listener for when a cue completes (for auto-follow).
     */
    public void setOnCueCompleteListener(Runnable listener) {
        this.onCueCompleteListener = listener;
    }
    
    /**
     * Updates status and notifies listeners.
     */
    private void updateStatus(String message) {
        if (statusUpdateListener != null) {
            statusUpdateListener.accept(message);
        }
    }
    
    /**
     * Disposes of resources.
     */
    public void dispose() {
        stop();
        audioService.dispose();
    }
}
