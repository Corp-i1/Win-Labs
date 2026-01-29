package com.winlabs.controller;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.Cue;
import com.winlabs.model.PlaybackState;
import com.winlabs.service.AudioService;
import com.winlabs.service.PlatformIndicatorService;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for managing audio playback logic.
 * Handles play, pause, stop, and auto-follow functionality.
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
     * Plays a cue.
     */
    public void playCue(Cue cue) {
        if (cue == null) {
            logger.warn("Attempted to play null cue");
            updateStatus("No cue to play");
            return;
        }
        
        logger.info("Playing cue: {} ({})", cue.getNumber(), cue.getName());
        String filePath = cue.getFilePath();
        
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

            try {
            // Set up completion listener for this track
            // Store the pool's original listener to chain them
            var poolListener = track.getOnEndListener();
            track.setOnEndListener(audioTrack -> {
                // Track has finished playing
                if (stateChangeListener != null) {
                    stateChangeListener.accept(getState());
                }
                handleCueComplete();
                try{
                // Call the pool's listener to properly release the track
                if (poolListener != null) {
                    poolListener.accept(audioTrack);
                }}catch(Exception e){
                    logger.error("Error in pool listener for cue {}: {}", cue.getNumber(), e.getMessage(), e);
                    updateStatus("Error playing audio: " + e.getMessage());
                    error = true;
                    return;
                }
            });   
            } catch (Exception e) {
                logger.error("Failed to set up listner for cue {}: {}",cue.getNumber(),e.getMessage(), e);
                updateStatus("Error setting up listener: " + e.getMessage());
                error = true;
                return;
            }

            try{
            // Now start playback
            track.play();
            }catch(Exception e){
                        logger.error("Error playing cue {}: {}", cue.getNumber(), e.getMessage(), e);
                        updateStatus("Error playing audio: " + e.getMessage());
                        error = true;
                        return;
            }

            if (stateChangeListener != null) {
                stateChangeListener.accept(getState());
            }
            if (!error) {
            updateStatus("Playing: " + cue.getName());    
            error = false;
            return;
            }
        } catch (Exception e) {
            logger.error("Error starting Playback for cue {}: {}", cue.getNumber(), e.getMessage(), e);
            
            // Check for Linux-specific MediaException issues
            String errorMessage = e.getMessage();
            
            if (PlatformIndicatorService.IS_LINUX && (
                    errorMessage.contains("Could not create player") ||
                    errorMessage.contains("MediaException") ||
                    e.getCause() != null && e.getCause().getMessage() != null && 
                    e.getCause().getMessage().contains("Could not create player"))) {
                
                String linuxErrorMsg = "Linux Audio Error: Missing multimedia libraries. " +
                    "Please install GStreamer libraries:\n" +
                    "• Ubuntu/Debian: sudo apt install libavcodec-extra gstreamer1.0-libav gstreamer1.0-plugins-ugly gstreamer1.0-vaapi\n" +
                    "• Fedora/RHEL: sudo dnf install gstreamer1-plugins-bad-free gstreamer1-libav gstreamer1-vaapi --allowerasing\n" +
                    "• Arch: sudo pacman -S gst-libav gst-plugins-ugly gst-plugins-bad\n" +
                    "For Fedora, you may also need RPM Fusion: sudo dnf install https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm https://mirrors.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm\n" +
                    "Then restart Win-Labs.";
                logger.info(linuxErrorMsg);
                logger.error("Linux multimedia libraries missing. User should install GStreamer codecs.");
                updateStatus("Linux multimedia libraries missing. User should install GStreamer codecs.");
            } else {
                updateStatus("Error loading audio: " + errorMessage);
            }
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
        logger.trace("pause() method entry");
        logger.debug("Attempting to pause playback");
        logger.info("Pause requested by user");
        
        if (audioService.getPlayerPool() != null) {
            logger.debug("AudioPlayerPool is not null, proceeding to pause all tracks");
            logger.trace("Calling pauseAll() on player pool");
            audioService.getPlayerPool().pauseAll();
            logger.debug("All tracks paused successfully");
        } else {
            logger.warn("AudioPlayerPool is null, cannot pause tracks");
        }
        
        // Pause any active timers
        logger.trace("Checking pre-wait timer for pause");
        if (preWaitTimer != null) {
            logger.debug("Pre-wait timer exists, pausing it. Current status: {}", preWaitTimer.getStatus());
            preWaitTimer.pause();
            logger.trace("Pre-wait timer paused. New status: {}", preWaitTimer.getStatus());
        } else {
            logger.trace("No pre-wait timer to pause");
        }
        
        logger.trace("Checking post-wait timer for pause");
        if (postWaitTimer != null) {
            logger.debug("Post-wait timer exists, pausing it. Current status: {}", postWaitTimer.getStatus());
            postWaitTimer.pause();
            logger.trace("Post-wait timer paused. New status: {}", postWaitTimer.getStatus());
        } else {
            logger.trace("No post-wait timer to pause");
        }
        
        logger.trace("Checking for state change listener");
        if (stateChangeListener != null) {
            logger.debug("State change listener exists, notifying with new state");
            PlaybackState currentState = getState();
            logger.trace("Current playback state: {}", currentState);
            stateChangeListener.accept(currentState);
            logger.debug("State change listener notified successfully");
        } else {
            logger.warn("No state change listener registered");
        }
        
        logger.debug("Updating status to 'Paused'");
        updateStatus("Paused");
        logger.info("Playback paused successfully");
        logger.trace("pause() method exit");
    }
    
    /**
     * Resumes playback.
     */
    public void resume() {
        logger.trace("resume() method entry");
        logger.debug("Attempting to resume playback");
        logger.info("Resume requested by user");
        
        if (audioService.getPlayerPool() != null) {
            logger.debug("AudioPlayerPool is not null, proceeding to resume all tracks");
            logger.trace("Calling resumeAll() on player pool");
            audioService.getPlayerPool().resumeAll();
            logger.debug("All tracks resumed successfully");
        } else {
            logger.warn("AudioPlayerPool is null, cannot resume tracks");
        }
        
        // Resume any paused timers
        logger.trace("Checking pre-wait timer for resume");
        if (preWaitTimer != null && preWaitTimer.getStatus() == javafx.animation.Animation.Status.PAUSED) {
            logger.debug("Pre-wait timer is paused, resuming it");
            logger.trace("Pre-wait timer status before resume: {}", preWaitTimer.getStatus());
            preWaitTimer.play();
            logger.trace("Pre-wait timer status after resume: {}", preWaitTimer.getStatus());
            logger.debug("Pre-wait timer resumed successfully");
        } else if (preWaitTimer != null) {
            logger.trace("Pre-wait timer exists but not paused. Current status: {}", preWaitTimer.getStatus());
        } else {
            logger.trace("No pre-wait timer to resume");
        }
        
        logger.trace("Checking post-wait timer for resume");
        if (postWaitTimer != null && postWaitTimer.getStatus() == javafx.animation.Animation.Status.PAUSED) {
            logger.debug("Post-wait timer is paused, resuming it");
            logger.trace("Post-wait timer status before resume: {}", postWaitTimer.getStatus());
            postWaitTimer.play();
            logger.trace("Post-wait timer status after resume: {}", postWaitTimer.getStatus());
            logger.debug("Post-wait timer resumed successfully");
        } else if (postWaitTimer != null) {
            logger.trace("Post-wait timer exists but not paused. Current status: {}", postWaitTimer.getStatus());
        } else {
            logger.trace("No post-wait timer to resume");
        }
        
        logger.trace("Checking for state change listener");
        if (stateChangeListener != null) {
            logger.debug("State change listener exists, notifying with new state");
            PlaybackState currentState = getState();
            logger.trace("Current playback state: {}", currentState);
            stateChangeListener.accept(currentState);
            logger.debug("State change listener notified successfully");
        } else {
            logger.warn("No state change listener registered");
        }
        
        logger.debug("Updating status to 'Resumed'");
        updateStatus("Resumed");
        logger.info("Playback resumed successfully");
        logger.trace("resume() method exit");
    }
    
    /**
     * Stops the current playback.
     */
    public void stop() {
        logger.trace("stop() method entry");
        logger.debug("Attempting to stop playback");
        logger.info("Stop requested by user");
        logger.trace("Current cue before stop: {}", currentCue != null ? currentCue.getName() : "null");
        logger.trace("Current track ID before stop: {}", currentTrackId);
        
        if (audioService.getPlayerPool() != null) {
            logger.debug("AudioPlayerPool is not null, proceeding to stop all tracks");
            logger.trace("Calling stopAll() on player pool");
            audioService.getPlayerPool().stopAll();
            logger.debug("All tracks stopped successfully");
        } else {
            logger.warn("AudioPlayerPool is null, cannot stop tracks");
        }
        
        // Stop and clear timers
        logger.trace("Checking pre-wait timer for cleanup");
        if (preWaitTimer != null) {
            logger.debug("Pre-wait timer exists, stopping and clearing it. Current status: {}", preWaitTimer.getStatus());
            preWaitTimer.stop();
            logger.trace("Pre-wait timer stopped");
            preWaitTimer = null;
            logger.trace("Pre-wait timer reference cleared");
        } else {
            logger.trace("No pre-wait timer to cleanup");
        }
        
        logger.trace("Checking post-wait timer for cleanup");
        if (postWaitTimer != null) {
            logger.debug("Post-wait timer exists, stopping and clearing it. Current status: {}", postWaitTimer.getStatus());
            postWaitTimer.stop();
            logger.trace("Post-wait timer stopped");
            postWaitTimer = null;
            logger.trace("Post-wait timer reference cleared");
        } else {
            logger.trace("No post-wait timer to cleanup");
        }
        
        logger.debug("Clearing current cue reference");
        currentCue = null;
        logger.trace("Current cue set to null");
        
        logger.debug("Clearing current track ID");
        currentTrackId = null;
        logger.trace("Current track ID set to null");
        
        logger.trace("Checking for state change listener");
        if (stateChangeListener != null) {
            logger.debug("State change listener exists, notifying with new state");
            PlaybackState currentState = getState();
            logger.trace("Current playback state: {}", currentState);
            stateChangeListener.accept(currentState);
            logger.debug("State change listener notified successfully");
        } else {
            logger.warn("No state change listener registered");
        }
        
        logger.debug("Updating status to 'Stopped'");
        updateStatus("Stopped");
        logger.info("Playback stopped successfully");
        logger.trace("stop() method exit");
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
