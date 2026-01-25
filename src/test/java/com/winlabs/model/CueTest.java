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
    
    // Tests for new constructor overloads
    
    @Test
    void testFourParameterConstructor() {
        Cue cue4 = new Cue(2, "Cue with Duration", "C:/music.mp3", 120.5);
        assertEquals(2, cue4.getNumber());
        assertEquals("Cue with Duration", cue4.getName());
        assertEquals("C:/music.mp3", cue4.getFilePath());
        assertEquals(120.5, cue4.getDuration(), 0.001);
        assertEquals(0.0, cue4.getPreWait(), 0.001);
        assertEquals(0.0, cue4.getPostWait(), 0.001);
        assertFalse(cue4.isAutoFollow());
    }
    
    @Test
    void testFiveParameterConstructor() {
        Cue cue5 = new Cue(3, "Cue with Timing", "C:/sound.mp3", 2.5, 3.0);
        assertEquals(3, cue5.getNumber());
        assertEquals("Cue with Timing", cue5.getName());
        assertEquals("C:/sound.mp3", cue5.getFilePath());
        assertEquals(0.0, cue5.getDuration(), 0.001);
        assertEquals(2.5, cue5.getPreWait(), 0.001);
        assertEquals(3.0, cue5.getPostWait(), 0.001);
        assertFalse(cue5.isAutoFollow());
    }
    
    @Test
    void testSixParameterConstructor() {
        Cue cue6 = new Cue(4, "Cue with AutoFollow", "C:/audio.mp3", 1.0, 2.0, true);
        assertEquals(4, cue6.getNumber());
        assertEquals("Cue with AutoFollow", cue6.getName());
        assertEquals("C:/audio.mp3", cue6.getFilePath());
        assertEquals(0.0, cue6.getDuration(), 0.001);
        assertEquals(1.0, cue6.getPreWait(), 0.001);
        assertEquals(2.0, cue6.getPostWait(), 0.001);
        assertTrue(cue6.isAutoFollow());
    }
    
    @Test
    void testSevenParameterConstructor() {
        Cue cue7 = new Cue(5, "Full Cue", "C:/full.mp3", 180.0, 5.0, 10.0, true);
        assertEquals(5, cue7.getNumber());
        assertEquals("Full Cue", cue7.getName());
        assertEquals("C:/full.mp3", cue7.getFilePath());
        assertEquals(180.0, cue7.getDuration(), 0.001);
        assertEquals(5.0, cue7.getPreWait(), 0.001);
        assertEquals(10.0, cue7.getPostWait(), 0.001);
        assertTrue(cue7.isAutoFollow());
    }
    
    // Tests for validation and error cases
    
    @Test
    void testNegativeNumberThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Cue(-1, "Test", "test.mp3");
        });
        assertTrue(exception.getMessage().contains("number must be non-negative"));
    }
    
    @Test
    void testSetNegativeNumberThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cue.setNumber(-5);
        });
        assertTrue(exception.getMessage().contains("number must be non-negative"));
    }
    
    @Test
    void testNegativeDurationThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Cue(1, "Test", "test.mp3", -10.0);
        });
        assertTrue(exception.getMessage().contains("duration must be non-negative"));
    }
    
    @Test
    void testSetNegativeDurationThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cue.setDuration(-1.5);
        });
        assertTrue(exception.getMessage().contains("duration must be non-negative"));
    }
    
    @Test
    void testNegativePreWaitThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Cue(1, "Test", "test.mp3", -2.0, 1.0);
        });
        assertTrue(exception.getMessage().contains("preWait must be non-negative"));
    }
    
    @Test
    void testSetNegativePreWaitThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cue.setPreWait(-0.5);
        });
        assertTrue(exception.getMessage().contains("preWait must be non-negative"));
    }
    
    @Test
    void testNegativePostWaitThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Cue(1, "Test", "test.mp3", 1.0, -3.0);
        });
        assertTrue(exception.getMessage().contains("postWait must be non-negative"));
    }
    
    @Test
    void testSetNegativePostWaitThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cue.setPostWait(-2.5);
        });
        assertTrue(exception.getMessage().contains("postWait must be non-negative"));
    }
    
    @Test
    void testNullNameThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Cue(1, null, "test.mp3");
        });
        assertTrue(exception.getMessage().contains("name cannot be null"));
    }
    
    @Test
    void testSetNullNameThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            cue.setName(null);
        });
        assertTrue(exception.getMessage().contains("name cannot be null"));
    }
    
    @Test
    void testNullFilePathThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Cue(1, "Test", null);
        });
        assertTrue(exception.getMessage().contains("filePath cannot be null"));
    }
    
    @Test
    void testSetNullFilePathThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            cue.setFilePath(null);
        });
        assertTrue(exception.getMessage().contains("filePath cannot be null"));
    }
}
