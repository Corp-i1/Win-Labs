package com.winlabs.service;

public class PlatformIndicatorService {
    
    /** Detect Windows platform (cached for performance) */
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    
    /** Detect macOS platform (cached for performance) */
    public static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    
    /** Detect Linux platform (cached for performance) */
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("nux");
    
}
    