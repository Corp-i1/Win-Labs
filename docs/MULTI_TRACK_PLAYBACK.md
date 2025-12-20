# Multi-Track Playback Implementation

## Overview
This document describes the multi-track playback system implemented for Win-Labs, enabling simultaneous overlapping audio playback with efficient resource management.

## Architecture

### Components

#### 1. AudioTrack Model (`com.winlabs.model.AudioTrack`)
Represents a single audio track with its own MediaPlayer and independent playback state.

**Key Features:**
- Unique track ID (UUID-based)
- Independent playback state (STOPPED, PLAYING, PAUSED)
- Individual volume control
- Progress and end-of-playback listeners
- Timestamp tracking for pool culling
- Pooled status flag for reuse

**Methods:**
- `play()`, `pause()`, `stop()` - Standard playback controls
- `setVolume(double)`, `getVolume()` - Volume management
- `getCurrentTime()`, `getDuration()` - Time information
- `reset()` - Prepares track for pool reuse
- `dispose()` - Releases resources

#### 2. AudioPlayerPool Service (`com.winlabs.service.AudioPlayerPool`)
Manages a pool of MediaPlayer instances for efficient multi-track playback.

**Key Features:**
- Pre-warming: Creates initial pool of ready-to-use tracks
- Dynamic growth: Expands pool up to maximum size as needed
- Automatic culling: Removes unused tracks after timeout (30 seconds)
- Resource management: Returns tracks to pool after playback

**Configuration:**
- Default initial size: 5 tracks
- Maximum pool size: 20 tracks
- Cull timeout: 30 seconds

**Methods:**
- `prewarm()` - Initialize pool with default size
- `acquireTrack(String filePath)` - Get a track for playback
- `releaseTrack(AudioTrack)` - Return track to pool
- `forceReleaseTrack(String trackId)` - Stop and release specific track
- `cullUnusedTracks()` - Remove tracks exceeding timeout
- `stopAll()`, `pauseAll()`, `resumeAll()` - Batch operations
- `setVolumeAll(double)` - Set volume for all tracks

#### 3. AudioService Updates (`com.winlabs.service.AudioService`)
Enhanced to support both single-track (legacy) and multi-track modes.

**Backward Compatibility:**
- Default constructor creates single-track service (legacy mode)
- All existing single-track methods remain unchanged
- Existing code continues to work without modifications

**Multi-Track Mode:**
- Enabled via `new AudioService(true)`
- Pool is automatically pre-warmed on initialization
- Pool is disposed when service is disposed

**New Methods:**
- `isMultiTrackMode()` - Check current mode
- `playTrack(String filePath)` - Start new track playback
- `playTrack(String filePath, double volume)` - Play with specific volume
- `getTrack(String trackId)` - Get specific track by ID
- `getActiveTracks()` - List all active tracks
- `getActiveTrackCount()` - Count active tracks
- `stopTrack(String trackId)` - Stop specific track
- `stopAllTracks()`, `pauseAllTracks()`, `resumeAllTracks()` - Batch controls
- `setVolumeAllTracks(double)` - Volume for all tracks
- `cullUnusedTracks()` - Trigger pool cleanup
- `getPlayerPool()` - Direct pool access (advanced use)

## Usage Examples

### Basic Multi-Track Playback
```java
// Create service in multi-track mode
AudioService audioService = new AudioService(true);

// Play multiple overlapping sounds
String track1Id = audioService.playTrack("sound1.mp3");
String track2Id = audioService.playTrack("sound2.mp3", 0.8); // 80% volume
String track3Id = audioService.playTrack("sound3.mp3");

// Control specific tracks
AudioTrack track1 = audioService.getTrack(track1Id);
track1.setVolume(0.5);
track1.pause();

// Stop specific track
audioService.stopTrack(track2Id);

// Global controls
audioService.pauseAllTracks();
audioService.setVolumeAllTracks(0.7);
audioService.resumeAllTracks();

// Cleanup
audioService.dispose();
```

