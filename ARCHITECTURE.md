# Win-Labs Architecture

This document describes the technical architecture and design patterns used in Win-Labs.

## Architecture Pattern: JavaFX MVC

Win-Labs follows the Model-View-Controller pattern with JavaFX-specific adaptations for reactive UI updates.

### Key Design Principle: Property-Based Reactive UI

All models use **JavaFX Properties** (not plain fields) to enable automatic UI updates through data binding:

- `Cue` uses `IntegerProperty`, `StringProperty`, `DoubleProperty`, `BooleanProperty`
- `Playlist` uses `ObservableList<Cue>` which binds directly to `TableView`
- When modifying models, **always** use property methods: `setName()` not direct field access
- Example: `cue.nameProperty().bind(textField.textProperty())`

**Why Properties?**
JavaFX properties automatically notify listeners when values change. This enables the UI to update without manual refresh logic, following the reactive programming paradigm.

## Layer Responsibilities

### Model Layer (`src/main/java/com/winlabs/model/`)
Pure data containers with JavaFX properties, no business logic.

**Key Classes:**
- `Cue`: Represents a single cue with audio file path, timers, and playback settings
- `Playlist`: Contains an observable list of cues
- `AudioTrack`: Individual audio track for multi-track playback (has MediaPlayer reference)
- `PlaybackState`: Enum for audio states (STOPPED, PLAYING, PAUSED)

**Rules:**
- Only JavaFX properties (no plain fields for UI-bound data)
- No service calls or business logic
- No file I/O or external dependencies

### Service Layer (`src/main/java/com/winlabs/service/`)
Stateless operations for file I/O, audio playback, and file system access.

**Key Classes:**
- `AudioService`: Audio playback using JavaFX MediaPlayer (single-track and multi-track modes)
- `AudioPlayerPool`: Manages pool of MediaPlayer instances for multi-track playback
- `PlaylistService`: JSON serialization/deserialization via Gson
- `FileSystemService`: Recursive/non-recursive audio file listing

**Rules:**
- Stateless where possible (exceptions: AudioService maintains current playback state)
- No direct UI manipulation
- No model creation (return data, let controllers create models)

### Controller Layer (`src/main/java/com/winlabs/controller/`)
Orchestrates services and manages application state.

**Key Classes:**
- `AudioController`: Manages audio playback with pre-wait/post-wait timers and auto-follow logic

**Rules:**
- Coordinates between services and views
- Manages complex state (e.g., timer sequences)
- No direct file I/O (delegate to services)
- No UI component creation (delegate to views)

### View Layer (`src/main/java/com/winlabs/view/`)
JavaFX UI components and user interaction.

**Key Classes:**
- `MainWindow`: Primary application window (monolithic, planned for refactoring)
- `BrowserFileView`: File browser with directory navigation
- `TreeFileView`: Hierarchical tree-based file view
- `FileView`: Abstract base for file views

**Rules:**
- Only UI logic (layout, styling, event handling)
- Delegate business logic to controllers
- Bind to model properties for reactive updates

## Critical Flow: Audio Playback with Timers

```
User clicks "Go"
      ↓
MainWindow.handleGo()
      ↓
AudioController.playCue(cue)
      ↓
[Pre-Wait Timer?]
  Yes → PauseTransition (pre-wait seconds)
      ↓
AudioService.playTrack(filePath)  [Multi-track mode]
      ↓
AudioPlayerPool.acquireTrack(filePath)
      ↓
AudioTrack.play()
      ↓
JavaFX MediaPlayer plays audio
      ↓
[Audio finishes]
      ↓
[Post-Wait Timer?]
  Yes → PauseTransition (post-wait seconds)
      ↓
[Auto-Follow enabled?]
  Yes → Trigger next cue
```

### State Management

The `AudioController` manages three states via `PauseTransition` JavaFX animations:

1. **PRE_WAIT**: Countdown before playback starts (configurable per cue)
2. **PLAYING**: Active audio playback via JavaFX MediaPlayer
3. **POST_WAIT**: Countdown after playback before auto-follow (configurable per cue)

Each state is independent, allowing pre-wait timers to run before audio loads, and post-wait timers to run after audio completes.

## Multi-Track Playback System

### Components

**AudioTrack** (`model/AudioTrack.java`)
- Represents a single audio track with its own MediaPlayer
- UUID-based unique ID for tracking
- Independent playback state (STOPPED, PLAYING, PAUSED)
- Individual volume control
- Timestamp tracking for pool culling

**AudioPlayerPool** (`service/AudioPlayerPool.java`)
- Manages pool of reusable AudioTrack instances
- Pre-warms 5 tracks on initialization (zero-latency playback)
- Dynamic growth up to 20 tracks maximum
- Automatic culling of unused tracks after 30 seconds
- Thread-safe with `CopyOnWriteArrayList` and `ConcurrentHashMap`

