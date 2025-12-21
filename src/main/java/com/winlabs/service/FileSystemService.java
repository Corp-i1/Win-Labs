package com.winlabs.service;

import com.winlabs.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for file system operations.
 * Handles file browsing, filtering, and directory operations.
 */
public class FileSystemService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSystemService.class);
    
    /**
     * Lists all audio files in a directory (non-recursive).
     */
    public List<Path> listAudioFiles(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            logger.debug("Directory does not exist or is not a directory: {}", directory);
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.list(directory)) {
            List<Path> audioFiles = paths
                .filter(Files::isRegularFile)
                .filter(PathUtil::isAudioFile)
                .sorted()
                .collect(Collectors.toList());
            logger.debug("Found {} audio files in {}", audioFiles.size(), directory);
            return audioFiles;
        }
    }
    
    /**
     * Lists all audio files in a directory recursively.
     */
    public List<Path> listAudioFilesRecursive(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(PathUtil::isAudioFile)
                .sorted()
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Lists all subdirectories in a directory.
     */
    public List<Path> listSubdirectories(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                .filter(Files::isDirectory)
                .sorted()
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Lists all files and directories in a directory.
     */
    public List<Path> listAll(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                .sorted((p1, p2) -> {
                    // Directories first, then files
                    boolean p1IsDir = Files.isDirectory(p1);
                    boolean p2IsDir = Files.isDirectory(p2);
                    if (p1IsDir && !p2IsDir) return -1;
                    if (!p1IsDir && p2IsDir) return 1;
                    return p1.getFileName().toString().compareTo(p2.getFileName().toString());
                })
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Checks if a file exists.
     */
    public boolean fileExists(Path path) {
        return Files.exists(path);
    }
    
    /**
     * Gets the user's home directory.
     */
    public Path getHomeDirectory() {
        return Paths.get(System.getProperty("user.home"));
    }
    
    /**
     * Gets the user's music directory (platform-specific).
     */
    public Path getMusicDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        Path home = getHomeDirectory();
        
        if (os.contains("win")) {
            // Windows: C:\Users\Username\Music
            return home.resolve("Music");
        } else if (os.contains("mac")) {
            // macOS: /Users/Username/Music
            return home.resolve("Music");
        } else {
            // Linux: /home/username/Music
            return home.resolve("Music");
        }
    }
    
    /**
     * Gets the file system roots (drives on Windows, / on Unix).
     */
    public List<Path> getFileSystemRoots() {
        List<Path> roots = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            roots.add(root);
        }
        return roots;
    }
    
    /**
     * Searches for audio files matching a name pattern in a directory.
     * Uses case-insensitive matching.
     */
    public List<Path> searchAudioFiles(Path directory, String searchTerm) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<>();
        }
        
        String lowerSearch = searchTerm.toLowerCase();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(PathUtil::isAudioFile)
                .filter(path -> path.getFileName().toString().toLowerCase().contains(lowerSearch))
                .sorted()
                .collect(Collectors.toList());
        }
    }
}
