
##  Current Implementation Status

###  Phase 0: Core Foundation (COMPLETED)
- [x] Basic workspace/playlist system
- [x] Cue list with ordered execution
- [x] Cue numbering (integer)
- [x] Cue names
- [x] Audio cue type (basic playback)
- [x] Auto-follow timing
- [x] Pre-wait & Post-wait delays
- [x] Audio file management (linking, detection)
- [x] Playlist save/load (JSON serialization)
- [x] Multi-channel audio support (via JavaFX MediaPlayer)
- [x] Basic GUI (file browser, cue table, playback controls)
- [x] Theme system (dark, light, rainbow)
- [x] Comprehensive test suite (68 tests, 100% pass rate)

---

##  Implementation Roadmap

### Phase 1: Enhanced Cue Management
**Goal:** Make the cue list more powerful and flexible

#### 1.1 Cue Inspector Panel
**Description:** Context-sensitive panel showing detailed properties for selected cue
- **Model Changes:**
  - Add `CueProperty` enum for all editable properties
  - Add validation rules to `Cue` class
- **View Changes:**
  - Create `CueInspectorView.java` (right sidebar panel)
  - Add property editors (TextField, Slider, CheckBox, ColorPicker)
  - Implement two-way binding with JavaFX properties
- **Controller Changes:**
  - Create `CueInspectorController.java`
  - Handle property updates and validation
- **Tests:**
  - `CueInspectorTest.java`: Property editing, validation, binding
  - `CuePropertyTest.java`: Enum tests, property metadata
- **Files to Create:**
  - `src/main/java/com/winlabs/view/CueInspectorView.java`
  - `src/main/java/com/winlabs/controller/CueInspectorController.java`
  - `src/main/java/com/winlabs/model/CueProperty.java`
  - `src/test/java/com/winlabs/view/CueInspectorTest.java`

#### 1.2 Advanced Cue Numbering
**Description:** Support decimal numbering (1.5, 2.1) for inserting cues
- **Model Changes:**
  - Change `Cue.number` from `int` to `double` or `String`
  - Add `CueNumbering` utility class with sorting/validation
- **Service Changes:**
  - Update `PlaylistService` for decimal number serialization
- **View Changes:**
  - Update table column to display decimals
  - Add "Insert Before/After" context menu
- **Tests:**
  - `CueNumberingTest.java`: Sorting, parsing, validation, insertion logic
  - Update `PlaylistTest.java` for decimal numbering
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/model/Playlist.java`
  - `src/main/java/com/winlabs/view/MainWindow.java`
- **Files to Create:**
  - `src/main/java/com/winlabs/util/CueNumbering.java`
  - `src/test/java/com/winlabs/util/CueNumberingTest.java`

#### 1.3 Cue Notes System
**Description:** Add notes/annotations to cues for operator instructions
- **Model Changes:**
  - Add `notes` StringProperty to `Cue`
  - Add `color` ObjectProperty<Color> for visual coding
- **View Changes:**
  - Add notes column to cue table (with truncation)
  - Add notes text area in inspector panel
  - Add color picker in inspector
- **Tests:**
  - Update `CueTest.java` with notes and color tests
  - Test notes serialization in `PlaylistServiceTest.java`
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/view/MainWindow.java`

#### 1.4 Cue Reordering
**Description:** Drag-and-drop to reorder cues in the list
- **View Changes:**
  - Implement TableRow drag handlers
  - Add visual drop indicator
  - Update selection after reorder
- **Controller Changes:**
  - Add `CueListController` for drag/drop logic
- **Tests:**
  - `CueListControllerTest.java`: Drag/drop simulation, order validation
  - Integration test for reorder + renumbering
- **Files to Create:**
  - `src/main/java/com/winlabs/controller/CueListController.java`
  - `src/test/java/com/winlabs/controller/CueListControllerTest.java`

---

### Phase 2: Group Cues & Advanced Timing
**Goal:** Implement hierarchical cue organization and execution modes

#### 2.1 Group Cue Type
**Description:** Container cue that holds child cues with execution modes
- **Model Changes:**
  - Create `GroupCue extends Cue` with `ObservableList<Cue> children`
  - Add `GroupMode` enum (SEQUENTIAL, SIMULTANEOUS, MANUAL)
  - Update `Playlist` to support nested structure
