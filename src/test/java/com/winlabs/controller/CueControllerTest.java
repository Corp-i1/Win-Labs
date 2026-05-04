package com.winlabs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;

class CueControllerTest {

    @TempDir
    Path tempDir;

    private Playlist playlist;
    private TestAudioController audioController;
    private CueController cueController;

    @BeforeEach
    void setUp() {
        playlist = new Playlist();
        audioController = new TestAudioController();
        cueController = new CueController(audioController);
    }

    @Test
    void playDelegatesToAudioController() {
        Cue cue = new Cue(1, "Cue 1", "track.mp3");

        cueController.play(cue);

        assertSame(cue, audioController.lastPlayedCue);
        assertEquals(1, audioController.playCallCount);
    }

    @Test
    void duplicateCopiesCueAndRenumbersPlaylist() {
        Cue cue1 = new Cue(1, "First", "first.mp3", 2.5, 1.0, 0.5, true);
        Cue cue2 = new Cue(2, "Second", "second.mp3", 3.0, 0.0, 0.0, false);
        playlist.addCue(cue1);
        playlist.addCue(cue2);

        Cue copy = cueController.duplicate(playlist, cue1);

        assertNotNull(copy);
        assertEquals(3, playlist.size());
        assertSame(copy, playlist.getCue(1));
        assertEquals("First", copy.getName());
        assertEquals("first.mp3", copy.getFilePath());
        assertEquals(2.5, copy.getDuration());
        assertEquals(1.0, copy.getPreWait());
        assertEquals(0.5, copy.getPostWait());
        assertTrue(copy.isAutoFollow());
        assertEquals(1, playlist.getCue(0).getNumber());
        assertEquals(2, playlist.getCue(1).getNumber());
        assertEquals(3, playlist.getCue(2).getNumber());
    }

    @Test
    void deleteRemovesCueAndRenumbersPlaylist() {
        Cue cue1 = new Cue(10, "First", "first.mp3");
        Cue cue2 = new Cue(20, "Second", "second.mp3");
        Cue cue3 = new Cue(30, "Third", "third.mp3");
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);

        cueController.delete(playlist, cue2);

        assertEquals(2, playlist.size());
        assertSame(cue1, playlist.getCue(0));
        assertSame(cue3, playlist.getCue(1));
        assertEquals(1, playlist.getCue(0).getNumber());
        assertEquals(2, playlist.getCue(1).getNumber());
    }

    @Test
    void duplicateSelectedCopiesAllSelectedCuesInOrder() {
        Cue cue1 = new Cue(1, "First", "first.mp3", 1.0, 0.0, 0.0, false);
        Cue cue2 = new Cue(2, "Second", "second.mp3", 2.0, 0.5, 0.0, true);
        Cue cue3 = new Cue(3, "Third", "third.mp3", 3.0, 0.0, 1.0, false);
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);

        List<Cue> duplicates = cueController.duplicateSelected(playlist, List.of(cue1, cue3));

        assertEquals(2, duplicates.size());
        assertEquals(5, playlist.size());
        assertSame(duplicates.get(0), playlist.getCue(1));
        assertSame(duplicates.get(1), playlist.getCue(4));
        assertEquals(1, playlist.getCue(0).getNumber());
        assertEquals(2, playlist.getCue(1).getNumber());
        assertEquals(3, playlist.getCue(2).getNumber());
        assertEquals(4, playlist.getCue(3).getNumber());
        assertEquals(5, playlist.getCue(4).getNumber());
    }

    @Test
    void deleteSelectedRemovesAllSelectedCuesAndRenumbersPlaylist() {
        Cue cue1 = new Cue(10, "First", "first.mp3");
        Cue cue2 = new Cue(20, "Second", "second.mp3");
        Cue cue3 = new Cue(30, "Third", "third.mp3");
        Cue cue4 = new Cue(40, "Fourth", "fourth.mp3");
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);
        playlist.addCue(cue4);

        cueController.deleteSelected(playlist, List.of(cue2, cue4));

        assertEquals(2, playlist.size());
        assertSame(cue1, playlist.getCue(0));
        assertSame(cue3, playlist.getCue(1));
        assertEquals(1, playlist.getCue(0).getNumber());
        assertEquals(2, playlist.getCue(1).getNumber());
    }

    @Test
    void renameUpdatesCueName() {
        Cue cue = new Cue(1, "Old Name", "track.mp3");

        cueController.rename(cue, "New Name");

        assertEquals("New Name", cue.getName());
    }

    @Test
    void changeFileAcceptsSupportedAudioFile() throws Exception {
        Cue cue = new Cue(1, "Cue", "old.mp3");
        Path audioFile = Files.createFile(tempDir.resolve("new-track.wav"));

        boolean changed = cueController.changeFile(cue, audioFile);

        assertTrue(changed);
        assertEquals(audioFile.toString(), cue.getFilePath());
    }

    @Test
    void changeFileRejectsUnsupportedFile() throws Exception {
        Cue cue = new Cue(1, "Cue", "old.mp3");
        Path textFile = Files.createFile(tempDir.resolve("notes.txt"));

        boolean changed = cueController.changeFile(cue, textFile);

        assertFalse(changed);
        assertEquals("old.mp3", cue.getFilePath());
    }

    private static final class TestAudioController extends AudioController {
        private Cue lastPlayedCue;
        private int playCallCount;

        @Override
        public void playCue(Cue cue) {
            lastPlayedCue = cue;
            playCallCount++;
        }
    }
}
