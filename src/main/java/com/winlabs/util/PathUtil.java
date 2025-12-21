package com.winlabs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Utility class for path manipulation and file type detection.
 */
public class PathUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PathUtil.class);
    
    // Supported audio file extensions
    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".wav", ".aiff", ".aac", ".ogg", ".flac", ".m4a", ".wma"
    };
    
    /**
     * Checks if a file is an audio file based on its extension.
     */
    public static boolean isAudioFile(Path path) {
        logger.trace("isAudioFile(Path) method entry");
        logger.debug("Checking if path is audio file: {}", path);
        logger.trace("Input parameter 'path' value: {}", path);
        logger.trace("Input parameter type: Path");
        
        if (path == null) {
            logger.warn("Path parameter is null");
            logger.debug("Returning false for null path");
            logger.trace("isAudioFile(Path) method exit (null case)");
            return false;
        }
        logger.trace("Path is not null, proceeding");
        
        logger.trace("Extracting filename from path");
        String fileName = path.getFileName().toString().toLowerCase();
        logger.debug("Filename extracted and lowercased: {}", fileName);
        logger.trace("Original filename: {}", path.getFileName());
        logger.trace("Lowercased filename: {}", fileName);
        
        logger.trace("Starting extension matching loop");
        logger.debug("Checking against {} supported extensions", AUDIO_EXTENSIONS.length);
        for (int i = 0; i < AUDIO_EXTENSIONS.length; i++) {
            String ext = AUDIO_EXTENSIONS[i];
            logger.trace("Iteration {}: checking extension '{}'", i, ext);
            logger.trace("Comparing: filename='{}' endsWith extension='{}'", fileName, ext);
            if (fileName.endsWith(ext)) {
                logger.info("Audio file detected: {} (extension: {})", path, ext);
                logger.debug("Match found at index {}", i);
                logger.trace("isAudioFile(Path) method exit (match found)");
                return true;
            }
            logger.trace("No match for extension '{}', continuing", ext);
        }
        
        logger.debug("No audio extension match found for: {}", fileName);
        logger.trace("Loop completed without matches");
        logger.trace("isAudioFile(Path) method exit (no match)");
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