**AudioService Multi-Track Mode** (`service/AudioService.java`)
- Enabled via `new AudioService(true)` constructor
- Single-track mode remains default for backward compatibility
- Provides `playTrack()` methods that return track IDs
- Batch operations: `stopAllTracks()`, `pauseAllTracks()`, etc.

### Design Rationale

**Why Object Pooling?**

Problem: MediaPlayer instantiation is expensive (~50-100ms per instance).

Solution: Pre-create and reuse MediaPlayer instances through pooling.

Trade-offs:
- Zero-latency playback for pre-warmed tracks (first 5 sounds)
- Reduced memory churn (reuse instead of create/destroy cycles)
- Predictable performance characteristics
- Upfront memory cost (~10-25 MB for 5 pre-warmed tracks)
- Additional code complexity (pool management logic)
- Hard limit on concurrent tracks

**Why 20 Track Maximum?**

Memory constraints:
- Each MediaPlayer: ~2-5 MB native memory
- 20 tracks: ~40-100 MB peak usage
- Reasonable for most modern systems

Practical considerations:
- Most shows use 5-10 simultaneous sounds
- 20 provides headroom for complex soundscapes
- Hard limit prevents runaway resource usage
- Pool exhaustion exception forces intentional design decisions

Future: May become configurable via constructor: `new AudioService(true, initialSize, maxSize)`

**Why 30-Second Culling Timeout?**

Memory management:
- Idle tracks consume memory without benefit
- Automatic cleanup reduces memory footprint
- Only culls **available** tracks (never active ones)

Why 30 seconds?
- Short enough to reclaim memory quickly during idle periods
- Long enough to avoid thrashing (constant create/destroy)
- Balances responsiveness vs. memory efficiency
- Pool regrows on demand if culled tracks are needed again

**Why Thread-Safe Collections?**

`CopyOnWriteArrayList` for available tracks:
- Read-heavy workload (checking for available tracks)
- Writes are rare (releasing tracks back to pool)
- Lock-free reads enable high performance

`ConcurrentHashMap` for active tracks:
- Frequent reads and writes (acquiring/releasing tracks)
- Fine-grained locking for better concurrency
- Safe for multi-threaded access without explicit synchronization

## File Paths & Audio Detection

- **Always** use `Path` objects (not `File` or `String`) for file system operations
- `PathUtil.isAudioFile()` validates extensions
- Supported formats: `.mp3`, `.wav`, `.aiff`, `.aac`, `.ogg`, `.flac`, `.m4a`, `.wma`
- `FileSystemService` provides recursive/non-recursive audio file listing

## JSON Persistence

`PlaylistService` serializes playlists to JSON via Gson:

**Format:**
```json
{
  "name": "Show Name",
  "version": "1.0",
  "cues": [
    {
      "number": 1,
      "name": "Intro Music",
      "filePath": "/path/to/audio.mp3",
      "duration": 180.5,
      "preWait": 2.0,
      "postWait": 1.0,
      "autoFollow": true
    }
  ]
}
```

**Important:** When adding properties to `Cue`, manually update `PlaylistService.save()` and `load()` methods. Gson serializes by field names, so property changes must be reflected in both methods.

## Theming System

Three CSS themes in `src/main/resources/css/`:
- `dark-theme.css` (default)
- `light-theme.css`
- `rainbow-theme.css`

Applied via:
```java
scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
```

Dark theme is default in `MainWindow.initializeUI()`.

## Time Formatting

Use `TimeUtil.formatSeconds()` for consistent time display:
- Input: `180.5` (seconds)
- Output: `"3:00.5"` (M:SS.f format)

## Common Pitfalls

1. **Don't bypass JavaFX properties**: Writing `cue.name = "..."` breaks UI binding. Always use `cue.setName("...")`
2. **MediaPlayer resource leaks**: Always call `audioService.dispose()` before loading new audio or shutting down
3. **Test file creation**: Put test resources in `src/test/resources/`, not `src/main/resources/`
4. **Gradle daemon issues**: If build hangs, use `--no-daemon` flag or run `.\gradlew --stop`

## Key Files Reference

- Entry point: `src/main/java/com/winlabs/Main.java`
- Main UI: `src/main/java/com/winlabs/view/MainWindow.java` (~600 lines, due for refactoring)
- Core models: `src/main/java/com/winlabs/model/Cue.java`, `Playlist.java`
- Playback orchestration: `src/main/java/com/winlabs/controller/AudioController.java`
- Audio services: `src/main/java/com/winlabs/service/AudioService.java`, `AudioPlayerPool.java`

## Future Architecture Improvements

See [FEATURE_IMPLEMENTATION_PLAN.md](FEATURE_IMPLEMENTATION_PLAN.md) for detailed roadmap.

**Planned refactorings:**
- Split `MainWindow` into smaller view components
- Extract cue table logic into dedicated controller
- Separate file browser into reusable component
- Implement plugin system for custom audio processors
