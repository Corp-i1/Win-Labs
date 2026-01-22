package com.winlabs.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.winlabs.model.LogLevel;
import com.winlabs.model.Settings;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for managing application logging configuration and operations.
 * Wraps SLF4J/Logback and provides dynamic configuration based on Settings.
 * 
 * <p>Thread Safety: All public methods are thread-safe using ReadWriteLock.
 * Configuration changes use write lock (exclusive), while read operations
 * (cleanup, file retrieval) use read lock (concurrent). Multiple threads can
 * safely call configureLogging() and read methods concurrently.</p>
 * 
 * <p>Validation: Settings are automatically validated and corrected:
 * <ul>
 *   <li>Rotation size: 1-1000MB (default: 10MB)</li>
 *   <li>Retention days: 1-365 days (default: 30 days)</li>
 *   <li>Log directory: Falls back to temp directory if invalid/unwritable</li>
 *   <li>Write permissions: Tested before accepting directory</li>
 *   <li>Disk space: Validated against retention requirements</li>
 * </ul>
 * </p>
 * 
 * <p>Error Recovery: If primary log directory fails validation, automatically falls back to:
 * System temp directory (/tmp/winlabs-logs or %TEMP%\winlabs-logs)</p>
 */
public class LoggerService {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerService.class);
    private static final String LOGGER_NAME = "com.winlabs";
    private static final ReadWriteLock CONFIG_LOCK = new ReentrantReadWriteLock();
    
    // Configuration constants
    /** Default async queue size for buffering log events before blocking */
    private static final int DEFAULT_ASYNC_QUEUE_SIZE = 256;
    
    /** Minimum log file size in MB before rotation */
    private static final int MIN_ROTATION_SIZE_MB = 1;
    /** Maximum log file size in MB before rotation */
    private static final int MAX_ROTATION_SIZE_MB = 1000;
    /** Default log file size in MB before rotation */
    private static final int DEFAULT_ROTATION_SIZE_MB = 10;
    
    /** Minimum log retention in days */
    private static final int MIN_RETENTION_DAYS = 1;
    /** Maximum log retention in days */
    private static final int MAX_RETENTION_DAYS = 365;
    /** Default log retention in days */
    private static final int DEFAULT_RETENTION_DAYS = 30;
    
    /** Factor to convert MB to bytes for total log capacity calculation */
    private static final int MB_TO_BYTES_FACTOR = 1024 * 1024;
    
    /** Minimum total size cap for all log files combined */
    private static final int MIN_TOTAL_CAP_MB = 100;
    
    /** Multiplier for calculating total cap from rotation size (rotationSize * 5) */
    private static final int TOTAL_CAP_MULTIPLIER = 5;
    
    /** Test file name for write permission validation */
    private static final String WRITE_TEST_FILE = ".winlabs_write_test";
    
    /** Windows reserved device names that cannot be used in file paths */
    private static final List<String> WINDOWS_RESERVED_NAMES = Arrays.asList(
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", 
        "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", 
        "LPT6", "LPT7", "LPT8", "LPT9"
    );
    
    /** Maximum path length on Windows (legacy limit, some systems support longer) */
    private static final int WINDOWS_MAX_PATH_LENGTH = 260;
    
    /** Detect Windows platform */
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    
    /** Detect Unix-like platforms (Linux, macOS, BSD) */
    private static final boolean IS_UNIX = System.getProperty("os.name").toLowerCase().matches(".*(nix|nux|mac|bsd).*");
    
    /**
     * Validates and sanitizes logging settings.
     * Auto-corrects invalid values to prevent runtime errors.
     * Creates a defensive copy to avoid mutating the input.
     * 
     * @param settings The settings to validate (input is not modified)
     * @return New validated Settings object with corrected values, or new Settings() if input is null
     * @see #configureLogging(Settings) for validation rules applied during configuration
     */
    private static Settings validateSettings(Settings settings) {
        if (settings == null) {
            return new Settings();
        }
        
        // Create defensive copy to avoid mutating input
        Settings validatedSettings = new Settings();
        validatedSettings.setTheme(settings.getTheme());
        validatedSettings.setLogDirectory(settings.getLogDirectory());
        validatedSettings.setLogRotationSizeMB(settings.getLogRotationSizeMB());
        validatedSettings.setLogRetentionDays(settings.getLogRetentionDays());
        validatedSettings.setLogLevel(settings.getLogLevel());
        validatedSettings.setLoggingEnabled(settings.isLoggingEnabled());
        
        // Validate log directory
        String logDir = validatedSettings.getLogDirectory();
        if (logDir == null || logDir.trim().isEmpty()) {
            Path defaultLogDir = Paths.get(System.getProperty("user.home"), ".winlabs", "logs");
            logger.warn("Log directory is null or empty. Auto-correcting to default: {}. " +
                "Please update your settings.", defaultLogDir);
            validatedSettings.setLogDirectory(defaultLogDir.toString());
            logDir = defaultLogDir.toString();
        }
        
        // Resolve symlinks to canonical path
        try {
            Path resolvedPath = Paths.get(logDir).toRealPath();
            if (!resolvedPath.toString().equals(logDir)) {
                logger.info("Resolved symlink '{}' to canonical path: {}", logDir, resolvedPath);
                validatedSettings.setLogDirectory(resolvedPath.toString());
                logDir = resolvedPath.toString();
            }
        } catch (Exception e) {
            // Path doesn't exist yet or can't be resolved - that's OK, we'll create it
            logger.debug("Could not resolve path '{}': {}", logDir, e.getMessage());
        }
        
        // Validate path length on Windows
        if (IS_WINDOWS && logDir.length() > WINDOWS_MAX_PATH_LENGTH) {
            logger.error("Log directory path exceeds Windows maximum length ({} > {} characters). " +
                "Using fallback directory.", logDir.length(), WINDOWS_MAX_PATH_LENGTH);
            Path fallbackDir = Paths.get(System.getProperty("java.io.tmpdir"), "winlabs-logs");
            validatedSettings.setLogDirectory(fallbackDir.toString());
            logDir = fallbackDir.toString();
        }
        
        // Check for Windows reserved names in ALL path components (Windows only)
        if (IS_WINDOWS) {
            Path logPath = Paths.get(logDir);
            for (Path component : logPath) {
                String name = component.toString().toUpperCase();
                // Remove extension if present (e.g., CON.txt is still invalid)
                int dotIndex = name.indexOf('.');
                if (dotIndex > 0) {
                    name = name.substring(0, dotIndex);
                }
                if (WINDOWS_RESERVED_NAMES.contains(name)) {
                    Path fallbackDir = Paths.get(System.getProperty("java.io.tmpdir"), "winlabs-logs");
                    logger.error("Log directory '{}' contains Windows reserved name ('{}'). " +
                        "Auto-correcting to fallback: {}. Please update your settings.", 
                        logDir, component.toString(), fallbackDir);
                    validatedSettings.setLogDirectory(fallbackDir.toString());
                    logDir = fallbackDir.toString();
                    break;
                }
            }
        }
        
        // Test write permissions with atomic operation
        Path testPath = Paths.get(logDir);
        try {
            Files.createDirectories(testPath);
            
            // Set platform-specific directory attributes
            setDirectoryAttributes(testPath);
            
            // Atomic test file write and delete
            Path testFile = testPath.resolve(WRITE_TEST_FILE);
            Files.writeString(testFile, "test"); // Atomic write operation
            if (!Files.deleteIfExists(testFile)) {
                logger.warn("Test file '{}' was not deleted (may not exist)", testFile);
            }
        } catch (Exception e) {
            Path fallbackDir = Paths.get(System.getProperty("java.io.tmpdir"), "winlabs-logs");
            logger.error("Cannot write to log directory '{}': {}. " +
                "Auto-correcting to fallback: {}. Please update your settings.", 
                logDir, e.getMessage(), fallbackDir);
            validatedSettings.setLogDirectory(fallbackDir.toString());
            logDir = fallbackDir.toString();
        }
        
        // Validate rotation size
        int rotationSize = validatedSettings.getLogRotationSizeMB();
        if (rotationSize < MIN_ROTATION_SIZE_MB || rotationSize > MAX_ROTATION_SIZE_MB) {
            logger.warn("Log rotation size {}MB is outside valid range [{}-{}MB]. " +
                "Auto-correcting to default {}MB. Please update your settings.", 
                rotationSize, MIN_ROTATION_SIZE_MB, MAX_ROTATION_SIZE_MB, DEFAULT_ROTATION_SIZE_MB);
            validatedSettings.setLogRotationSizeMB(DEFAULT_ROTATION_SIZE_MB);
            rotationSize = DEFAULT_ROTATION_SIZE_MB;
        }
        
        // Validate retention days
        int retentionDays = validatedSettings.getLogRetentionDays();
        if (retentionDays < MIN_RETENTION_DAYS || retentionDays > MAX_RETENTION_DAYS) {
            logger.warn("Log retention {} days is outside valid range [{}-{} days]. " +
                "Auto-correcting to default {} days. Please update your settings.", 
                retentionDays, MIN_RETENTION_DAYS, MAX_RETENTION_DAYS, DEFAULT_RETENTION_DAYS);
            validatedSettings.setLogRetentionDays(DEFAULT_RETENTION_DAYS);
            retentionDays = DEFAULT_RETENTION_DAYS;
        }
        
        // Validate disk space
        try {
            Path logPathFinal = Paths.get(validatedSettings.getLogDirectory());
            long usableSpace = Files.getFileStore(logPathFinal).getUsableSpace();
            long requiredSpace = (long) retentionDays * rotationSize * MB_TO_BYTES_FACTOR;
            
            if (usableSpace < requiredSpace) {
                logger.warn("Insufficient disk space in '{}': {} MB available, {} MB required for {} days retention. " +
                           "Consider reducing rotation size or retention days.",
                           logDir, usableSpace / MB_TO_BYTES_FACTOR, requiredSpace / MB_TO_BYTES_FACTOR, retentionDays);
            }
        } catch (Exception e) {
            logger.warn("Could not check disk space for log directory '{}': {}", logDir, e.getMessage());
        }
        
        // Warn about unusual configurations
        if (rotationSize < 5) {
            logger.warn("Small log rotation size ({}MB) may create many files", rotationSize);
        }
        if (retentionDays > 180) {
            logger.warn("Long retention period ({} days) may consume significant disk space", 
                retentionDays);
        }
        
        return validatedSettings;
    }
    
    /**
     * Configures logging based on application settings.
     * Updates log level, directory, rotation size, and retention policy.
     * Persists fallback directory if validation changed the location.
     * 
     * @param settings The settings to configure (will be updated if validation changed directory)
     */
    public static void configureLogging(Settings settings) {
        // Thread safety - prevent concurrent configuration
        CONFIG_LOCK.writeLock().lock();
        try {
            // Validate and get corrected settings
            Settings validatedSettings = validateSettings(settings);
            if (validatedSettings == null) {
                logger.error("Settings is null after validation, using default logging configuration");
                return;
            }
            
            // Update original settings with validated values (especially fallback directory)
            settings.setLogDirectory(validatedSettings.getLogDirectory());
            settings.setLogRotationSizeMB(validatedSettings.getLogRotationSizeMB());
            settings.setLogRetentionDays(validatedSettings.getLogRetentionDays());
            
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
            
            logger.info("Logging configured: level={}, directory={}, rotation={}MB, retention={} days", 
                settings.getLogLevel(), settings.getLogDirectory(), 
                settings.getLogRotationSizeMB(), settings.getLogRetentionDays());
                
        } catch (Exception e) {
            logger.error("Failed to configure logging: {}", e.getMessage(), e);
        } finally {
            CONFIG_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Configures the rolling file appender with user settings.
     * Assumes the log directory has already been validated and is writable.
     */
    private static void configureFileAppender(LoggerContext context, Settings settings) {
        Path logPath = Paths.get(settings.getLogDirectory());
        
        // Create log directory if it doesn't exist (validation already confirmed it's writable)
        try {
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
                logger.debug("Created log directory: {}", logPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create log directory '{}': {}", logPath, e.getMessage());
            return;
        }
        
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger winlabsLogger = context.getLogger(LOGGER_NAME);
        
        // Stop and remove old appenders to prevent resource leaks
        // AsyncAppender is named "FILE", so we need to match that
        Appender<ILoggingEvent> oldRootAppender = rootLogger.getAppender("FILE");
        if (oldRootAppender != null) {
            oldRootAppender.stop();
            rootLogger.detachAppender(oldRootAppender);
        }
        Appender<ILoggingEvent> oldWinlabsAppender = winlabsLogger.getAppender("FILE");
        if (oldWinlabsAppender != null) {
            oldWinlabsAppender.stop();
            winlabsLogger.detachAppender(oldWinlabsAppender);
        }
        
        // Create new rolling file appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("ROLLING_FILE");
        fileAppender.setFile(logPath.resolve("winlabs.log").toString());
        
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
        rollingPolicy.setFileNamePattern(logPath.resolve("winlabs.%d{yyyy-MM-dd}.%i.log").toString());
        rollingPolicy.setMaxFileSize(FileSize.valueOf(settings.getLogRotationSizeMB() + "MB"));
        rollingPolicy.setMaxHistory(settings.getLogRetentionDays());
        
        // Total size cap: max(100MB, rotationSize * 5) to prevent unbounded growth
        int totalCapMB = Math.max(MIN_TOTAL_CAP_MB, settings.getLogRotationSizeMB() * TOTAL_CAP_MULTIPLIER);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf(totalCapMB + "MB"));
        rollingPolicy.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();
        
        // Wrap in AsyncAppender for better performance (non-blocking I/O)
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(context);
        asyncAppender.setName("FILE");
        asyncAppender.setQueueSize(DEFAULT_ASYNC_QUEUE_SIZE);  // Buffer up to 256 log events
        asyncAppender.setDiscardingThreshold(0);  // Don't discard events unless queue is full
        asyncAppender.setIncludeCallerData(false);  // Performance optimization
        asyncAppender.addAppender(fileAppender);
        asyncAppender.start();
        
        // Attach async appender to loggers
        rootLogger.addAppender(asyncAppender);
        winlabsLogger.addAppender(asyncAppender);
    }
    
    /**
     * Sets platform-specific attributes on the log directory.
     * - Windows: Sets hidden attribute on .winlabs directory
     * - Unix: Sets 755 permissions (rwxr-xr-x)
     * 
     * @param directory The directory to configure
     */
    private static void setDirectoryAttributes(Path directory) {
        try {
            if (IS_WINDOWS) {
                // Hide .winlabs directory on Windows
                if (directory.getFileName().toString().startsWith(".")) {
                    DosFileAttributes dosAttrs = Files.readAttributes(directory, DosFileAttributes.class);
                    if (!dosAttrs.isHidden()) {
                        Files.setAttribute(directory, "dos:hidden", true);
                        logger.debug("Set hidden attribute on Windows directory: {}", directory);
                    }
                }
            } else if (IS_UNIX) {
                // Set 755 permissions on Unix (rwxr-xr-x)
                Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE
                );
                Files.setPosixFilePermissions(directory, perms);
                logger.debug("Set 755 permissions on Unix directory: {}", directory);
            }
        } catch (Exception e) {
            // Non-critical - log and continue
            logger.debug("Could not set platform-specific attributes on '{}': {}", 
                directory, e.getMessage());
        }
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
     * 
     * <p>Thread-safe: Uses read lock to allow concurrent cleanup operations.</p>
     * 
     * @param settings The settings containing log directory and retention policy
     */
    public static void cleanupOldLogs(Settings settings) {
        if (settings == null || !settings.isLoggingEnabled()) {
            return;
        }
        
        Path logDir = Paths.get(settings.getLogDirectory());
        if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
            return;
        }
        
        CONFIG_LOCK.readLock().lock();
        try {
            
            int retentionDays = settings.getLogRetentionDays();
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
            try (Stream<Path> files = Files.list(logDir)) {
                List<Path> logFilesToCheck = files
                    .filter(path -> path.getFileName().toString().matches("winlabs.*\\.log"))
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
        } finally {
            CONFIG_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Gets all log files in the log directory.
     * Files are sorted by modification time (most recent first).
     * 
     * <p>Thread-safe: Uses read lock to allow concurrent reads.</p>
     * 
     * @param settings The settings containing log directory path
     * @return List of log files sorted by modification time, empty list if directory doesn't exist
     */
    public static List<File> getLogFiles(Settings settings) {
        List<File> logFiles = new ArrayList<>();
        
        if (settings == null || !settings.isLoggingEnabled()) {
            return logFiles;
        }
        
        Path logDir = Paths.get(settings.getLogDirectory());
        if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
            return logFiles;
        }
        
        CONFIG_LOCK.readLock().lock();
        try {
        
            try (Stream<Path> files = Files.list(logDir)) {
                List<Path> logPaths = files
                    .filter(path -> path.getFileName().toString().matches("winlabs.*\\.log"))
                    .collect(Collectors.toList());
                
                // Sort by modification time (most recent first)
                logPaths.sort((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        logger.warn("Failed to get modification time for log file comparison: {}", 
                            e.getMessage());
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
        } finally {
            CONFIG_LOCK.readLock().unlock();
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
                // Check if Desktop API is supported
                if (Desktop.isDesktopSupported() && 
                    Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(directory);
                    logger.info("Opened log directory: {}", logDir);
                } else {
                    logger.warn("Desktop API not supported, cannot open log directory");
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to open log directory", e);
        }
    }
}
