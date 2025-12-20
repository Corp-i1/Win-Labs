# Testing Guide

This document describes Win-Labs' testing strategy, how to run tests, and how to write new tests.

## Testing Philosophy

- **100% pass rate required** before merging to master
- Tests should be fast, isolated, and deterministic
- Integration tests validate end-to-end workflows
- Unit tests validate individual component behavior
- Test coverage is important, but meaningful tests matter more than 100% coverage

## Test Structure

```
src/test/java/com/winlabs/
├── model/                    # Model tests
│   ├── AudioTrackTest.java
│   ├── CueTest.java
│   └── PlaylistTest.java
├── service/                  # Service tests
│   ├── AudioServiceTest.java
│   ├── AudioServiceMultiTrackTest.java
│   ├── AudioPlayerPoolTest.java
│   ├── FileSystemServiceTest.java
│   └── PlaylistServiceTest.java
├── util/                     # Utility tests
│   ├── PathUtilTest.java
│   └── TimeUtilTest.java
├── view/                     # View tests
│   └── components/
│       ├── BrowserFileViewTest.java
│       ├── FileViewTest.java
│       └── TreeFileViewTest.java
└── integration/              # Integration tests
    └── PlaylistIntegrationTest.java
```

## Running Tests

### All Tests

```bash
# Run all tests
.\gradlew test

# Run with verbose output
.\gradlew test --info

# Run with detailed logging
.\gradlew test --debug
```

### Specific Tests

```bash
# Run single test class
.\gradlew test --tests AudioServiceTest

# Run specific test method
.\gradlew test --tests AudioServiceTest.testPlayAudio

# Run tests matching pattern
.\gradlew test --tests "*MultiTrack*"
```

### Test Reports

After running tests, view HTML reports:
- **Location:** `build/reports/tests/test/index.html`
- **Open in browser:** Double-click the file or use a browser

**Report includes:**
- Pass/fail status for each test
- Execution time
- Error messages and stack traces
- Test output (System.out/System.err)

## Test Categories

### Unit Tests

Test individual classes in isolation.

**Location:** `src/test/java/com/winlabs/model/`, `service/`, `util/`

**Characteristics:**
- Fast execution (<100ms per test)
- No external dependencies (files, network, databases)
- Mock or stub dependencies
- Test one thing at a time

**Example:**
```java
@Test
void testCueNameProperty() {
    Cue cue = new Cue(1, "Test Cue", "/path/to/audio.mp3");
    cue.setName("New Name");
    assertEquals("New Name", cue.getName());
}
```

### Integration Tests

Test multiple components working together.

**Location:** `src/test/java/com/winlabs/integration/`

**Characteristics:**
- Slower execution (may take seconds)
- Tests full workflows (save → load → verify)
- Uses real file I/O with temporary files
- Tests interaction between components

**Example:**
```java
@Test
void testSaveAndLoadPlaylist() throws Exception {
    // Create playlist with cues
    Playlist playlist = new Playlist("Test Show");
    playlist.addCue(new Cue(1, "Cue 1", "audio1.mp3"));
    
    // Save to file
    Path tempFile = Files.createTempFile("test-playlist", ".json");
    PlaylistService.save(playlist, tempFile);
    
    // Load from file
    Playlist loaded = PlaylistService.load(tempFile);
    
    // Verify
    assertEquals("Test Show", loaded.getName());
    assertEquals(1, loaded.getCues().size());
}
```

### View Tests

Test UI components and behavior.

**Location:** `src/test/java/com/winlabs/view/`

**Characteristics:**
- Uses TestFX or manual JavaFX testing
- Tests event handling and UI updates
- Verifies JavaFX property bindings
- May require JavaFX toolkit initialization

**Example:**
```java
@Test
void testFileViewSelection() {
    // Assuming TestFX setup
    FileView fileView = new BrowserFileView(mockFileSystemService);
    fileView.selectFile(testPath);
    assertEquals(testPath, fileView.getSelectedFile());
}
```

## Writing Tests

### Test Naming Convention

```java
// Pattern: test[MethodName][Scenario][ExpectedResult]
@Test
void testPlayCueWithPreWaitStartsPlaybackAfterDelay() { }

@Test
void testSavePlaylistWithInvalidPathThrowsException() { }

@Test
void testGetVolumeReturnsDefaultWhenNotSet() { }
```

### Test Structure: Arrange-Act-Assert

```java
@Test
void testCueAutoFollowProperty() {
    // Arrange - Set up test data
    Cue cue = new Cue(1, "Test", "audio.mp3");
    
    // Act - Perform the action
    cue.setAutoFollow(true);
    
    // Assert - Verify the result
    assertTrue(cue.isAutoFollow());
}
```

### Using JUnit 5

Win-Labs uses **JUnit 5** (Jupiter).

**Common Annotations:**
```java
@Test                    // Marks a test method
@BeforeEach             // Runs before each test
@AfterEach              // Runs after each test
@BeforeAll              // Runs once before all tests (static)
@AfterAll               // Runs once after all tests (static)
@Disabled               // Temporarily disable a test
@DisplayName("Name")    // Custom test name in reports
```

**Assertions:**
```java
import static org.junit.jupiter.api.Assertions.*;

assertEquals(expected, actual);
assertNotEquals(unexpected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(object);
assertNotNull(object);
assertThrows(ExceptionClass.class, () -> { /* code */ });
assertTimeout(Duration.ofSeconds(1), () -> { /* code */ });
```

