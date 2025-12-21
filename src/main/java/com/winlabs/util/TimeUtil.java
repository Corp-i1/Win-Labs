package com.winlabs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for time and duration formatting.
 */
public class TimeUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeUtil.class);
    
    /**
     * Formats seconds into MM:SS format.
     * Example: 125.5 seconds -> "02:05"
     */
    public static String formatTime(double seconds) {
        logger.trace("formatTime() method entry");
        logger.debug("Formatting time from {} seconds", seconds);
        logger.trace("Input parameter 'seconds' value: {}", seconds);
        logger.trace("Input parameter type: double");
        
        if (seconds < 0) {
            logger.warn("Negative seconds value received: {}", seconds);
            logger.debug("Returning default '00:00' for negative input");
            logger.trace("formatTime() method exit (negative case)");
            return "00:00";
        }
        logger.trace("Seconds value is non-negative, proceeding with formatting");
        
        logger.trace("Applying Math.floor to seconds: {}", seconds);
        int totalSeconds = (int) Math.floor(seconds);
        logger.debug("Total seconds after floor: {}", totalSeconds);
        logger.trace("Cast to int complete");
        
        logger.trace("Calculating minutes: {} / 60", totalSeconds);
        int minutes = totalSeconds / 60;
        logger.debug("Minutes calculated: {}", minutes);
        logger.trace("Division operation complete");
        
        logger.trace("Calculating remaining seconds: {} % 60", totalSeconds);
        int secs = totalSeconds % 60;
        logger.debug("Remaining seconds: {}", secs);
        logger.trace("Modulo operation complete");
        
        logger.trace("Formatting string with minutes={}, seconds={}", minutes, secs);
        String result = String.format("%02d:%02d", minutes, secs);
        logger.debug("Formatted time string: {}", result);
        logger.info("Time {} seconds formatted as {}", seconds, result);
        logger.trace("formatTime() method exit");
        return result;
    }
    
    /**
     * Formats seconds into HH:MM:SS format.
     * Example: 3665.0 seconds -> "01:01:05"
     */
    public static String formatTimeWithHours(double seconds) {
        if (seconds < 0) {
            return "00:00:00";
        }
        
        int totalSeconds = (int) Math.floor(seconds);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * Parses time string in MM:SS format to seconds.
     * Example: "02:05" -> 125.0
     */
    public static double parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0.0;
        }
        
        try {
            String[] parts = timeString.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60.0 + seconds;
            } else if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return hours * 3600.0 + minutes * 60.0 + seconds;
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
        
        return 0.0;
    }
    
    /**
     * Converts milliseconds to seconds.
     */
    public static double millisToSeconds(long millis) {
        return millis / 1000.0;
    }
    
    /**
     * Converts seconds to milliseconds.
     */
    public static long secondsToMillis(double seconds) {
        return (long) (seconds * 1000.0);
    }
}
