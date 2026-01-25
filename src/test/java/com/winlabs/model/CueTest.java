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
    
    // Tests for factory methods
    
    @Test
    void testWithDurationFactoryMethod() {
        Cue cue2 = Cue.withDuration(5, "Audio Cue", "C:/audio.mp3", 120.5);
        assertEquals(5, cue2.getNumber());
        assertEquals("Audio Cue", cue2.getName());
        assertEquals("C:/audio.mp3", cue2.getFilePath());
        assertEquals(120.5, cue2.getDuration(), 0.001);
        assertEquals(0.0, cue2.getPreWait(), 0.001);
        assertEquals(0.0, cue2.getPostWait(), 0.001);
        assertFalse(cue2.isAutoFollow());
    }
    
    @Test
    void testWithTimingFactoryMethod() {
        Cue cue2 = Cue.withTiming(3, "Timed Cue", "C:/timed.mp3", 2.5, 3.0);
        assertEquals(3, cue2.getNumber());
        assertEquals("Timed Cue", cue2.getName());
        assertEquals("C:/timed.mp3", cue2.getFilePath());
        assertEquals(0.0, cue2.getDuration(), 0.001);
        assertEquals(2.5, cue2.getPreWait(), 0.001);
        assertEquals(3.0, cue2.getPostWait(), 0.001);
        assertFalse(cue2.isAutoFollow());
    }
    
    @Test
    void testWithTimingAndAutoFollowFactoryMethod() {
        Cue cue2 = Cue.withTimingAndAutoFollow(7, "Auto Cue", "C:/auto.mp3", 1.0, 2.0, true);
        assertEquals(7, cue2.getNumber());
        assertEquals("Auto Cue", cue2.getName());
        assertEquals("C:/auto.mp3", cue2.getFilePath());
        assertEquals(0.0, cue2.getDuration(), 0.001);
        assertEquals(1.0, cue2.getPreWait(), 0.001);
        assertEquals(2.0, cue2.getPostWait(), 0.001);
        assertTrue(cue2.isAutoFollow());
    }
    
    @Test
    void testFullConstructor() {
        Cue cue2 = new Cue(10, "Full Cue", "C:/full.mp3", 150.0, 5.0, 10.0, true);
        assertEquals(10, cue2.getNumber());
        assertEquals("Full Cue", cue2.getName());
        assertEquals("C:/full.mp3", cue2.getFilePath());
        assertEquals(150.0, cue2.getDuration(), 0.001);
        assertEquals(5.0, cue2.getPreWait(), 0.001);
        assertEquals(10.0, cue2.getPostWait(), 0.001);
        assertTrue(cue2.isAutoFollow());
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
            Cue.withDuration(1, "Test", "test.mp3", -10.0);
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
            Cue.withTiming(1, "Test", "test.mp3", -2.0, 1.0);
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
            Cue.withTiming(1, "Test", "test.mp3", 1.0, -3.0);
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
