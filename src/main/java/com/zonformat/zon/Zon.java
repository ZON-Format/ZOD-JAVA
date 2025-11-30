/*
 * ZON Format v1.0.5
 * Zero Overhead Notation - A human-readable data serialization format
 * optimized for LLM token efficiency
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

/**
 * Main entry point for ZON encoding and decoding operations.
 * 
 * <p>ZON (Zero Overhead Notation) is a compact, human-readable data format
 * optimized for LLM token efficiency. It achieves 35-50% token reduction
 * vs JSON through tabular encoding, single-character primitives, and
 * intelligent compression while maintaining 100% data fidelity.</p>
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Encode Java data to ZON
 * Map<String, Object> data = new HashMap<>();
 * data.put("name", "Alice");
 * data.put("age", 30);
 * String zon = Zon.encode(data);
 * 
 * // Decode ZON back to Java
 * Object decoded = Zon.decode(zon);
 * }</pre>
 * 
 * <h2>Table Format</h2>
 * <p>Arrays of uniform objects are encoded as tables:</p>
 * <pre>{@code
 * // Input: [{id: 1, name: "Alice"}, {id: 2, name: "Bob"}]
 * // Output:
 * // @(2):id,name
 * // 1,Alice
 * // 2,Bob
 * }</pre>
 * 
 * <h2>Boolean and Null Encoding</h2>
 * <ul>
 *   <li>{@code true} → {@code T}</li>
 *   <li>{@code false} → {@code F}</li>
 *   <li>{@code null} → {@code null}</li>
 * </ul>
 * 
 * @see ZonEncoder
 * @see ZonDecoder
 * @see <a href="https://github.com/ZON-Format/ZOD-JAVA">GitHub Repository</a>
 */
public final class Zon {
    
    private Zon() {
        // Prevent instantiation
    }
    
    /**
     * Encodes Java data to ZON format.
     * 
     * <p>Supported types:</p>
     * <ul>
     *   <li>Objects ({@code Map<String, Object>})</li>
     *   <li>Arrays ({@code List<?>})</li>
     *   <li>Strings</li>
     *   <li>Numbers (Integer, Long, Double, Float)</li>
     *   <li>Booleans</li>
     *   <li>Null</li>
     * </ul>
     * 
     * @param data Data to encode
     * @return ZON-formatted string
     * @throws IllegalArgumentException if circular reference detected
     */
    public static String encode(Object data) {
        return new ZonEncoder().encode(data);
    }
    
    /**
     * Decodes ZON format string to Java objects.
     * 
     * <p>Uses strict mode by default, which validates:</p>
     * <ul>
     *   <li>Row counts match declared values</li>
     *   <li>Field counts match column counts</li>
     * </ul>
     * 
     * @param zonStr ZON format string
     * @return Decoded Java object (Map, List, or primitive)
     * @throws ZonDecodeError if decoding fails or validation errors occur
     */
    public static Object decode(String zonStr) {
        return new ZonDecoder().decode(zonStr);
    }
    
    /**
     * Decodes ZON format string to Java objects with specified strictness.
     * 
     * @param zonStr ZON format string
     * @param strict Whether to enable strict validation
     * @return Decoded Java object (Map, List, or primitive)
     * @throws ZonDecodeError if decoding fails
     */
    public static Object decode(String zonStr, boolean strict) {
        return new ZonDecoder(strict).decode(zonStr);
    }
    
    /**
     * Gets the library version.
     * 
     * @return Version string
     */
    public static String getVersion() {
        return "1.0.5";
    }
}
