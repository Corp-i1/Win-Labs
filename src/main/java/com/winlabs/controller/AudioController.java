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
                startWaitTimer(true, preWait, () -> startPlayback(cue, filePath));
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
                if (stateChangeListener != null) {
                    stateChangeListener.accept(getState());
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
     * Starts a wait timer (pre-wait or post-wait).
     * 
     * @param isPreWait Whether this is a pre-wait timer (vs post-wait)
     * @param seconds Duration in seconds
     * @param onComplete Callback when timer finishes
     */
    private void startWaitTimer(boolean isPreWait, double seconds, Runnable onComplete) {
        PauseTransition timer = isPreWait ? preWaitTimer : postWaitTimer;
        
        if (timer != null) {
            timer.stop();
        }
        
        timer = new PauseTransition(Duration.seconds(seconds));
        timer.setOnFinished(e -> onComplete.run());
        timer.play();
        
        if (isPreWait) {
            preWaitTimer = timer;
        } else {
            postWaitTimer = timer;
        }
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
            startWaitTimer(false, postWait, () -> {
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
     * Pauses the current playback.
     */
    public void pause() {
        if (audioService.getPlayerPool() != null) {
            audioService.getPlayerPool().pauseAll();
        }
        
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
        if (audioService.getPlayerPool() != null) {
            audioService.getPlayerPool().resumeAll();
        }
        
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
        if (audioService.getPlayerPool() != null) {
            audioService.getPlayerPool().stopAll();
        }
        
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
        List<AudioTrack> activeTracks = audioService.getPlayerPool() != null 
            ? audioService.getPlayerPool().getActiveTracks() 
            : List.of();
        
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
     * Gets the current track ID for the active cue playback.
     */
    public String getCurrentTrackId() {
        return currentTrackId;
    }
    
    /**
     * Gets the audio service (for direct access to pool and other operations).
     */
    public AudioService getAudioService() {
        return audioService;
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
