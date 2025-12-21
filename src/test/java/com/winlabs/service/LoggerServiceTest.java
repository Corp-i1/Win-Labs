package com.winlabs.service;

import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LoggerService.
 * Note: Some tests are disabled because they conflict with Logback's file locking in test environment.
 */
public class LoggerServiceTest {
    
    @TempDir
    Path tempDir;
    
    private Settings settings;
    
    @BeforeEach
    void setUp() {
        settings = new Settings();
        settings.setLoggingEnabled(true);
        settings.setLogLevel(LogLevel.INFO);
        // Use a unique subdirectory for each test to avoid file locking
        Path uniqueDir = tempDir.resolve("logs-" + System.currentTimeMillis());
        settings.setLogDirectory(uniqueDir.toString());
        settings.setLogRotationSizeMB(10);
        settings.setLogRetentionDays(5);
    }
    
    @Test
    @Disabled("Conflicts with Logback file locking in test environment")
    void testConfigureLogging() {
        // Should not throw exception
        assertDoesNotThrow(() -> LoggerService.configureLogging(settings));
    }
    
    @Test
    void testConfigureLoggingWithNull() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> LoggerService.configureLogging(null));
    }
    
    @Test
    void testGetLogFilesWithNull() {
        List<File> logFiles = LoggerService.getLogFiles(null);
        assertNotNull(logFiles);
        assertTrue(logFiles.isEmpty());
    }
    
    @Test
    void testCleanupOldLogs() {
        // Should not throw exception
        assertDoesNotThrow(() -> LoggerService.cleanupOldLogs(settings));
    }
    
    @Test
    void testCleanupOldLogsWithNull() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> LoggerService.cleanupOldLogs(null));
    }
    
    @Test
    @Disabled("Conflicts with Logback file locking in test environment")
    void testLogDirectoryCreation() throws Exception {
        // Configure logging should create the directory
        LoggerService.configureLogging(settings);
        
        Path logDir = Path.of(settings.getLogDirectory());
        // Give it a moment to create the directory
        Thread.sleep(100);
        
        assertTrue(Files.exists(logDir), "Log directory should be created");
        assertTrue(Files.isDirectory(logDir), "Log directory should be a directory");
    }
    
    @Test
    void testDisabledLogging() {
        settings.setLoggingEnabled(false);
        
        // Should not throw exception when disabled
        assertDoesNotThrow(() -> LoggerService.configureLogging(settings));
    }
}
