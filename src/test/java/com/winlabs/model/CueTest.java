package com.winlabs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class CueTest {
    
    private Cue cue;
    
    @BeforeEach
    void setUp() {
        cue = new Cue();
    }
    
    @Test
    void testDefaultConstructor() {
        assertEquals(0, cue.getNumber());
        assertEquals("", cue.getName());
        assertEquals(0.0, cue.getDuration());
        assertEquals(0.0, cue.getPreWait());
        assertEquals(0.0, cue.getPostWait());
        assertFalse(cue.isAutoFollow());
        assertEquals("", cue.getFilePath());
    }
    
    @Test
    void testParameterizedConstructor() {
        Cue cue2 = new Cue(1, "Test Cue", "C:/test.mp3");
        assertEquals(1, cue2.getNumber());
        assertEquals("Test Cue", cue2.getName());
        assertEquals("C:/test.mp3", cue2.getFilePath());
    }
    
    @Test
    void testSetAndGetNumber() {
        cue.setNumber(5);
        assertEquals(5, cue.getNumber());
    }
    
    @Test
    void testSetAndGetName() {
        cue.setName("Opening Music");
        assertEquals("Opening Music", cue.getName());
    }
    
    @Test
    void testSetAndGetDuration() {
        cue.setDuration(180.5);
        assertEquals(180.5, cue.getDuration(), 0.001);
    }
    
    @Test
    void testSetAndGetPreWait() {
        cue.setPreWait(5.0);
        assertEquals(5.0, cue.getPreWait(), 0.001);
    }
    
    @Test
    void testSetAndGetPostWait() {
        cue.setPostWait(3.0);
        assertEquals(3.0, cue.getPostWait(), 0.001);
    }
    
    @Test
    void testSetAndGetAutoFollow() {
        cue.setAutoFollow(true);
        assertTrue(cue.isAutoFollow());
        cue.setAutoFollow(false);
        assertFalse(cue.isAutoFollow());
    }
    
    @Test
    void testSetAndGetFilePath() {
        String path = "C:/Music/song.mp3";
        cue.setFilePath(path);
        assertEquals(path, cue.getFilePath());
    }
    
    @Test
    void testProperties() {
        assertNotNull(cue.numberProperty());
        assertNotNull(cue.nameProperty());
        assertNotNull(cue.durationProperty());
        assertNotNull(cue.preWaitProperty());
        assertNotNull(cue.postWaitProperty());
        assertNotNull(cue.autoFollowProperty());
        assertNotNull(cue.filePathProperty());
    }
    
    @Test
    void testToString() {
        cue.setNumber(1);
        cue.setName("Test");
        cue.setFilePath("test.mp3");
        String result = cue.toString();
        assertTrue(result.contains("1"));
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("test.mp3"));
    }
}
