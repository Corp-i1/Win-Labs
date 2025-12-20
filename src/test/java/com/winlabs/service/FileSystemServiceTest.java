package com.winlabs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemServiceTest {
    
    private FileSystemService service;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        service = new FileSystemService();
    }
    
    @Test
    void testListAudioFilesInEmptyDirectory() throws IOException {
        List<Path> files = service.listAudioFiles(tempDir);
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }
    
    @Test
    void testListAudioFiles() throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("song.mp3"));
        Files.createFile(tempDir.resolve("audio.wav"));
        Files.createFile(tempDir.resolve("document.txt"));
        Files.createFile(tempDir.resolve("music.aac"));
        
        List<Path> files = service.listAudioFiles(tempDir);
        
        assertEquals(3, files.size());
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("song.mp3")));
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("audio.wav")));
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("music.aac")));
        assertFalse(files.stream().anyMatch(p -> p.getFileName().toString().equals("document.txt")));
    }
    
    @Test
    void testListAudioFilesRecursive() throws IOException {
        // Create nested structure
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(tempDir.resolve("root.mp3"));
        Files.createFile(subDir.resolve("nested.wav"));
        
        List<Path> files = service.listAudioFilesRecursive(tempDir);
        
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("root.mp3")));
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("nested.wav")));
    }
    
    @Test
    void testListSubdirectories() throws IOException {
        Files.createDirectory(tempDir.resolve("dir1"));
        Files.createDirectory(tempDir.resolve("dir2"));
        Files.createFile(tempDir.resolve("file.txt"));
        
        List<Path> dirs = service.listSubdirectories(tempDir);
        
        assertEquals(2, dirs.size());
        assertTrue(dirs.stream().anyMatch(p -> p.getFileName().toString().equals("dir1")));
        assertTrue(dirs.stream().anyMatch(p -> p.getFileName().toString().equals("dir2")));
    }
    
    @Test
    void testListAll() throws IOException {
        Files.createDirectory(tempDir.resolve("folder"));
        Files.createFile(tempDir.resolve("audio.mp3"));
        Files.createFile(tempDir.resolve("document.txt"));
        
        List<Path> all = service.listAll(tempDir);
        
        assertEquals(3, all.size());
        // Directories should come first
        assertEquals("folder", all.get(0).getFileName().toString());
    }
    
    @Test
    void testFileExists() throws IOException {
        Path existingFile = Files.createFile(tempDir.resolve("exists.txt"));
        Path nonExistingFile = tempDir.resolve("doesnotexist.txt");
        
        assertTrue(service.fileExists(existingFile));
        assertFalse(service.fileExists(nonExistingFile));
    }
    
    @Test
    void testGetHomeDirectory() {
        Path home = service.getHomeDirectory();
        assertNotNull(home);
        assertTrue(Files.exists(home));
    }
    
    @Test
    void testGetMusicDirectory() {
        Path music = service.getMusicDirectory();
        assertNotNull(music);
        assertTrue(music.toString().contains("Music"));
    }
    
    @Test
    void testGetFileSystemRoots() {
        List<Path> roots = service.getFileSystemRoots();
        assertNotNull(roots);
        assertFalse(roots.isEmpty());
    }
    
    @Test
    void testSearchAudioFiles() throws IOException {
        Files.createFile(tempDir.resolve("rock_song.mp3"));
        Files.createFile(tempDir.resolve("jazz_music.wav"));
        Files.createFile(tempDir.resolve("classical.aac"));
        
        List<Path> results = service.searchAudioFiles(tempDir, "rock");
        
        assertEquals(1, results.size());
        assertTrue(results.get(0).getFileName().toString().contains("rock"));
    }
    
    @Test
    void testSearchAudioFilesCaseInsensitive() throws IOException {
        Files.createFile(tempDir.resolve("ROCK_SONG.mp3"));
        
        List<Path> results = service.searchAudioFiles(tempDir, "rock");
        
        assertEquals(1, results.size());
    }
    
    @Test
    void testListAudioFilesNonExistentDirectory() throws IOException {
        Path nonExistent = tempDir.resolve("doesnotexist");
        List<Path> files = service.listAudioFiles(nonExistent);
        
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }
}