- **View Changes:**
  - Add tree structure to cue table (indent child cues)
  - Add expand/collapse icons
  - Show group mode in inspector
- **Controller Changes:**
  - Update `AudioController` to handle group execution
  - Implement recursive playback for sequential groups
  - Implement parallel playback for simultaneous groups
- **Tests:**
  - `GroupCueTest.java`: Add/remove children, mode switching
  - `GroupExecutionTest.java`: Sequential, simultaneous, manual modes
  - Integration test: Nested groups (group within group)
- **Files to Create:**
  - `src/main/java/com/winlabs/model/GroupCue.java`
  - `src/main/java/com/winlabs/model/GroupMode.java`
  - `src/test/java/com/winlabs/model/GroupCueTest.java`
  - `src/test/java/com/winlabs/integration/GroupExecutionTest.java`

#### 2.2 Auto-Continue Enhancement
**Description:** Improve auto-continue with configurable delay
- **Model Changes:**
  - Add `autoContinue` BooleanProperty to `Cue`
  - Add `autoContinueDelay` DoubleProperty (seconds)
- **Controller Changes:**
  - Update `AudioController` to check autoContinue flag
  - Add PauseTransition for delay
- **Tests:**
  - `AutoContinueTest.java`: Zero delay, fixed delay, combined with auto-follow
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/controller/AudioController.java`
- **Files to Create:**
  - `src/test/java/com/winlabs/integration/AutoContinueTest.java`

#### 2.3 Cue Targeting System
**Description:** Allow cues to reference and control other cues
- **Model Changes:**
  - Add `targetCueNumber` StringProperty to `Cue`
  - Create `CueAction` enum (START, STOP, PAUSE, RESUME, RESET)
- **Service Changes:**
  - Create `CueTargetingService` to resolve targets
- **Controller Changes:**
  - Update `AudioController` to execute actions on targets
- **Tests:**
  - `CueTargetingServiceTest.java`: Target resolution, validation
  - `CueTargetingIntegrationTest.java`: Start cue from another, stop target
- **Files to Create:**
  - `src/main/java/com/winlabs/model/CueAction.java`
  - `src/main/java/com/winlabs/service/CueTargetingService.java`
  - `src/test/java/com/winlabs/service/CueTargetingServiceTest.java`
  - `src/test/java/com/winlabs/integration/CueTargetingIntegrationTest.java`

---

### Phase 3: Transport Controls & Keyboard Shortcuts
**Goal:** Implement professional transport controls and hotkeys

#### 3.1 Go/Stop/Panic Controls
**Description:** Transport buttons for show control
- **View Changes:**
  - Enhance toolbar with larger Go/Stop/Panic buttons
  - Add visual state feedback (button colors, disabled states)
- **Controller Changes:**
  - Create `TransportController.java`
  - Implement Go (start selected or next), Stop (selected), Panic (all)
- **Model Changes:**
  - Add `PlaylistState` enum (IDLE, RUNNING, STOPPED)
- **Tests:**
  - `TransportControllerTest.java`: Go, Stop, Panic behavior
  - Integration test: Go from middle of playlist, Panic during playback
- **Files to Create:**
  - `src/main/java/com/winlabs/controller/TransportController.java`
  - `src/main/java/com/winlabs/model/PlaylistState.java`
  - `src/test/java/com/winlabs/controller/TransportControllerTest.java`

#### 3.2 Keyboard Shortcuts
**Description:** Configurable keyboard bindings for common actions
- **Model Changes:**
  - Create `KeyBinding` class with action/key mapping
  - Create `KeyBindingPreset` enum (DEFAULT, CUSTOM)
- **Service Changes:**
  - Create `KeyBindingService` for save/load preferences
- **View Changes:**
  - Add global KeyEvent handlers to MainWindow
  - Create `KeyBindingSettingsDialog` for customization
- **Default Bindings:**
  - Space: Go
  - Esc: Stop
  - Ctrl+.: Panic
  - Up/Down: Navigate cues
  - Enter: Edit selected cue
  - Delete: Remove selected cue
  - Ctrl+S: Save playlist
  - Ctrl+O: Open playlist
  - Ctrl+N: New playlist
  - Ctrl+Z/Y: Undo/Redo
- **Tests:**
  - `KeyBindingServiceTest.java`: Save/load, default bindings
  - `KeyBindingTest.java`: Key parsing, conflict detection
- **Files to Create:**
  - `src/main/java/com/winlabs/model/KeyBinding.java`
  - `src/main/java/com/winlabs/service/KeyBindingService.java`
  - `src/main/java/com/winlabs/view/KeyBindingSettingsDialog.java`
  - `src/test/java/com/winlabs/service/KeyBindingServiceTest.java`

#### 3.3 Undo/Redo System
**Description:** Full undo/redo for cue editing and workspace changes
- **Model Changes:**
  - Create `Command` interface with execute/undo methods
  - Create concrete commands: AddCueCommand, RemoveCueCommand, EditCueCommand, etc.
- **Service Changes:**
  - Create `CommandHistoryService` with undo/redo stacks
- **Controller Changes:**
  - Wrap all user actions in Command objects
- **Tests:**
  - `CommandHistoryServiceTest.java`: Undo/redo, history limits
  - `CommandTest.java`: Each command type's execute/undo
- **Files to Create:**
  - `src/main/java/com/winlabs/command/Command.java`
  - `src/main/java/com/winlabs/command/AddCueCommand.java`
  - `src/main/java/com/winlabs/command/RemoveCueCommand.java`
  - `src/main/java/com/winlabs/command/EditCueCommand.java`
  - `src/main/java/com/winlabs/service/CommandHistoryService.java`
  - `src/test/java/com/winlabs/service/CommandHistoryServiceTest.java`

---

### Phase 4: Advanced Audio Features
**Goal:** Professional audio control and routing

#### 4.1 Audio Levels (Gain Control)
**Description:** Per-cue volume with decibel precision
- **Model Changes:**
  - Add `gain` DoubleProperty to `Cue` (in dB, -60 to +12)
  - Add `gainLinear` calculated property (0.0 to 1.0)
- **Service Changes:**
  - Update `AudioService` to apply gain via MediaPlayer.setVolume()
  - Create `AudioMath` utility for dB ↔ linear conversion
- **View Changes:**
  - Add gain slider in inspector (-60 to +12 dB)
  - Add gain column in cue table
  - Add visual meter (optional)
- **Tests:**
  - `AudioMathTest.java`: dB conversion formulas
  - `AudioServiceTest.java`: Gain application during playback
- **Files to Create:**
  - `src/main/java/com/winlabs/util/AudioMath.java`
  - `src/test/java/com/winlabs/util/AudioMathTest.java`

#### 4.2 Fade Cues
**Description:** Dedicated cue type for fading audio over time
- **Model Changes:**
  - Create `FadeCue extends Cue` with target, startLevel, endLevel, duration
  - Add `FadeCurve` enum (LINEAR, EXPONENTIAL, LOGARITHMIC, S_CURVE)
- **Controller Changes:**
  - Update `AudioController` to handle fade execution
  - Use Timeline/KeyFrame for smooth interpolation
- **Tests:**
  - `FadeCueTest.java`: Properties, validation
  - `FadeExecutionTest.java`: Different curves, duration accuracy
- **Files to Create:**
  - `src/main/java/com/winlabs/model/FadeCue.java`
  - `src/main/java/com/winlabs/model/FadeCurve.java`
  - `src/test/java/com/winlabs/model/FadeCueTest.java`
  - `src/test/java/com/winlabs/integration/FadeExecutionTest.java`

#### 4.3 Audio Trimming
**Description:** Define in/out points without editing original file
- **Model Changes:**
  - Add `startTime` DoubleProperty to `Cue` (seconds into file)
  - Add `endTime` DoubleProperty (0 = play to end)
- **Service Changes:**
  - Update `AudioService` to use MediaPlayer.setStartTime()
  - Add duration recalculation based on trim points
- **View Changes:**
  - Add waveform view (optional, Phase 5)
  - Add start/end time editors in inspector
- **Tests:**
  - `AudioTrimmingTest.java`: Start/end time validation, duration calculation
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/service/AudioService.java`

