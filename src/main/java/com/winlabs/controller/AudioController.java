package com.winlabs.controller;


import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.service.AudioService;
import com.winlabs.service.PlatformIndicatorService;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Coordinates cue playback, timer-based waits, and auto-follow behavior.
 */
public class AudioController {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);
    
    private final AudioService audioService;
    private Cue currentCue;
    private String currentTrackId; // Track ID for current cue playback
    private Consumer<String> statusUpdateListener;
    private Consumer<PlaybackState> stateChangeListener;
    private Runnable onCueCompleteListener;
    
    private PauseTransition preWaitTimer;
    private PauseTransition postWaitTimer;
    private boolean error = false;
    
    public AudioController() {
        this.audioService = new AudioService(true); // Enable multi-track mode
        logger.info("AudioController initialized with multi-track mode enabled");
    }
    
    /**
     * Starts playback for a cue after validating the cue and honoring any pre-wait delay.
     */
    public void playCue(Cue cue) {
        if (cue == null) {
            logger.warn("Attempted to play null cue");
            updateStatus("No cue to play");
            return;
        }
        
        logger.info("Playing cue: {} ({})", cue.getNumber(), cue.getName());
        String filePath = cue.getFilePath();
        logger.debug("Cue file path: {}", filePath);
        
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("Cue {} has no audio file path", cue.getNumber());
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
        } catch (Exception exception) {
            updateStatus("Error playing cue: " + exception.getMessage());
        }
    }
    
    /**
     * Acquires the track, wires completion handling, and starts playback.
     */
    private void startPlayback(Cue cue, String filePath) {
        try {
            // Acquire track before setting up listeners to avoid race conditions
            var audioTrack = audioService.getPlayerPool().acquireTrack(filePath);
            currentTrackId = audioTrack.getTrackId();

            // Set up listener and start playback with consolidated error handling
            if (!setupTrackListener(cue, audioTrack)) {
                return;
            }
            if (!startAudioPlayback(cue, audioTrack)) {
                return;
            }

            // Notify state change and update status
            if (stateChangeListener != null) {
                stateChangeListener.accept(getState());
            }
            updateStatus("Playing: " + cue.getName());
        } catch (Exception exception) {
            handlePlaybackError(cue, exception);
        }
    }

    /**
     * Attaches completion handling to the acquired track and preserves the pool listener.
     */
    private boolean setupTrackListener(Cue cue, AudioTrack audioTrack) {
        try {
            // Store the pool's original listener to chain them
            var poolTrackEndListener = audioTrack.getOnEndListener();
            audioTrack.setOnEndListener(completedTrack -> {
                // Track has finished playing
                if (stateChangeListener != null) {
                    stateChangeListener.accept(getState());
                }
                handleCueComplete();
                // Call the pool's listener to properly release the track
                try {
                    if (poolTrackEndListener != null) {
                        poolTrackEndListener.accept(completedTrack);
                    }
                } catch (Exception e) {
                    logger.error("Error in pool listener for cue {}: {}", cue.getNumber(), e.getMessage(), e);
                    updateStatus("Error playing audio: " + e.getMessage());
                }
            });
            return true;
        } catch (Exception exception) {
            logger.error("Failed to set up listener for cue {}: {}", cue.getNumber(), exception.getMessage(), exception);
            updateStatus("Error setting up listener: " + exception.getMessage());
            return false;
        }
    }

    /**
     * Attempts to start playback on the acquired track.
     */
    private boolean startAudioPlayback(Cue cue, AudioTrack audioTrack) {
        try {
            audioTrack.play();
            return true;
        } catch (Exception exception) {
            logger.error("Error playing cue {}: {}", cue.getNumber(), exception.getMessage(), exception);
            updateStatus("Error playing audio: " + exception.getMessage());
            return false;
        }
    }

    /**
     * Converts playback startup failures into user-facing status updates.
     */
    private void handlePlaybackError(Cue cue, Exception exception) {
        logger.error("Error starting playback for cue {}: {}", cue.getNumber(), exception.getMessage(), exception);

        // Check for Linux-specific multimedia issues
        String errorMessage = exception.getMessage();
        if (PlatformIndicatorService.IS_LINUX && isLinuxMultimediaError(exception, errorMessage)) {
            String linuxErrorMsg = """
                                   Linux Audio Error: Missing multimedia libraries.
                                   Please install GStreamer libraries:
                                   • Fedora/RHEL: sudo dnf group upgrade multimedia sound-and-video --setopt="install_weak_deps=False" --exclude=PackageKit-gstreamer-plugin && sudo dnf install ffmpeg-libs gstreamer* --allowerasing 
                                   • You also need RPM Fusion: sudo dnf install https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm https://mirrors.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm
                                   Then restart Win-Labs.""";
            logger.info(linuxErrorMsg);
            updateStatus("Linux multimedia libraries missing. Install GStreamer codecs.");
        } else {
            updateStatus("Error loading audio: " + errorMessage);
        }
    }

    /**
     * Checks if an exception is a Linux multimedia library error.
     */
    private boolean isLinuxMultimediaError(Exception exception, String errorMessage) {
        return (errorMessage != null && (
                    errorMessage.contains("Could not create player") ||
                    errorMessage.contains("MediaException"))) ||
               (exception.getCause() != null && 
                exception.getCause().getMessage() != null && 
                exception.getCause().getMessage().contains("Could not create player"));
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
        
        logger.info("Cue completed: {} ({})", currentCue.getNumber(), currentCue.getName());
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

        // Notify state change
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }

        updateStatus("Paused");
        logger.info("Playback paused");
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

        // Notify state change
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }

        updateStatus("Resumed");
        logger.info("Playback resumed");
    }
    
    /**
     * Stops the current playback.
     */
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

        // Clear current playback state
        currentCue = null;
        currentTrackId = null;

        // Notify state change
        if (stateChangeListener != null) {
            stateChangeListener.accept(getState());
        }

        updateStatus("Stopped");
        logger.info("Playback stopped");
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
