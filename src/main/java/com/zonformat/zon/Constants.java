/*
 * ZON Protocol Constants v1.0.5
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

/**
 * Constants for the ZON format protocol.
 */
public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    
    // Format markers
    public static final char TABLE_MARKER = '@';
    public static final char META_SEPARATOR = ':';
    
    // Reserved tokens (for future use)
    public static final String GAS_TOKEN = "_";
    public static final String LIQUID_TOKEN = "^";
    
    // Default anchor interval for large datasets
    public static final int DEFAULT_ANCHOR_INTERVAL = 100;
    
    // Security limits (DOS prevention)
    public static final long MAX_DOCUMENT_SIZE = 100L * 1024 * 1024;  // 100 MB
    public static final int MAX_LINE_LENGTH = 1024 * 1024;            // 1 MB
    public static final int MAX_ARRAY_LENGTH = 1_000_000;             // 1 million items
    public static final int MAX_OBJECT_KEYS = 100_000;                // 100K keys
    public static final int MAX_NESTING_DEPTH = 100;                  // Maximum nesting depth
    
    // Legacy compatibility
    public static final char LEGACY_TABLE_MARKER = '@';
    public static final int INLINE_THRESHOLD_ROWS = 0;
}
