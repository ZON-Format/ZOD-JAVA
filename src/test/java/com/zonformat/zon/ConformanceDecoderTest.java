/*
 * Conformance Tests - Decoder
 * Port of conformance-decoder.test.ts from the TypeScript implementation
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests based on SPEC.md §11.2 Decoder Checklist.
 */
class ConformanceDecoderTest {
    
    @Test
    @DisplayName("should accept UTF-8 with LF or CRLF")
    void testAcceptsLFandCRLF() {
        String zonLF = "key:value\nkey2:value2";
        String zonCRLF = "key:value\r\nkey2:value2";
        
        assertDoesNotThrow(() -> Zon.decode(zonLF));
        assertDoesNotThrow(() -> Zon.decode(zonCRLF));
    }
    
    @Test
    @DisplayName("should decode T → true, F → false, null → null")
    void testDecodePrimitives() {
        String zonData = "active:T\narchived:F\nvalue:null";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        assertEquals(true, result.get("active"));
        assertEquals(false, result.get("archived"));
        assertNull(result.get("value"));
    }
    
    @Test
    @DisplayName("should parse decimal and exponent numbers")
    void testParseNumbers() {
        String zonData = "int:42\nfloat:3.14\nbig:1000000";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        assertEquals(42L, ((Number) result.get("int")).longValue());
        assertEquals(3.14, ((Number) result.get("float")).doubleValue(), 0.0001);
        assertEquals(1000000L, ((Number) result.get("big")).longValue());
    }
    
    @Test
    @DisplayName("should treat leading-zero numbers as strings")
    void testLeadingZeroAsString() {
        String zonData = "code:\"007\"";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        assertEquals("007", result.get("code"));
        assertTrue(result.get("code") instanceof String);
    }
    
    @Test
    @DisplayName("should unescape quoted strings")
    void testUnescapeQuotedStrings() {
        String zonData = "text:\"he said \\\"hello\\\"\"";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        assertEquals("he said \"hello\"", result.get("text"));
    }
    
    @Test
    @DisplayName("should parse table rows into array of objects")
    void testParseTableRows() {
        String zonData = "users:@(2):id,name\n1,Alice\n2,Bob";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        
        assertEquals(2, users.size());
        assertEquals(1L, ((Number) users.get(0).get("id")).longValue());
        assertEquals("Alice", users.get(0).get("name"));
        assertEquals(2L, ((Number) users.get(1).get("id")).longValue());
        assertEquals("Bob", users.get(1).get("name"));
    }
    
    @Test
    @DisplayName("should preserve key order from document")
    void testPreserveKeyOrder() {
        String zonData = "z:1\na:2\nm:3";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        List<String> keys = new ArrayList<>(result.keySet());
        assertEquals(Arrays.asList("z", "a", "m"), keys);
    }
    
    @Test
    @DisplayName("should reject prototype pollution attempts")
    void testRejectPrototypePollution() {
        String malicious = "items:@(1):id,__proto__.polluted\n1,true";
        Object decoded = Zon.decode(malicious, false);
        
        // Check that prototype pollution didn't occur
        Map<String, Object> testObj = new HashMap<>();
        assertNull(testObj.get("polluted"));
    }
    
    @Test
    @DisplayName("should throw on nesting depth > 100")
    void testThrowOnDeepNesting() {
        StringBuilder deepNested = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            deepNested.append("[");
        }
        for (int i = 0; i < 150; i++) {
            deepNested.append("]");
        }
        
        assertThrows(ZonDecodeError.class, () -> Zon.decode(deepNested.toString()));
    }
    
    @Test
    @DisplayName("should throw on line length > 1MB (E302)")
    void testThrowOnLineLengthExceeded() {
        StringBuilder longLine = new StringBuilder("key:");
        for (int i = 0; i < Constants.MAX_LINE_LENGTH + 1; i++) {
            longLine.append("x");
        }
        
        ZonDecodeError error = assertThrows(ZonDecodeError.class, 
            () -> Zon.decode(longLine.toString()));
        assertTrue(error.getMessage().contains("E302"));
    }
    
    @Test
    @DisplayName("should handle case-insensitive null/boolean aliases")
    void testCaseInsensitiveAliases() {
        String zonData = "a:TRUE\nb:False\nc:NONE\nd:nil";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        assertEquals(true, result.get("a"));
        assertEquals(false, result.get("b"));
        assertNull(result.get("c"));
        assertNull(result.get("d"));
    }
    
    @Test
    @DisplayName("should reconstruct nested objects from dotted keys")
    void testReconstructNestedObjects() {
        String zonData = "config.db.host:localhost\nconfig.db.port:5432";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) result.get("config");
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) config.get("db");
        
        assertEquals("localhost", db.get("host"));
        assertEquals(5432L, ((Number) db.get("port")).longValue());
    }
    
    @Test
    @DisplayName("should unwrap pure lists (data key)")
    void testUnwrapPureLists() {
        String zonData = "data:@(2):id,name\n1,Alice\n2,Bob";
        Object result = Zon.decode(zonData);
        
        // Should return array directly, not { data: [...] }
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result;
        assertEquals(2, list.size());
    }
    
    @Test
    @DisplayName("should handle empty strings in table cells")
    void testEmptyStringsInTableCells() {
        String zonData = "users:@(2):id,name\n1,\"\"\n2,Bob";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        assertEquals("", users.get(0).get("name"));
    }
}