### Advanced Pool Management
```java
AudioService audioService = new AudioService(true);
AudioPlayerPool pool = audioService.getPlayerPool();

// Check pool status
int active = pool.getActiveTrackCount();
int available = pool.getAvailableTrackCount();
int total = pool.getTotalTrackCount();

System.out.println("Pool: " + active + " active, " + 
                   available + " available, " + 
                   total + " total");

// Manual culling (automatic after 30s idle)
int culled = audioService.cullUnusedTracks();
System.out.println("Culled " + culled + " unused tracks");

// Get all active tracks
List<AudioTrack> activeTracks = audioService.getActiveTracks();
for (AudioTrack track : activeTracks) {
    System.out.println("Track " + track.getTrackId() + 
                       " playing: " + track.getFilePath());
}
```

### Backward Compatibility (Single-Track Mode)
```java
// Old code continues to work unchanged
AudioService audioService = new AudioService(); // Single-track mode

audioService.loadAudio("music.mp3");
audioService.play();
audioService.setVolume(0.8);
audioService.pause();
audioService.stop();

audioService.dispose();
```

## Resource Management

### Pre-Warming
The pool pre-warms on initialization, creating 5 ready-to-use AudioTrack instances. This eliminates the delay when playing the first few sounds.

### Dynamic Growth
When all pre-warmed tracks are in use, the pool dynamically creates new tracks up to the maximum limit (20 by default). This prevents resource exhaustion while supporting high-concurrency scenarios.

### Automatic Culling
Tracks unused for 30+ seconds are automatically culled from the available pool, freeing memory. Active tracks are never culled until they finish playing and are released.

### Pool Exhaustion
If all 20 tracks are active, attempting to play another sound throws `IllegalStateException`. This prevents runaway resource usage. Consider:
- Stopping some tracks before playing new ones
- Increasing `maxPoolSize` if needed
- Reviewing audio design to reduce simultaneous playback

## Performance Considerations

### Latency
- Pre-warmed tracks: ~0ms latency (track already created)
- New track creation: ~50-100ms (MediaPlayer instantiation)
- Media loading: Variable based on file format and size

### Memory
- Each track holds a MediaPlayer instance
- Maximum memory: ~20 MediaPlayers at peak usage
- Automatic culling reduces idle memory footprint

### Thread Safety
- `AudioPlayerPool` uses thread-safe collections
- `CopyOnWriteArrayList` for available tracks
- `ConcurrentHashMap` for active tracks
- Safe for multi-threaded use

## Testing

### Test Coverage
- **AudioTrackTest**: 17 tests covering model behavior
- **AudioPlayerPoolTest**: 23 tests covering pool mechanics
- **AudioServiceMultiTrackTest**: 18 tests covering service integration

### Test Strategy
Tests focus on:
- Object lifecycle and state management
- Pool acquisition and release mechanics
- Backward compatibility
- Error handling
- Resource cleanup

**Note:** Tests use temporary empty files to avoid JavaFX MediaPlayer initialization complexity. Full integration testing with real audio files is recommended for production validation.

## Migration Guide

### Existing Code
No changes needed! Existing single-track code continues to work:
```java
AudioService service = new AudioService(); // Single-track mode
service.loadAudio("file.mp3");
service.play();
```

### New Multi-Track Code
Enable multi-track mode explicitly:
```java
AudioService service = new AudioService(true); // Multi-track mode
String trackId = service.playTrack("file.mp3");
```

### Gradual Migration
You can mix both approaches:
1. Use multi-track service for new features
2. Keep single-track logic for legacy use cases
3. Both work simultaneously in different service instances

## Future Enhancements

Potential improvements for the multi-track system:
- [ ] Configurable pool sizes via constructor
- [ ] Priority-based track allocation
- [ ] Track groups for batch operations
- [ ] Fade in/out during track transitions
- [ ] Automatic volume normalization
- [ ] Track chaining/sequencing
- [ ] Memory usage monitoring and adaptive pooling

## References

- [AudioTrack.java](../src/main/java/com/winlabs/model/AudioTrack.java)
- [AudioPlayerPool.java](../src/main/java/com/winlabs/service/AudioPlayerPool.java)
- [AudioService.java](../src/main/java/com/winlabs/service/AudioService.java)
- [Test Files](../src/test/java/com/winlabs/)
