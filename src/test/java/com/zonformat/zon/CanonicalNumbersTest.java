/*
 * Canonical Number Formatting Tests
 * Port of canonical-numbers.test.ts from the TypeScript implementation
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
 * Tests for canonical number formatting in ZON.
 */
class CanonicalNumbersTest {
    
    @Nested
    @DisplayName("Integer Numbers")
    class IntegerNumberTests {
        
        @Test
        @DisplayName("should encode integers without decimal point")
        void testIntegerWithoutDecimal() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 42);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("42"));
            assertFalse(encoded.contains("42.0"));
        }
        
        @Test
        @DisplayName("should handle zero")
        void testZero() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 0);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("value:0"));
        }
        
        @Test
        @DisplayName("should handle negative integers")
        void testNegativeIntegers() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", -123);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("-123"));
        }
    }
    
    @Nested
    @DisplayName("Floating Point Numbers")
    class FloatingPointTests {
        
        @Test
        @DisplayName("should encode floats without trailing zeros")
        void testFloatWithoutTrailingZeros() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 3.14);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("3.14"));
            assertFalse(encoded.contains("3.140000"));
        }
        
        @Test
        @DisplayName("should handle very small decimals")
        void testVerySmallDecimals() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 0.001);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("0.001"));
            assertFalse(encoded.contains("1e-3"));
        }
        
        @Test
        @DisplayName("should not use scientific notation for large numbers")
        void testLargeNumbersNoScientificNotation() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 1000000);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("1000000"));
            assertFalse(encoded.contains("1e6"));
            assertFalse(encoded.contains("1e+6"));
        }
        
        @Test
        @DisplayName("should handle numbers with many decimal places")
        void testManyDecimalPlaces() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 3.141592653589793);
            
            String encoded = Zon.encode(data);
            
            // Should preserve precision
            assertTrue(encoded.contains("3.14159265358979"));
            // Should not contain scientific notation
            assertFalse(encoded.matches(".*\\de[+-]?\\d.*"));
        }
    }
    
    @Nested
    @DisplayName("Special Values")
    class SpecialValuesTests {
        
        @Test
        @DisplayName("should encode NaN as null")
        void testNaNAsNull() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", Double.NaN);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("value:null"));
        }
        
        @Test
        @DisplayName("should encode Infinity as null")
        void testInfinityAsNull() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", Double.POSITIVE_INFINITY);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("value:null"));
        }
        
        @Test
        @DisplayName("should encode -Infinity as null")
        void testNegativeInfinityAsNull() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", Double.NEGATIVE_INFINITY);
            
            String encoded = Zon.encode(data);
            
            assertTrue(encoded.contains("value:null"));
        }
    }
    
    @Nested
    @DisplayName("Round-Trip Preservation")
    class RoundTripPreservationTests {
        
        @Test
        @DisplayName("should preserve integer values through round-trip")
        void testIntegerRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 42);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(42L, ((Number) decoded.get("value")).longValue());
        }
        
        @Test
        @DisplayName("should preserve float values through round-trip")
        void testFloatRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 3.14);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(3.14, ((Number) decoded.get("value")).doubleValue(), 0.0000000001);
        }
        
        @Test
        @DisplayName("should preserve large numbers through round-trip")
        void testLargeNumberRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 1000000);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(1000000L, ((Number) decoded.get("value")).longValue());
        }
        
        @Test
        @DisplayName("should preserve very small numbers through round-trip")
        void testVerySmallNumberRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", 0.000001);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(0.000001, ((Number) decoded.get("value")).doubleValue(), 0.0000000001);
        }
    }
    
    @Nested
    @DisplayName("Array of Numbers")
    class ArrayOfNumbersTests {
        
        @Test
        @DisplayName("should format all numbers canonically in arrays")
        void testArrayNumbersCanonical() {
            List<Map<String, Object>> values = new ArrayList<>();
            
            Map<String, Object> v1 = new LinkedHashMap<>();
            v1.put("num", 1000000);
            values.add(v1);
            
            Map<String, Object> v2 = new LinkedHashMap<>();
            v2.put("num", 0.001);
            values.add(v2);
            
            Map<String, Object> v3 = new LinkedHashMap<>();
            v3.put("num", 42);
            values.add(v3);
            
            Map<String, Object> v4 = new LinkedHashMap<>();
            v4.put("num", 3.14);
            values.add(v4);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("values", values);
            
            String encoded = Zon.encode(data);
            
            // Should not contain scientific notation
            assertFalse(encoded.contains("e+"));
            assertFalse(encoded.contains("e-"));
            assertFalse(encoded.contains("E"));
            
            // Should contain actual values
            assertTrue(encoded.contains("1000000"));
            assertTrue(encoded.contains("0.001"));
            assertTrue(encoded.contains("42"));
            assertTrue(encoded.contains("3.14"));
        }
    }
}
