package com.winlabs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistTest {
    
    private Playlist playlist;
    
    @BeforeEach
    void setUp() {
        playlist = new Playlist();
    }
    
    @Test
    void testDefaultConstructor() {
        assertEquals("Untitled Playlist", playlist.getName());
        assertNull(playlist.getFilePath());
        assertEquals(0, playlist.size());
    }
    
    @Test
    void testParameterizedConstructor() {
        Playlist namedPlaylist = new Playlist("My Playlist");
        assertEquals("My Playlist", namedPlaylist.getName());
    }
    
    @Test
    void testAddCue() {
        Cue cue = new Cue(1, "Test", "test.mp3");
        playlist.addCue(cue);
        assertEquals(1, playlist.size());
        assertEquals(cue, playlist.getCue(0));
    }
    
    @Test
    void testAddCueAtIndex() {
        Cue cue1 = new Cue(1, "First", "first.mp3");
        Cue cue2 = new Cue(2, "Second", "second.mp3");
        Cue cue3 = new Cue(3, "Third", "third.mp3");
        
        playlist.addCue(cue1);
        playlist.addCue(cue3);
        playlist.addCue(1, cue2);
        
        assertEquals(3, playlist.size());
        assertEquals(cue1, playlist.getCue(0));
        assertEquals(cue2, playlist.getCue(1));
        assertEquals(cue3, playlist.getCue(2));
    }
    
    @Test
    void testRemoveCue() {
        Cue cue = new Cue(1, "Test", "test.mp3");
        playlist.addCue(cue);
        assertEquals(1, playlist.size());
        
        playlist.removeCue(cue);
        assertEquals(0, playlist.size());
    }
    
    @Test
    void testRemoveCueByIndex() {
        Cue cue1 = new Cue(1, "First", "first.mp3");
        Cue cue2 = new Cue(2, "Second", "second.mp3");
        
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        
        playlist.removeCue(0);
        assertEquals(1, playlist.size());
        assertEquals(cue2, playlist.getCue(0));
    }
    
    @Test
    void testClear() {
        playlist.addCue(new Cue(1, "Test1", "test1.mp3"));
        playlist.addCue(new Cue(2, "Test2", "test2.mp3"));
        assertEquals(2, playlist.size());
        
        playlist.clear();
        assertEquals(0, playlist.size());
    }
    
    @Test
    void testRenumberCues() {
        Cue cue1 = new Cue(5, "First", "first.mp3");
        Cue cue2 = new Cue(10, "Second", "second.mp3");
        Cue cue3 = new Cue(15, "Third", "third.mp3");
        
        playlist.addCue(cue1);
        playlist.addCue(cue2);
        playlist.addCue(cue3);
        
        playlist.renumberCues();
        
        assertEquals(1, cue1.getNumber());
        assertEquals(2, cue2.getNumber());
        assertEquals(3, cue3.getNumber());
    }
    
    @Test
    void testSetAndGetName() {
        playlist.setName("My Show");
        assertEquals("My Show", playlist.getName());
    }
    
    @Test
    void testSetAndGetFilePath() {
        playlist.setFilePath("C:/playlists/show.json");
        assertEquals("C:/playlists/show.json", playlist.getFilePath());
    }
    
    @Test
    void testGetCues() {
        assertNotNull(playlist.getCues());
        assertTrue(playlist.getCues().isEmpty());
    }
    
    @Test
    void testToString() {
        playlist.setName("Test Playlist");
        playlist.addCue(new Cue(1, "Cue1", "cue1.mp3"));
        playlist.addCue(new Cue(2, "Cue2", "cue2.mp3"));
        
        String result = playlist.toString();
        assertTrue(result.contains("Test Playlist"));
        assertTrue(result.contains("2"));
    }
}
