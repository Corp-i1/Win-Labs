package com.winlabs.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for managing application logging configuration and operations.
 * Wraps SLF4J/Logback and provides dynamic configuration based on Settings.
 */
public class LoggerService {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerService.class);
    private static final String LOGGER_NAME = "com.winlabs";
    
    /**
     * Configures logging based on application settings.
     * Updates log level, directory, rotation size, and retention policy.
     */
    public static void configureLogging(Settings settings) {
        if (settings == null) {
            logger.warn("Settings is null, using default logging configuration");
            return;
        }
        
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            Logger winlabsLogger = context.getLogger(LOGGER_NAME);
            
            // Set log level
            Level level = convertLogLevel(settings.getLogLevel());
            rootLogger.setLevel(level);
            winlabsLogger.setLevel(level);
            
            // Enable/disable logging
            if (!settings.isLoggingEnabled()) {
                rootLogger.setLevel(Level.OFF);
                winlabsLogger.setLevel(Level.OFF);
                logger.info("Logging disabled by user settings");
                return;
            }
            
            // Configure file appender with dynamic settings
            configureFileAppender(context, settings);
            
            logger.info("Logging configured: level={}, directory={}, rotation={}MB, retention={}days",
                settings.getLogLevel(), settings.getLogDirectory(), 
                settings.getLogRotationSizeMB(), settings.getLogRetentionDays());
                
        } catch (Exception e) {
            logger.error("Failed to configure logging", e);
        }
    }
    
    /**
     * Configures the rolling file appender with user settings.
     */
    private static void configureFileAppender(LoggerContext context, Settings settings) {
        String logDir = settings.getLogDirectory();
        
        // Create log directory if it doesn't exist
        try {
            Path logPath = Paths.get(logDir);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
                logger.info("Created log directory: {}", logDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create log directory: {}", logDir, e);
            return;
        }
        
        // Find and update existing FILE appender
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger winlabsLogger = context.getLogger(LOGGER_NAME);
        
        // Remove old file appenders
        rootLogger.detachAppender("FILE");
        winlabsLogger.detachAppender("FILE");
        
        // Create new rolling file appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("FILE");
        fileAppender.setFile(logDir + "/winlabs.log");
        
        // Configure encoder (log format)
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        fileAppender.setEncoder(encoder);
        
        // Configure rolling policy
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir + "/winlabs.%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxFileSize(FileSize.valueOf(settings.getLogRotationSizeMB() + "MB"));
        rollingPolicy.setMaxHistory(settings.getLogRetentionDays());
        rollingPolicy.setTotalSizeCap(FileSize.valueOf((settings.getLogRotationSizeMB() * 10) + "MB"));
        rollingPolicy.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();
        
        // Attach to loggers
        rootLogger.addAppender(fileAppender);
        winlabsLogger.addAppender(fileAppender);
    }
    
    /**
     * Converts application LogLevel to Logback Level.
     */
    private static Level convertLogLevel(LogLevel logLevel) {
        if (logLevel == null) {
            return Level.INFO;
        }
        
        switch (logLevel) {
            case TRACE:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARN;
            case ERROR:
                return Level.ERROR;
            default:
                return Level.INFO;
        }
    }
    
    /**
     * Cleans up old log files based on retention policy.
     * Deletes log files older than the specified number of days.
     */
    public static void cleanupOldLogs(Settings settings) {
        if (settings == null || !settings.isLoggingEnabled()) {
            return;
        }
        
        try {
            Path logDir = Paths.get(settings.getLogDirectory());
            if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
                return;
            }
            
            int retentionDays = settings.getLogRetentionDays();
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            
            try (Stream<Path> files = Files.list(logDir)) {
                List<Path> logFilesToCheck = files
                    .filter(path -> path.toString().endsWith(".log"))
                    .collect(Collectors.toList());
                
                for (Path path : logFilesToCheck) {
                    try {
                        Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                        if (lastModified.isBefore(cutoffDate)) {
                            Files.delete(path);
                            logger.debug("Deleted old log file: {}", path);
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to process log file: {}", path, e);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to list log directory: {}", logDir, e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old logs", e);
        }
    }
    
    /**
     * Gets all log files in the log directory.
     */
    public static List<File> getLogFiles(Settings settings) {
        List<File> logFiles = new ArrayList<>();
        
        if (settings == null || !settings.isLoggingEnabled()) {
            return logFiles;
        }
        
        try {
            Path logDir = Paths.get(settings.getLogDirectory());
            if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
                return logFiles;
            }
            
            try (Stream<Path> files = Files.list(logDir)) {
                List<Path> logPaths = files
                    .filter(path -> path.toString().endsWith(".log"))
                    .collect(Collectors.toList());
                
                // Sort by modification time (most recent first)
                logPaths.sort((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                });
                
                // Convert to File objects
                for (Path path : logPaths) {
                    logFiles.add(path.toFile());
                }
            } catch (IOException e) {
                logger.warn("Failed to list log directory: {}", logDir, e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to get log files", e);
        }
        
        return logFiles;
    }
    
    /**
     * Opens the log directory in the system file explorer.
     */
    public static void openLogDirectory(Settings settings) {
        if (settings == null) {
            logger.warn("Settings is null, cannot open log directory");
            return;
        }
        
        try {
            Path logDir = Paths.get(settings.getLogDirectory());
            
            // Create directory if it doesn't exist
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // Open in file explorer
            File directory = logDir.toFile();
            if (directory.exists() && directory.isDirectory()) {
                java.awt.Desktop.getDesktop().open(directory);
                logger.info("Opened log directory: {}", logDir);
            }
            
        } catch (Exception e) {
            logger.error("Failed to open log directory", e);
        }
    }
}