#### 4.4 Looping
**Description:** Audio cues can loop indefinitely or N times
- **Model Changes:**
  - Add `loopEnabled` BooleanProperty to `Cue`
  - Add `loopCount` IntegerProperty (0 = infinite)
- **Service Changes:**
  - Update `AudioService` to handle MediaPlayer.setCycleCount()
- **Tests:**
  - `AudioLoopingTest.java`: Infinite loop, fixed count, loop + auto-follow interaction
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/service/AudioService.java`

#### 4.5 Audio Rate & Pitch Control
**Description:** Adjust playback speed and pitch
- **Model Changes:**
  - Add `rate` DoubleProperty to `Cue` (0.5 to 2.0)
  - Add `pitchShift` DoubleProperty (semitones, -12 to +12)
- **Service Changes:**
  - Use MediaPlayer.setRate() for speed
  - Note: JavaFX MediaPlayer doesn't support independent pitch shifting
  - Document limitation or explore alternative libraries (e.g., TarsosDSP)
- **Tests:**
  - `AudioRateTest.java`: Rate changes, validation
- **Files to Modify:**
  - `src/main/java/com/winlabs/model/Cue.java`
  - `src/main/java/com/winlabs/service/AudioService.java`

#### 4.6 Audio Metering
**Description:** Real-time visual level meters
- **View Changes:**
  - Create `AudioMeterView.java` (Canvas-based VU meter)
  - Add meters to main window (master output)
  - Add peak hold indicators
- **Service Changes:**
  - Create `AudioMeterService` to poll MediaPlayer audio spectrum
  - Note: JavaFX AudioSpectrumListener provides frequency data, not direct level
  - May need to calculate RMS from spectrum data
- **Tests:**
  - `AudioMeterServiceTest.java`: Level calculation, peak detection
- **Files to Create:**
  - `src/main/java/com/winlabs/view/AudioMeterView.java`
  - `src/main/java/com/winlabs/service/AudioMeterService.java`
  - `src/test/java/com/winlabs/service/AudioMeterServiceTest.java`

---

### Phase 5: Additional Cue Types
**Goal:** Expand beyond audio cues

#### 5.1 Memo Cue
**Description:** Text-only cue for notes/instructions (no audio playback)
- **Model Changes:**
  - Create `MemoCue extends Cue` with `text` StringProperty
- **View Changes:**
  - Display memo icon in cue table
  - Show large text area in inspector
  - No playback controls for memo cues
- **Tests:**
  - `MemoCueTest.java`: Text storage, serialization
- **Files to Create:**
  - `src/main/java/com/winlabs/model/MemoCue.java`
  - `src/test/java/com/winlabs/model/MemoCueTest.java`

#### 5.2 Stop Cue
**Description:** Cue that stops one or more target cues
- **Model Changes:**
  - Create `StopCue extends Cue` with target reference
  - Add `StopMode` enum (IMMEDIATE, FADE_OUT, AFTER_CURRENT)
- **Controller Changes:**
  - Handle stop action in AudioController
- **Tests:**
  - `StopCueTest.java`: Target validation, mode selection
  - Integration test: Stop cue during playback
- **Files to Create:**
  - `src/main/java/com/winlabs/model/StopCue.java`
  - `src/main/java/com/winlabs/model/StopMode.java`
  - `src/test/java/com/winlabs/model/StopCueTest.java`

#### 5.3 Start Cue
**Description:** Cue that triggers another cue (for branching)
- **Model Changes:**
  - Create `StartCue extends Cue` with target reference
- **Tests:**
  - `StartCueTest.java`: Target resolution
  - Integration test: Branching execution
- **Files to Create:**
  - `src/main/java/com/winlabs/model/StartCue.java`
  - `src/test/java/com/winlabs/model/StartCueTest.java`

#### 5.4 Load Cue
**Description:** Preload audio files into memory for instant playback
- **Model Changes:**
  - Create `LoadCue extends Cue` with targets
- **Service Changes:**
  - Add preloading to AudioService
- **Tests:**
  - `LoadCueTest.java`: Multiple targets, error handling
- **Files to Create:**
  - `src/main/java/com/winlabs/model/LoadCue.java`
  - `src/test/java/com/winlabs/model/LoadCueTest.java`

---

### Phase 6: Workspace Enhancements
**Goal:** Professional workspace management

#### 6.1 Workspace Settings Persistence
**Description:** Save window position, theme, recent files
- **Model Changes:**
  - Create `WorkspaceSettings` class
- **Service Changes:**
  - Create `SettingsService` (JSON storage in user home)
- **Features:**
  - Window size/position
  - Last opened playlist
  - Selected theme
  - Recent files list (up to 10)
  - Default directories
- **Tests:**
  - `SettingsServiceTest.java`: Save/load, defaults, migration
- **Files to Create:**
  - `src/main/java/com/winlabs/model/WorkspaceSettings.java`
  - `src/main/java/com/winlabs/service/SettingsService.java`
  - `src/test/java/com/winlabs/service/SettingsServiceTest.java`

#### 6.2 Workspace Snapshots
**Description:** Quick save/restore of workspace state
- **Model Changes:**
  - Create `WorkspaceSnapshot` class (immutable copy of playlist)
- **Service Changes:**
  - Add snapshot stack to PlaylistService
- **View Changes:**
  - Add "Create Snapshot" and "Restore Snapshot" menu items
- **Tests:**
  - `WorkspaceSnapshotTest.java`: Create, restore, multiple snapshots
- **Files to Create:**
  - `src/main/java/com/winlabs/model/WorkspaceSnapshot.java`
  - `src/test/java/com/winlabs/model/WorkspaceSnapshotTest.java`

#### 6.3 Recent Files Menu
**Description:** Quick access to recently opened playlists
- **View Changes:**
  - Add "Recent Files" submenu
  - Limit to 10 most recent
- **Service Changes:**
  - Update SettingsService to track recent files
- **Tests:**
  - Integration test: Open file → check recent menu

---

### Phase 7: Alternative Interfaces
**Goal:** Implement Cue Cart mode

#### 7.1 Cue Cart Mode
**Description:** Grid-based layout for non-linear triggering
- **View Changes:**
  - Create `CueCartView.java` (GridPane of buttons)
  - Each button represents a cue
  - Trigger on click (no sequential order)
- **Model Changes:**
  - Add `CartPosition` class (row, column)
  - Add cartPosition property to Cue
- **View Changes:**
  - Add "View → Cue Cart Mode" menu toggle
  - Switch between list and cart view
- **Tests:**
  - `CueCartViewTest.java`: Grid layout, button triggering
- **Files to Create:**
  - `src/main/java/com/winlabs/view/CueCartView.java`
  - `src/main/java/com/winlabs/model/CartPosition.java`
  - `src/test/java/com/winlabs/view/CueCartViewTest.java`

---

### Phase 8: External Control
**Goal:** Remote control integration

#### 8.1 OSC (Open Sound Control) Support
**Description:** Send/receive OSC messages for remote control
- **Dependencies:**
  - Add JavaOSC library to build.gradle
- **Service Changes:**
  - Create `OSCService` (OSC receiver/sender)
  - Define OSC address space (/cue/go, /cue/stop, etc.)
- **Features:**
  - Start/stop cues via OSC
  - Send cue state updates via OSC
- **Tests:**
  - `OSCServiceTest.java`: Message parsing, sending
  - Integration test: Trigger cue via OSC
- **Files to Create:**
  - `src/main/java/com/winlabs/service/OSCService.java`
  - `src/test/java/com/winlabs/service/OSCServiceTest.java`

#### 8.2 MIDI Support
**Description:** Trigger cues via MIDI notes/controllers
- **Dependencies:**
  - Use javax.sound.midi (built-in Java)
- **Service Changes:**
  - Create `MIDIService` (device detection, listener)
- **Model Changes:**
  - Add `midiTrigger` property to Cue (note number, channel)
- **Tests:**
  - `MIDIServiceTest.java`: Device detection, message handling
- **Files to Create:**
  - `src/main/java/com/winlabs/service/MIDIService.java`
  - `src/test/java/com/winlabs/service/MIDIServiceTest.java`

#### 8.3 Network Cues
**Description:** Cue type that sends HTTP/TCP/UDP messages
- **Model Changes:**
  - Create `NetworkCue extends Cue`
  - Add protocol, host, port, message properties
- **Service Changes:**
  - Create `NetworkService` for sending messages
- **Tests:**
  - `NetworkCueTest.java`: Message formatting, validation
  - Integration test: Send HTTP request
- **Files to Create:**
  - `src/main/java/com/winlabs/model/NetworkCue.java`
  - `src/main/java/com/winlabs/service/NetworkService.java`
  - `src/test/java/com/winlabs/service/NetworkCueTest.java`

---

### Phase 9: Advanced Features

#### 9.1 Timecode Sync
**Description:** Sync playback to external timecode (LTC, MTC, SMPTE)
- **Dependencies:** May require native libraries or specialized hardware
- **Research:** JavaFX doesn't natively support timecode

#### 9.2 Video Cues
**Description:** Play video files alongside audio
- **Uses:** JavaFX MediaPlayer also supports video
- **View Changes:** Separate video output window

#### 9.3 Lighting Cues
**Description:** Control DMX lighting via Art-Net or sACN
- **Dependencies:** Add DMX library (e.g., OLA Java bindings)

#### 9.4 Multi-Output Audio Routing
**Description:** Route cues to specific output channels
- **Limitation:** JavaFX MediaPlayer has limited routing control
- **Alternative:** Explore JACK Audio or CoreAudio bindings

#### 9.5 Audio Effects (EQ, Filters)
**Description:** Real-time audio processing
- **Limitation:** JavaFX doesn't provide DSP effects
- **Alternative:** Integrate TarsosDSP or JNAJack

#### 9.6 Scripting Cues
**Description:** Execute JavaScript or Python scripts from cues
- **Uses:** Embedded JavaScript engine (Nashorn/GraalVM)

---

## Testing Strategy

### Unit Tests
- **Every model class** should have a test class
- Test all getters, setters, and calculated properties
- Test serialization/deserialization
- Test validation logic

### Integration Tests
- Test interactions between services and controllers
- Test complete workflows (e.g., create playlist → add cues → save → load → play)
- Test error handling and recovery

### UI Tests
- Use TestFX for JavaFX UI testing
- Test user interactions (button clicks, drag/drop, keyboard shortcuts)
- Test UI state updates

### Performance Tests
- Test with large playlists (1000+ cues)
- Test simultaneous audio playback (10+ cues)
- Test memory usage with many preloaded files

---

## Learning Resources

### JavaFX
- [JavaFX Documentation](https://openjfx.io/)
- [ControlsFX](https://github.com/controlsfx/controlsfx) - Additional UI controls

### Audio Programming
- [javax.sound.sampled](https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/package-summary.html)
- [JavaFX MediaPlayer](https://openjfx.io/javadoc/21/javafx.media/javafx/scene/media/MediaPlayer.html)
- [TarsosDSP](https://github.com/JorenSix/TarsosDSP) - Advanced audio processing

### Show Control
- [OSC Specification](http://opensoundcontrol.stanford.edu/spec-1_0.html)
- [MIDI Specification](https://www.midi.org/specifications)
- [Art-Net Protocol](https://art-net.org.uk/)

---

## Development Guidelines

### Code Style
- Follow Java naming conventions (camelCase, PascalCase)
- Use JavaFX properties for UI-bindable fields
- Document public APIs with JavaDoc
- Keep classes focused (Single Responsibility Principle)

### Git Workflow
- Create feature branches for each phase (e.g., `feature/cue-inspector`)
- Write descriptive commit messages

### Testing Requirements
- All tests must pass before committing
- Write tests BEFORE implementing complex logic

---

## Priority Matrix

### Must Have
- Phase 1: Enhanced Cue Management
- Phase 2: Group Cues & Advanced Timing
- Phase 3: Transport Controls & Keyboard Shortcuts
- Phase 4: Advanced Audio Features (Levels, Fades, Trimming)

### Should Have
- Phase 5: Additional Cue Types
- Phase 6: Workspace Enhancements
- Phase 7: Cue Cart Mode

### Nice to Have
- Phase 8: External Control (OSC, MIDI)
- Phase 9: Advanced Features (Video, Lighting, Timecode)

---

## Current Architecture

```
src/main/java/com/winlabs/
├── model/              # Data models with JavaFX properties
│   ├── Cue.java
│   ├── Playlist.java
│   ├── PlaybackState.java
│   └── Theme.java
├── view/               # JavaFX UI components
│   ├── MainWindow.java
│   └── FileTreeView.java
├── controller/         # Business logic controllers
│   └── AudioController.java
├── service/            # Core services
│   ├── AudioService.java
│   ├── FileSystemService.java
│   └── PlaylistService.java
├── util/               # Helper utilities
│   ├── PathUtil.java
│   └── TimeUtil.java
└── Main.java           # Application entry point

src/test/java/com/winlabs/
├── model/              # Model unit tests
├── service/            # Service unit tests
├── util/               # Utility unit tests
└── integration/        # Integration tests
```
