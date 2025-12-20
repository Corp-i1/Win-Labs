package com.winlabs.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {
    
    @Test
    void testFormatTime() {
        assertEquals("00:00", TimeUtil.formatTime(0.0));
        assertEquals("00:05", TimeUtil.formatTime(5.0));
        assertEquals("01:00", TimeUtil.formatTime(60.0));
        assertEquals("02:05", TimeUtil.formatTime(125.0));
        assertEquals("10:30", TimeUtil.formatTime(630.0));
        assertEquals("00:59", TimeUtil.formatTime(59.5));
    }
    
    @Test
    void testFormatTimeWithNegative() {
        assertEquals("00:00", TimeUtil.formatTime(-10.0));
    }
    
    @Test
    void testFormatTimeWithHours() {
        assertEquals("00:00:00", TimeUtil.formatTimeWithHours(0.0));
        assertEquals("00:00:30", TimeUtil.formatTimeWithHours(30.0));
        assertEquals("00:01:00", TimeUtil.formatTimeWithHours(60.0));
        assertEquals("01:00:00", TimeUtil.formatTimeWithHours(3600.0));
        assertEquals("01:01:05", TimeUtil.formatTimeWithHours(3665.0));
        assertEquals("12:30:45", TimeUtil.formatTimeWithHours(45045.0));
    }
    
    @Test
    void testFormatTimeWithHoursNegative() {
        assertEquals("00:00:00", TimeUtil.formatTimeWithHours(-100.0));
    }
    
    @Test
    void testParseTimeMMSS() {
        assertEquals(0.0, TimeUtil.parseTime("00:00"), 0.001);
        assertEquals(30.0, TimeUtil.parseTime("00:30"), 0.001);
        assertEquals(60.0, TimeUtil.parseTime("01:00"), 0.001);
        assertEquals(125.0, TimeUtil.parseTime("02:05"), 0.001);
        assertEquals(630.0, TimeUtil.parseTime("10:30"), 0.001);
    }
    
    @Test
    void testParseTimeHHMMSS() {
        assertEquals(0.0, TimeUtil.parseTime("00:00:00"), 0.001);
        assertEquals(30.0, TimeUtil.parseTime("00:00:30"), 0.001);
        assertEquals(3600.0, TimeUtil.parseTime("01:00:00"), 0.001);
        assertEquals(3665.0, TimeUtil.parseTime("01:01:05"), 0.001);
        assertEquals(45045.0, TimeUtil.parseTime("12:30:45"), 0.001);
    }
    
    @Test
    void testParseTimeInvalid() {
        assertEquals(0.0, TimeUtil.parseTime(null), 0.001);
        assertEquals(0.0, TimeUtil.parseTime(""), 0.001);
        assertEquals(0.0, TimeUtil.parseTime("invalid"), 0.001);
        assertEquals(0.0, TimeUtil.parseTime("12:34:56:78"), 0.001);
    }
    
    @Test
    void testMillisToSeconds() {
        assertEquals(0.0, TimeUtil.millisToSeconds(0), 0.001);
        assertEquals(1.0, TimeUtil.millisToSeconds(1000), 0.001);
        assertEquals(2.5, TimeUtil.millisToSeconds(2500), 0.001);
        assertEquals(60.0, TimeUtil.millisToSeconds(60000), 0.001);
    }
    
    @Test
    void testSecondsToMillis() {
        assertEquals(0L, TimeUtil.secondsToMillis(0.0));
        assertEquals(1000L, TimeUtil.secondsToMillis(1.0));
        assertEquals(2500L, TimeUtil.secondsToMillis(2.5));
        assertEquals(60000L, TimeUtil.secondsToMillis(60.0));
    }
    
    @Test
    void testRoundTripConversion() {
        double seconds = 125.5;
        long millis = TimeUtil.secondsToMillis(seconds);
        double convertedBack = TimeUtil.millisToSeconds(millis);
        assertEquals(seconds, convertedBack, 0.001);
    }
}
