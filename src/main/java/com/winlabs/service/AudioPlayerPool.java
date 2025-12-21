package com.winlabs.service;

import com.winlabs.model.AudioTrack;
import com.winlabs.model.PlaybackState;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages a pool of MediaPlayer instances for multi-track audio playback.
 * Handles pre-warming, pooling, and automatic culling of unused players
 * to minimize latency and resource usage.
 */
public class AudioPlayerPool {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioPlayerPool.class);
    
    private static final int DEFAULT_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final long CULL_TIMEOUT_MS = 30000; // 30 seconds
    private static final long CULL_INTERVAL_MS = 10000; // 10 seconds - how often to check
    
    private final CopyOnWriteArrayList<AudioTrack> availableTracks;
    private final ConcurrentHashMap<String, AudioTrack> activeTracks;
    private final int initialPoolSize;
    private final int maxPoolSize;
    private final ScheduledExecutorService cullScheduler;
    private volatile boolean autoCullEnabled;
    private volatile ScheduledFuture<?> cullTask;
    
    public AudioPlayerPool() {
        this(DEFAULT_POOL_SIZE, MAX_POOL_SIZE);
    }
    
    public AudioPlayerPool(int initialPoolSize, int maxPoolSize) {
        this.initialPoolSize = Math.max(1, initialPoolSize);
        this.maxPoolSize = Math.max(this.initialPoolSize, maxPoolSize);
        this.availableTracks = new CopyOnWriteArrayList<>();
        this.activeTracks = new ConcurrentHashMap<>();
        logger.info("AudioPlayerPool created: initialSize={}, maxSize={}", this.initialPoolSize, this.maxPoolSize);
        this.cullScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerPool-Culler");
            thread.setDaemon(true);
            return thread;
        });
        this.autoCullEnabled = false;
        this.cullTask = null;
    }
    
    /**
     * Pre-warms the pool by creating initial track instances.
     * This should be called during initialization to avoid delays on first playback.
     * Also starts automatic culling of unused tracks.
     */
    public void prewarm() {
        logger.trace("prewarm() method entry");
        logger.info("Pre-warming audio player pool with {} initial tracks", initialPoolSize);
        logger.debug("Starting track creation loop");
        
        for (int i = 0; i < initialPoolSize; i++) {
            logger.trace("Creating track {} of {}", i + 1, initialPoolSize);
            logger.debug("Instantiating new AudioTrack object (iteration {})", i);
            AudioTrack track = new AudioTrack();
            logger.trace("AudioTrack created with ID: {}", track.getTrackId());
            
            logger.debug("Setting track {} as pooled", track.getTrackId());
            track.setPooled(true);
            logger.trace("Track {} pooled status: {}", track.getTrackId(), track.isPooled());
            
            logger.debug("Adding track {} to available tracks list", track.getTrackId());
            availableTracks.add(track);
            logger.trace("Available tracks count after adding: {}", availableTracks.size());
            logger.debug("Track {} added successfully (iteration {} complete)", track.getTrackId(), i);
        }
        
        logger.info("Successfully created {} tracks for pool", initialPoolSize);
        logger.debug("Final available tracks count: {}", availableTracks.size());
        logger.trace("Track creation loop completed");
        
        // Enable automatic culling
        logger.debug("Enabling automatic track culling");
        logger.trace("Calling enableAutoCulling()");
        enableAutoCulling();
        logger.info("Automatic culling enabled for pool");
        logger.trace("prewarm() method exit");
    }
    
    /**
     * Enables automatic periodic culling of unused tracks.
     * When enabled, the pool will automatically remove tracks that haven't been
     * used for longer than CULL_TIMEOUT_MS.
     */
    public synchronized void enableAutoCulling() {
        logger.trace("enableAutoCulling() method entry (synchronized)");
        logger.debug("Attempting to enable automatic culling");
        logger.trace("Current autoCullEnabled status: {}", autoCullEnabled);
        
        if (!autoCullEnabled) {
            logger.info("Auto-culling is disabled, enabling it now");
            logger.debug("Setting autoCullEnabled flag to true");
            autoCullEnabled = true;
            logger.trace("autoCullEnabled flag set to: {}", autoCullEnabled);
            
            logger.debug("Scheduling periodic cull task with interval: {}ms", CULL_INTERVAL_MS);
            logger.trace("Timeout threshold: {}ms", CULL_TIMEOUT_MS);
            logger.trace("Calling scheduleWithFixedDelay on cullScheduler");
            cullTask = cullScheduler.scheduleWithFixedDelay(
                this::cullUnusedTracksInternal,
                CULL_INTERVAL_MS,
                CULL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
            logger.trace("Scheduled cull task: {}", cullTask);
            logger.info("Automatic culling enabled successfully");
            logger.debug("Cull task will run every {}ms with timeout {}ms", CULL_INTERVAL_MS, CULL_TIMEOUT_MS);
        } else {
            logger.debug("Auto-culling is already enabled, skipping");
            logger.trace("Current cull task: {}", cullTask);
        }
        
        logger.trace("enableAutoCulling() method exit");
    }
    
    /**
     * Disables automatic periodic culling of unused tracks.
     */
    public synchronized void disableAutoCulling() {
        logger.trace("disableAutoCulling() method entry (synchronized)");
        logger.debug("Attempting to disable automatic culling");
        logger.trace("Current autoCullEnabled status: {}", autoCullEnabled);
        
        if (autoCullEnabled) {
            logger.info("Auto-culling is enabled, disabling it now");
            logger.debug("Setting autoCullEnabled flag to false");
            autoCullEnabled = false;
            logger.trace("autoCullEnabled flag set to: {}", autoCullEnabled);
            
            logger.trace("Checking if cull task exists");
            if (cullTask != null) {
                logger.debug("Cull task exists, cancelling it. Task: {}", cullTask);
                logger.trace("Calling cancel(false) on cull task");
                boolean cancelled = cullTask.cancel(false);
                logger.trace("Cull task cancellation result: {}", cancelled);
                logger.debug("Clearing cull task reference");
                cullTask = null;
                logger.trace("Cull task set to null");
                logger.info("Cull task cancelled and cleared successfully");
            } else {
                logger.warn("Cull task was null despite auto-culling being enabled");
            }
            
            logger.info("Automatic culling disabled successfully");
        } else {
            logger.debug("Auto-culling is already disabled, skipping");
        }
        
        logger.trace("disableAutoCulling() method exit");
    }
    
    /**
     * Checks if automatic culling is currently enabled.
     */
    public boolean isAutoCullEnabled() {
        return autoCullEnabled;
    }
    
    /**
     * Acquires an audio track from the pool for playback.
     * Creates a new track if the pool is empty and under max size.
     * 
     * @param filePath Path to the audio file to load
     * @return An AudioTrack ready for playback
     * @throws Exception if the audio file cannot be loaded
     */
    public AudioTrack acquireTrack(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        
        // Try to get an available track from the pool
        AudioTrack track = null;
        if (!availableTracks.isEmpty()) {
            track = availableTracks.remove(0);
        } else if (getTotalTrackCount() < maxPoolSize) {
            // Create a new track if under max size
            track = new AudioTrack();
        } else {
            throw new IllegalStateException(
                "Cannot acquire track: pool exhausted (max " + maxPoolSize + " tracks)");
        }
        
        // Load the audio file
        String mediaUrl = path.toUri().toString();
        Media media = new Media(mediaUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        
        track.setMediaPlayer(mediaPlayer);
        track.setFilePath(filePath);
        track.setPooled(false);
        
        // Set up listener to return track to pool when playback ends
        track.setOnEndListener(this::releaseTrack);
        
        // Add to active tracks
        activeTracks.put(track.getTrackId(), track);
        
        return track;
    }
    
    /**
     * Releases a track back to the pool after playback.
     * The track is reset and made available for reuse.
     * 
     * @param track The track to release
     */
    public void releaseTrack(AudioTrack track) {
        if (track == null) {
            return;
        }
        
        logger.debug("Releasing track: {}", track.getTrackId());
        // Remove from active tracks
        activeTracks.remove(track.getTrackId());
        
        // Reset the track
        track.reset();
        track.setPooled(true);
        
        // Return to pool if not over initial size
        if (availableTracks.size() < initialPoolSize) {
            availableTracks.add(track);
        } else {
            // Dispose if pool is full
            track.dispose();
        }
    }
    
    /**
     * Forces a track to be released back to the pool immediately.
     * Stops playback if active.
     * 
     * @param trackId The ID of the track to release
     */
    public void forceReleaseTrack(String trackId) {
        logger.debug("Force releasing track: {}", trackId);
        AudioTrack track = activeTracks.get(trackId);
        if (track != null) {
            track.stop();
            releaseTrack(track);
        }
    }
    
    /**
     * Gets a track by its ID.
     * 
     * @param trackId The track ID
     * @return The track, or null if not found
     */
    public AudioTrack getTrack(String trackId) {
        return activeTracks.get(trackId);
    }
    
    /**
     * Gets all currently active tracks.
     * 
     * @return List of active tracks
     */
    public List<AudioTrack> getActiveTracks() {
        return new ArrayList<>(activeTracks.values());
    }
    
    /**
     * Gets the count of active tracks.
     */
    public int getActiveTrackCount() {
        return activeTracks.size();
    }
    
    /**
     * Gets the count of available (pooled) tracks.
     */
    public int getAvailableTrackCount() {
        return availableTracks.size();
    }
    
    /**
     * Gets the total count of all tracks (active + available).
     */
    public int getTotalTrackCount() {
        return activeTracks.size() + availableTracks.size();
    }
    
    /**
     * Internal method for automatic culling called by the scheduler.
     * Culls unused tracks from the pool that have exceeded the timeout.
     * 
     * @return Number of tracks culled
     */
    private int cullUnusedTracksInternal() {
        long now = System.currentTimeMillis();
        int culled = 0;
        
        List<AudioTrack> tracksToRemove = availableTracks.stream()
            .filter(track -> (now - track.getLastUsedTimestamp()) > CULL_TIMEOUT_MS)
            .collect(Collectors.toList());
        
        for (AudioTrack track : tracksToRemove) {
            availableTracks.remove(track);
            track.dispose();
            culled++;
        }
        
        return culled;
    }
    
    /**
     * Culls unused tracks from the pool that have exceeded the timeout.
     * This helps manage memory by disposing of tracks that haven't been used recently.
     * This method can be called manually to trigger culling on demand.
     * 
     * @return Number of tracks culled
     */
    public int cullUnusedTracks() {
        return cullUnusedTracksInternal();
    }
    
    /**
     * Stops all active tracks immediately.
     */
    public void stopAll() {
        for (AudioTrack track : activeTracks.values()) {
            track.stop();
        }
    }
    
    /**
     * Pauses all active tracks.
     */
    public void pauseAll() {
        for (AudioTrack track : activeTracks.values()) {
            if (track.isPlaying()) {
                track.pause();
            }
        }
    }
    
    /**
     * Resumes all paused tracks.
     */
    public void resumeAll() {
        for (AudioTrack track : activeTracks.values()) {
            if (track.getState() == PlaybackState.PAUSED) {
                track.play();
            }
        }
    }
    
    /**
     * Sets the volume for all tracks.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolumeAll(double volume) {
        double clampedVolume = Math.max(0.0, Math.min(1.0, volume));
        
        for (AudioTrack track : activeTracks.values()) {
            track.setVolume(clampedVolume);
        }
        
        for (AudioTrack track : availableTracks) {
            track.setVolume(clampedVolume);
        }
    }
    
    /**
     * Disposes of all tracks and releases all resources.
     * The pool should not be used after calling this method.
     */
    public void dispose() {
        // Disable auto-culling and cancel the scheduled task
        disableAutoCulling();
        
        // Shutdown the cull scheduler
        cullScheduler.shutdown();
        try {
            if (!cullScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                cullScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cullScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Dispose active tracks
        for (AudioTrack track : activeTracks.values()) {
            track.dispose();
        }
        activeTracks.clear();
        
        // Dispose available tracks
        for (AudioTrack track : availableTracks) {
            track.dispose();
        }
        availableTracks.clear();
    }
}
