/*
 * Security Limits Tests (DOS Prevention)
 * Port of security-limits.test.ts from the TypeScript implementation
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for security limits (DOS prevention).
 */
class SecurityLimitsTest {
    
    @Nested
    @DisplayName("E301: Document Size Limit")
    class DocumentSizeLimitTests {
        
        @Test
        @DisplayName("should allow documents under 100MB")
        void testAllowDocumentsUnder100MB() {
            StringBuilder doc = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                doc.append("test:value\n");
            }
            
            assertDoesNotThrow(() -> Zon.decode(doc.toString()));
        }
        
        // Note: We don't test > 100MB as it would be too slow/memory intensive
    }
    
    @Nested
    @DisplayName("E302: Line Length Limit")
    class LineLengthLimitTests {
        
        @Test
        @DisplayName("should throw when line exceeds 1MB")
        void testThrowOnLineLengthExceeded() {
            StringBuilder longLine = new StringBuilder("key:");
            for (int i = 0; i < Constants.MAX_LINE_LENGTH + 1; i++) {
                longLine.append("x");
            }
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(longLine.toString()));
            assertTrue(error.getMessage().contains("Line length exceeds maximum"));
            assertEquals("E302", error.getCode());
        }
        
        @Test
        @DisplayName("should allow lines under 1MB")
        void testAllowLinesUnder1MB() {
            StringBuilder line = new StringBuilder("key:");
            for (int i = 0; i < 1000; i++) {
                line.append("x");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(line.toString());
            assertNotNull(result.get("key"));
        }
    }
    
    @Nested
    @DisplayName("E303: Array Length Limit")
    class ArrayLengthLimitTests {
        
        @Test
        @DisplayName("should have array length limit defined")
        void testArrayLengthLimitDefined() {
            // The limit exists in implementation at MAX_ARRAY_LENGTH (1M items)
            assertEquals(1_000_000, Constants.MAX_ARRAY_LENGTH);
        }
    }
    
    @Nested
    @DisplayName("E304: Object Key Count Limit")
    class ObjectKeyCountLimitTests {
        
        @Test
        @DisplayName("should have object key limit defined")
        void testObjectKeyLimitDefined() {
            // The limit exists in implementation at MAX_OBJECT_KEYS (100K keys)
            assertEquals(100_000, Constants.MAX_OBJECT_KEYS);
        }
        
        @Test
        @DisplayName("should allow objects under 100K keys")
        void testAllowObjectsUnder100KKeys() {
            StringBuilder keys = new StringBuilder("{");
            for (int i = 0; i < 100; i++) {
                if (i > 0) keys.append(",");
                keys.append("k").append(i).append(":").append(i);
            }
            keys.append("}");
            
            String zonData = "data:\"" + keys.toString() + "\"";
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            assertEquals(100, data.size());
        }
    }
    
    @Nested
    @DisplayName("Nesting Depth Limit")
    class NestingDepthLimitTests {
        
        @Test
        @DisplayName("should throw when nesting exceeds 100 levels")
        void testThrowOnExcessiveNesting() {
            StringBuilder nested = new StringBuilder();
            for (int i = 0; i < 150; i++) {
                nested.append("[");
            }
            for (int i = 0; i < 150; i++) {
                nested.append("]");
            }
            
            assertThrows(ZonDecodeError.class, () -> Zon.decode(nested.toString()));
        }
        
        @Test
        @DisplayName("should allow nesting under 100 levels")
        void testAllowNestingUnder100Levels() {
            StringBuilder nested = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                nested.append("[");
            }
            for (int i = 0; i < 50; i++) {
                nested.append("]");
            }
            
            Object result = Zon.decode(nested.toString());
            assertNotNull(result);
        }
    }
    
    @Nested
    @DisplayName("Combined Limits")
    class CombinedLimitsTests {
        
        @Test
        @DisplayName("should work with normal data within all limits")
        void testNormalDataWithinLimits() {
            String zonData = "metadata:\"{version:1.0.5,env:prod}\"\nusers:@(3):id,name\n1,Alice\n2,Bob\n3,Carol\ntags:\"[nodejs,typescript,llm]\"";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            assertEquals(3, users.size());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertEquals("1.0.5", metadata.get("version"));
            
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) result.get("tags");
            assertEquals(3, tags.size());
        }
    }
}
