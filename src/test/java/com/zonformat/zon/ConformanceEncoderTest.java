/*
 * Conformance Tests - Encoder
 * Port of conformance-encoder.test.ts from the TypeScript implementation
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
 * Conformance tests based on SPEC.md §11.1 Encoder Checklist.
 */
class ConformanceEncoderTest {
    
    @Test
    @DisplayName("should emit UTF-8 with LF line endings")
    void testEmitUTF8WithLF() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("a", 1);
        data.put("b", 2);
        
        String encoded = Zon.encode(data);
        
        // Should use LF, not CRLF
        assertFalse(encoded.contains("\r\n"));
        // Should be a string (UTF-8 compatible)
        assertNotNull(encoded);
    }
    
    @Test
    @DisplayName("should encode booleans as T/F")
    void testEncodeBooleans() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", true);
        data.put("archived", false);
        
        String encoded = Zon.encode(data);
        
        assertTrue(encoded.contains("active:T"));
        assertTrue(encoded.contains("archived:F"));
        assertFalse(encoded.contains("true"));
        assertFalse(encoded.contains("false"));
    }
    
    @Test
    @DisplayName("should encode null as 'null'")
    void testEncodeNull() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("value", null);
        
        String encoded = Zon.encode(data);
        
        assertTrue(encoded.contains("value:null"));
    }
    
    @Test
    @DisplayName("should emit canonical numbers")
    void testCanonicalNumbers() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("int", 42);
        data.put("float", 3.14);
        data.put("big", 1000000);
        
        String encoded = Zon.encode(data);
        
        // No scientific notation
        assertTrue(encoded.contains("1000000"));
        assertFalse(encoded.contains("1e6"));
        assertFalse(encoded.contains("1e+6"));
        
        // Has decimal for floats
        assertTrue(encoded.contains("3.14"));
    }
    
    @Test
    @DisplayName("should normalize NaN/Infinity to null")
    void testNormalizeSpecialValues() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nan", Double.NaN);
        data.put("inf", Double.POSITIVE_INFINITY);
        data.put("negInf", Double.NEGATIVE_INFINITY);
        
        String encoded = Zon.encode(data);
        
        assertTrue(encoded.contains("nan:null"));
        assertTrue(encoded.contains("inf:null"));
        assertTrue(encoded.contains("negInf:null"));
    }
    
    @Test
    @DisplayName("should detect uniform arrays → table format")
    void testDetectUniformArrays() {
        List<Map<String, Object>> users = new ArrayList<>();
        
        Map<String, Object> u1 = new LinkedHashMap<>();
        u1.put("id", 1);
        u1.put("name", "Alice");
        users.add(u1);
        
        Map<String, Object> u2 = new LinkedHashMap<>();
        u2.put("id", 2);
        u2.put("name", "Bob");
        users.add(u2);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", users);
        
        String encoded = Zon.encode(data);
        
        // Should have table marker
        assertTrue(encoded.matches("(?s).*users:@\\(\\d+\\).*"));
        assertTrue(encoded.contains("id,name"));
    }
    
    @Test
    @DisplayName("should emit table headers with count and columns")
    void testTableHeaders() {
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> i1 = new LinkedHashMap<>();
        i1.put("x", 1);
        i1.put("y", 2);
        items.add(i1);
        
        Map<String, Object> i2 = new LinkedHashMap<>();
        i2.put("x", 3);
        i2.put("y", 4);
        items.add(i2);
        
        Map<String, Object> i3 = new LinkedHashMap<>();
        i3.put("x", 5);
        i3.put("y", 6);
        items.add(i3);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        
        String encoded = Zon.encode(data);
        
        assertTrue(encoded.contains("items:@(3):"));
    }
    
    @Test
    @DisplayName("should sort columns alphabetically")
    void testSortColumnsAlphabetically() {
        List<Map<String, Object>> records = new ArrayList<>();
        
        Map<String, Object> r1 = new LinkedHashMap<>();
        r1.put("z", 1);
        r1.put("a", 2);
        r1.put("m", 3);
        records.add(r1);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        
        String encoded = Zon.encode(data);
        
        // Columns should be sorted: a, m, z
        assertTrue(encoded.matches("(?s).*records:@\\(1\\):a,m,z.*"));
    }
    
    @Test
    @DisplayName("should quote strings with special characters")
    void testQuoteSpecialCharacters() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("comma", "a,b");
        data.put("colon", "x:y");
        data.put("quote", "say \"hi\"");
        
        String encoded = Zon.encode(data);
        
        assertTrue(encoded.contains("\"a,b\""));
        // Colons are allowed unquoted in v2.0.5
        assertTrue(encoded.contains("x:y"));
        // Uses quote doubling: " becomes ""
        assertTrue(encoded.contains("\"\"hi\"\""));
    }
    
    @Test
    @DisplayName("should escape quotes in strings")
    void testEscapeQuotes() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", "he said \"hello\"");
        
        String encoded = Zon.encode(data);
        
        // Uses quote doubling
        assertTrue(encoded.contains("\"\"hello\"\""));
    }
    
    @Test
    @DisplayName("should produce deterministic output")
    void testDeterministicOutput() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("b", 2);
        data.put("a", 1);
        data.put("c", 3);
        
        String encoded1 = Zon.encode(data);
        String encoded2 = Zon.encode(data);
        
        assertEquals(encoded1, encoded2);
    }
    
    @Test
    @DisplayName("should handle empty objects")
    void testEmptyObjects() {
        Map<String, Object> data = new LinkedHashMap<>();
        
        String encoded = Zon.encode(data);
        
        // Empty object is empty string in ZON
        assertEquals("", encoded);
    }
    
    @Test
    @DisplayName("should handle empty arrays")
    void testEmptyArrays() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", new ArrayList<>());
        
        String encoded = Zon.encode(data);
        
        assertNotNull(encoded);
        assertTrue(encoded.length() > 0);
    }
}