### Testing with Temporary Files

Always use temporary files for I/O tests:

```java
@BeforeEach
void setUp() throws IOException {
    testFile = Files.createTempFile("test-audio", ".mp3");
}

@AfterEach
void tearDown() throws IOException {
    if (testFile != null && Files.exists(testFile)) {
        Files.deleteIfExists(testFile);
    }
}

@Test
void testFileOperation() throws Exception {
    // Use testFile for testing
    PlaylistService.save(playlist, testFile);
    // ...
}
```

### Testing JavaFX Properties

Test property behavior and bindings:

```java
@Test
void testCueNamePropertyBinding() {
    Cue cue = new Cue(1, "Original", "audio.mp3");
    
    // Test property getter
    assertEquals("Original", cue.nameProperty().get());
    
    // Test property setter
    cue.nameProperty().set("New Name");
    assertEquals("New Name", cue.getName());
    
    // Test binding
    StringProperty boundProperty = new SimpleStringProperty();
    boundProperty.bind(cue.nameProperty());
    cue.setName("Updated");
    assertEquals("Updated", boundProperty.get());
}
```

### Mocking (When Needed)

Win-Labs tests primarily use real objects, but mocking is acceptable for:
- External dependencies (file systems, networks)
- Expensive operations (database queries)
- Non-deterministic behavior (current time, random values)

**Mockito Example (if added as dependency):**
```java
@Test
void testAudioServiceWithMockPool() {
    AudioPlayerPool mockPool = mock(AudioPlayerPool.class);
    AudioTrack mockTrack = mock(AudioTrack.class);
    
    when(mockPool.acquireTrack(anyString())).thenReturn(mockTrack);
    
    // Test behavior
    verify(mockPool).acquireTrack("audio.mp3");
}
```

## Test Coverage

### Viewing Coverage Reports

```bash
# Run tests with coverage
.\gradlew test jacocoTestReport

# Open report
# Location: build/reports/jacoco/test/html/index.html
```

### Coverage Guidelines

- **Models:** Aim for 90%+ (properties, getters, setters)
- **Services:** Aim for 80%+ (business logic, error handling)
- **Controllers:** Aim for 70%+ (complex state management)
- **Views:** Aim for 50%+ (UI logic, event handlers)

**Note:** Coverage is a guide, not a goal. Meaningful tests > high coverage.

## Testing Limitations

### MediaPlayer Complexity

JavaFX `MediaPlayer` requires native audio libraries and cannot run in headless environments.

### Manual Testing Checklist

Some scenarios require manual testing with real audio files:

- [ ] Play multiple overlapping cues
- [ ] Verify volume control works for all tracks
- [ ] Test pool exhaustion (play 20+ tracks)
- [ ] Verify automatic culling after 30 seconds
- [ ] Test batch operations (pause all, stop all)
- [ ] Verify pre-wait and post-wait timers
- [ ] Test auto-follow functionality
- [ ] Verify theme switching

Run manual tests with:
```bash
.\gradlew run
```

## Continuous Integration

Tests run automatically on:
- Every push to feature branches
- Every pull request to master
- Before merging to master

**CI Requirements:**
- All tests must pass (100% pass rate)
- No compilation errors or warnings
- Build must complete successfully

## Common Test Issues

### Flaky Tests (Non-Deterministic)

**Problem:** Test passes sometimes, fails other times.

**Causes:**
- Race conditions (threading issues)
- Time-dependent code (`System.currentTimeMillis()`)
- Unordered collections
- External state (files, network)

**Solutions:**
- Use deterministic test data
- Mock time-dependent operations
- Sort collections before assertions
- Isolate tests (no shared state)

### Slow Tests

**Problem:** Tests take too long to run.

**Causes:**
- Large file I/O operations
- Long `Thread.sleep()` calls
- Loading unnecessary resources
- Too many integration tests

**Solutions:**
- Use smaller test files
- Reduce wait times (e.g., 10ms instead of 1000ms)
- Load resources only when needed
- Balance unit vs integration tests

### Memory Leaks in Tests

**Problem:** Tests consume increasing memory over time.

**Causes:**
- MediaPlayer instances not disposed
- File handles not closed
- Event listeners not removed

**Solutions:**
```java
@AfterEach
void tearDown() {
    if (audioService != null) {
        audioService.dispose();  // Clean up MediaPlayer
    }
    
    if (testFile != null && Files.exists(testFile)) {
        Files.deleteIfExists(testFile);  // Clean up temp files
    }
}
```

## Best Practices

**Do:**
- Write tests before or alongside production code (TDD/BDD)
- Keep tests simple and focused (one assertion per test when possible)
- Use descriptive test names
- Clean up resources in `@AfterEach`
- Test edge cases and error conditions
- Make tests independent (no execution order dependencies)

**Don't:**
- Commit failing tests to master
- Use `Thread.sleep()` for synchronization
- Test implementation details (test behavior, not internals)
- Share state between tests
- Ignore test failures ("works on my machine")
- Write tests that depend on external resources (network, specific files)

## Need Help?

- **Architecture questions:** See [ARCHITECTURE.md](ARCHITECTURE.md)
- **JUnit 5 Docs:** https://junit.org/junit5/docs/current/user-guide/
- **GitHub Issues:** https://github.com/Corp-i1/Win-Labs/issues
