package com.winlabs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for handling Windows file type associations for .wlp files.
 * Allows double-clicking .wlp files to open them directly in Win-Labs.
 */
public class FileAssociationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileAssociationService.class);
    
    private static final String FILE_TYPE = ".wlp";
    private static final String PROGRAM_ID = "WinLabs.PlaylistFile";
    private static final String PROGRAM_NAME = "Win-Labs Playlist";
    
    /** Detect Windows platform (cached for performance) */
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    
    /**
     * Registers the .wlp file type with Windows registry.
     * This allows double-clicking .wlp files to open them in Win-Labs.
     * 
     * Note: Requires administrator privileges on Windows.
     */
    public static void registerFileType() {
        if (!isWindows()) {
            logger.debug("File association registration skipped (not Windows)");
            return;
        }
        
        logger.info("Registering .wlp file type association");
        try {
            // Get the path to the current executable
            String executablePath = getExecutablePath();
            
            // Register the file type association
            // Command: reg add HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\.wlp\UserChoice
            executeWindowsCommand(
                "reg",
                "add",
                "HKCU\\Software\\Classes\\" + PROGRAM_ID,
                "/ve",
                "/d",
                PROGRAM_NAME,
                "/f"
            );
            
            // Register the open action
            executeWindowsCommand(
                "reg",
                "add",
                "HKCU\\Software\\Classes\\" + PROGRAM_ID + "\\shell\\open\\command",
                "/ve",
                "/d",
                "\"" + executablePath + "\" \"%1\"",
                "/f"
            );
            
            // Associate .wlp with the program ID
            executeWindowsCommand(
                "reg",
                "add",
                "HKCU\\Software\\Classes\\" + FILE_TYPE,
                "/ve",
                "/d",
                PROGRAM_ID,
                "/f"
            );
            
            logger.info(".wlp file type registration completed successfully");
        } catch (Exception e) {
            logger.error("Failed to register file type association: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Unregisters the .wlp file type from Windows registry.
     */
    public static void unregisterFileType() {
        if (!isWindows()) {
            logger.debug("File association unregistration skipped (not Windows)");
            return;
        }
        
        logger.info("Unregistering .wlp file type association");
        try {
            executeWindowsCommand(
                "reg",
                "delete",
                "HKCU\\Software\\Classes\\" + FILE_TYPE,
                "/f"
            );
            
            executeWindowsCommand(
                "reg",
                "delete",
                "HKCU\\Software\\Classes\\" + PROGRAM_ID,
                "/f"
            );
            
            logger.info("File type association unregistered successfully");
        } catch (Exception e) {
            logger.error("Failed to unregister file type association: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Gets the path to the current executable.
     * For JAR-based applications, returns the path to the JAR file.
     */
    private static String getExecutablePath() {
        try {
            // Get the path to the current JAR or class
            String classPath = FileAssociationService.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            
            // Decode URL encoding
            classPath = java.net.URLDecoder.decode(classPath, "UTF-8");
            
            // Remove leading slash on Windows
            if (classPath.startsWith("/") && classPath.length() > 2 && classPath.charAt(2) == ':') {
                classPath = classPath.substring(1);
            }
            
            return classPath;
        } catch (Exception e) {
            // Fallback: use system property with platform-agnostic path construction
            return System.getProperty("java.home") + java.io.File.separator + "bin" + java.io.File.separator + "javaw.exe";
        }
    }
    
    /**
     * Executes a Windows command.
     */
    private static void executeWindowsCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
    }
    
    /**
     * Checks if the operating system is Windows.
     */
    private static boolean isWindows() {
        return IS_WINDOWS;
    }
    
    /**
     * Checks if a file is a .wlp (Win-Labs Playlist) file.
     */
    public static boolean isPlaylistFile(String filePath) {
        return filePath != null && filePath.endsWith(FILE_TYPE);
    }
    
    /**
     * Extracts the playlist file path from command line arguments.
     * Used when opening a .wlp file from Windows Explorer.
     */
    public static String extractPlaylistFileFromArgs(String[] args) {
        if (args.length > 0 && isPlaylistFile(args[0])) {
            return args[0];
        }
        return null;
    }
}
