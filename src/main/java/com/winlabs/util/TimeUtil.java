package com.winlabs.util;

/**
 * Utility class for time and duration formatting.
 */
public class TimeUtil {
    
    /**
     * Formats seconds into MM:SS format.
     * Example: 125.5 seconds -> "02:05"
     */
    public static String formatTime(double seconds) {
        if (seconds < 0) {
            return "00:00";
        }
        
        int totalSeconds = (int) Math.floor(seconds);
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        
        return String.format("%02d:%02d", minutes, secs);
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
