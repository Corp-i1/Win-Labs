package com.winlabs.util;

import java.nio.file.Path;

/**
 * Utility class for path manipulation and file type detection.
 */
public class PathUtil {
    
    // Supported audio file extensions
    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".wav", ".aiff", ".aac", ".ogg", ".flac", ".m4a", ".wma"
    };
    
    /**
     * Checks if a file is an audio file based on its extension.
     */
    public static boolean isAudioFile(Path path) {
        if (path == null) {
            return false;
        }
        
        String fileName = path.getFileName().toString().toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a filename has an audio extension.
     */
    public static boolean isAudioFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String lowerName = fileName.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the file extension from a path (includes the dot).
     * Returns empty string if no extension found.
     */
    public static String getFileExtension(Path path) {
        if (path == null) {
            return "";
        }
        
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }
    
    /**
     * Gets the file name without extension.
     */
    public static String getFileNameWithoutExtension(Path path) {
        if (path == null) {
            return "";
        }
        
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * Converts a path to a relative path from a base directory.
     * Useful for storing portable playlist paths.
     */
    public static String getRelativePath(Path base, Path target) {
        if (base == null || target == null) {
            return "";
        }
        
        try {
            return base.relativize(target).toString();
        } catch (IllegalArgumentException e) {
            // Paths are from different roots, return absolute
            return target.toString();
        }
    }
    
    /**
     * Returns array of supported audio extensions.
     */
    public static String[] getSupportedExtensions() {
        return AUDIO_EXTENSIONS.clone();
    }
}
