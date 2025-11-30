/*
 * ZON Codec Tests
 * Port of codec.test.ts from the TypeScript implementation
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
 * Comprehensive codec tests for ZON encoding and decoding.
 */
class ZonCodecTest {
    
    @Nested
    @DisplayName("Round-trip tests")
    class RoundTripTests {
        
        @Test
        @DisplayName("Empty object")
        void testEmptyObject() {
            Map<String, Object> data = new LinkedHashMap<>();
            String encoded = Zon.encode(data);
            Object decoded = Zon.decode(encoded);
            assertTrue(decoded instanceof Map);
            assertTrue(((Map<?, ?>) decoded).isEmpty());
        }
        
        @Test
        @DisplayName("Simple metadata")
        void testSimpleMetadata() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Alice");
            data.put("age", 30);
            data.put("active", true);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("Alice", decoded.get("name"));
            assertEquals(30L, ((Number) decoded.get("age")).longValue());
            assertEquals(true, decoded.get("active"));
        }
        
        @Test
        @DisplayName("Nested object")
        void testNestedObject() {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("age", 25);
            profile.put("city", "NYC");
            
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("name", "Bob");
            user.put("profile", profile);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertNotNull(decoded.get("user"));
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedUser = (Map<String, Object>) decoded.get("user");
            assertEquals("Bob", decodedUser.get("name"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedProfile = (Map<String, Object>) decodedUser.get("profile");
            assertEquals(25L, ((Number) decodedProfile.get("age")).longValue());
            assertEquals("NYC", decodedProfile.get("city"));
        }
        
        @Test
        @DisplayName("Array of objects (table)")
        void testArrayOfObjects() {
            List<Map<String, Object>> data = new ArrayList<>();
            
            Map<String, Object> item1 = new LinkedHashMap<>();
            item1.put("id", 1);
            item1.put("name", "Alice");
            item1.put("score", 95);
            data.add(item1);
            
            Map<String, Object> item2 = new LinkedHashMap<>();
            item2.put("id", 2);
            item2.put("name", "Bob");
            item2.put("score", 87);
            data.add(item2);
            
            Map<String, Object> item3 = new LinkedHashMap<>();
            item3.put("id", 3);
            item3.put("name", "Charlie");
            item3.put("score", 92);
            data.add(item3);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decoded = (List<Map<String, Object>>) Zon.decode(encoded);
            
            assertEquals(3, decoded.size());
            assertEquals("Alice", decoded.get(0).get("name"));
            assertEquals(87L, ((Number) decoded.get(1).get("score")).longValue());
        }
        
        @Test
        @DisplayName("Mixed metadata and table")
        void testMixedMetadataAndTable() {
            List<Map<String, Object>> records = new ArrayList<>();
            
            Map<String, Object> r1 = new LinkedHashMap<>();
            r1.put("month", "Jan");
            r1.put("sales", 1000);
            records.add(r1);
            
            Map<String, Object> r2 = new LinkedHashMap<>();
            r2.put("month", "Feb");
            r2.put("sales", 1200);
            records.add(r2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", "Sales Report");
            data.put("year", 2024);
            data.put("records", records);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("Sales Report", decoded.get("title"));
            assertEquals(2024L, ((Number) decoded.get("year")).longValue());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decodedRecords = (List<Map<String, Object>>) decoded.get("records");
            assertEquals(2, decodedRecords.size());
            assertEquals("Jan", decodedRecords.get(0).get("month"));
        }
        
        @Test
        @DisplayName("Boolean values")
        void testBooleanValues() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("active", true);
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("active", false);
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("success", true);
            data.put("error", false);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(true, decoded.get("success"));
            assertEquals(false, decoded.get("error"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decodedItems = (List<Map<String, Object>>) decoded.get("items");
            assertEquals(true, decodedItems.get(0).get("active"));
            assertEquals(false, decodedItems.get(1).get("active"));
        }
        
        @Test
        @DisplayName("Null values")
        void testNullValues() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("data", null);
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("data", "value");
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Test");
            data.put("value", null);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("Test", decoded.get("name"));
            assertNull(decoded.get("value"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decodedItems = (List<Map<String, Object>>) decoded.get("items");
            assertNull(decodedItems.get(0).get("data"));
            assertEquals("value", decodedItems.get(1).get("data"));
        }
        
        @Test
        @DisplayName("Numbers (integers and floats)")
        void testNumbers() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("value", 100);
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("value", 200.5);
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("integer", 42);
            data.put("float", 3.14);
            data.put("negative", -10);
            data.put("negativeFloat", -2.5);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(42L, ((Number) decoded.get("integer")).longValue());
            assertEquals(3.14, ((Number) decoded.get("float")).doubleValue(), 0.001);
            assertEquals(-10L, ((Number) decoded.get("negative")).longValue());
            assertEquals(-2.5, ((Number) decoded.get("negativeFloat")).doubleValue(), 0.001);
        }
        
        @Test
        @DisplayName("Strings with special characters")
        void testStringsWithSpecialCharacters() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("text", "normal");
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("text", "with, comma");
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("plain", "hello");
            data.put("withComma", "hello, world");
            data.put("withQuotes", "say \"hello\"");
            data.put("withNewline", "line1\nline2");
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("hello", decoded.get("plain"));
            assertEquals("hello, world", decoded.get("withComma"));
            assertEquals("say \"hello\"", decoded.get("withQuotes"));
            assertEquals("line1\nline2", decoded.get("withNewline"));
        }
        
        @Test
        @DisplayName("Empty arrays")
        void testEmptyArrays() {
            Map<String, Object> nested = new LinkedHashMap<>();
            nested.put("also_empty", new ArrayList<>());
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("empty", new ArrayList<>());
            data.put("nested", nested);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertTrue(decoded.get("empty") instanceof List);
            assertTrue(((List<?>) decoded.get("empty")).isEmpty());
        }
        
        @Test
        @DisplayName("Nested arrays in metadata")
        void testNestedArraysInMetadata() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("values", Arrays.asList(10, 20));
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("values", Arrays.asList(30, 40));
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("javascript", "typescript", "node"));
            data.put("matrix", Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) decoded.get("tags");
            assertEquals(3, tags.size());
            assertEquals("javascript", tags.get(0));
        }
        
        @Test
        @DisplayName("Complex nested objects in table cells")
        void testComplexNestedObjectsInTableCells() {
            List<Map<String, Object>> data = new ArrayList<>();
            
            Map<String, Object> meta1 = new LinkedHashMap<>();
            meta1.put("tags", Arrays.asList("a", "b"));
            meta1.put("count", 5);
            
            Map<String, Object> item1 = new LinkedHashMap<>();
            item1.put("id", 1);
            item1.put("metadata", meta1);
            data.add(item1);
            
            Map<String, Object> meta2 = new LinkedHashMap<>();
            meta2.put("tags", Arrays.asList("c"));
            meta2.put("count", 3);
            
            Map<String, Object> item2 = new LinkedHashMap<>();
            item2.put("id", 2);
            item2.put("metadata", meta2);
            data.add(item2);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decoded = (List<Map<String, Object>>) Zon.decode(encoded);
            
            assertEquals(2, decoded.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMeta1 = (Map<String, Object>) decoded.get(0).get("metadata");
            assertEquals(5L, ((Number) decodedMeta1.get("count")).longValue());
        }
    }
    
    @Nested
    @DisplayName("Hikes example from README")
    class HikesExampleTests {
        
        @Test
        @DisplayName("Full hikes example")
        void testFullHikesExample() {
            // Context
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("task", "Our favorite hikes together");
            context.put("location", "Boulder");
            context.put("season", "spring_2025");
            
            // Friends
            List<String> friends = Arrays.asList("ana", "luis", "sam");
            
            // Hikes
            List<Map<String, Object>> hikes = new ArrayList<>();
            
            Map<String, Object> h1 = new LinkedHashMap<>();
            h1.put("id", 1);
            h1.put("name", "Blue Lake Trail");
            h1.put("distanceKm", 7.5);
            h1.put("elevationGain", 320);
            h1.put("companion", "ana");
            h1.put("wasSunny", true);
            hikes.add(h1);
            
            Map<String, Object> h2 = new LinkedHashMap<>();
            h2.put("id", 2);
            h2.put("name", "Ridge Overlook");
            h2.put("distanceKm", 9.2);
            h2.put("elevationGain", 540);
            h2.put("companion", "luis");
            h2.put("wasSunny", false);
            hikes.add(h2);
            
            Map<String, Object> h3 = new LinkedHashMap<>();
            h3.put("id", 3);
            h3.put("name", "Wildflower Loop");
            h3.put("distanceKm", 5.1);
            h3.put("elevationGain", 180);
            h3.put("companion", "sam");
            h3.put("wasSunny", true);
            hikes.add(h3);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("context", context);
            data.put("friends", friends);
            data.put("hikes", hikes);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            // Verify structure
            assertNotNull(decoded.get("context"));
            assertNotNull(decoded.get("friends"));
            assertNotNull(decoded.get("hikes"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decodedHikes = (List<Map<String, Object>>) decoded.get("hikes");
            assertEquals(3, decodedHikes.size());
            assertEquals("Blue Lake Trail", decodedHikes.get(0).get("name"));
            assertEquals(true, decodedHikes.get(0).get("wasSunny"));
            assertEquals(false, decodedHikes.get(1).get("wasSunny"));
            
            // Verify the encoded format structure
            assertTrue(encoded.contains("hikes:@(3):"));
            assertTrue(encoded.contains("companion,distanceKm,elevationGain,id,name,wasSunny"));
        }
    }
    
    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("String that looks like a number")
        void testStringLooksLikeNumber() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("code", "001");
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("code", "002");
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stringNumber", "123");
            data.put("actualNumber", 123);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertTrue(decoded.get("stringNumber") instanceof String);
            assertTrue(decoded.get("actualNumber") instanceof Number);
        }
        
        @Test
        @DisplayName("String that looks like boolean")
        void testStringLooksLikeBoolean() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("status", "T");
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("status", true);
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stringTrue", "true");
            data.put("actualTrue", true);
            data.put("stringFalse", "false");
            data.put("actualFalse", false);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertTrue(decoded.get("stringTrue") instanceof String);
            assertTrue(decoded.get("actualTrue") instanceof Boolean);
            assertTrue(decoded.get("stringFalse") instanceof String);
            assertTrue(decoded.get("actualFalse") instanceof Boolean);
        }
        
        @Test
        @DisplayName("Empty strings")
        void testEmptyStrings() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("name", "");
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("name", "value");
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("empty", "");
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("", decoded.get("empty"));
        }
        
        @Test
        @DisplayName("Whitespace preservation")
        void testWhitespacePreservation() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("text", "  padded  ");
            items.add(i1);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("leading", "  space");
            data.put("trailing", "space  ");
            data.put("both", "  both  ");
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals("  space", decoded.get("leading"));
            assertEquals("space  ", decoded.get("trailing"));
            assertEquals("  both  ", decoded.get("both"));
        }
        
        @Test
        @DisplayName("Very long strings")
        void testVeryLongStrings() {
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longString.append("a");
            }
            
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("text", longString.toString());
            items.add(i1);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("long", longString.toString());
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(longString.toString(), decoded.get("long"));
        }
        
        @Test
        @DisplayName("Large arrays")
        void testLargeArrays() {
            List<Map<String, Object>> items = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", i + 1);
                item.put("name", "Item " + (i + 1));
                item.put("value", i * 10);
                items.add(item);
            }
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decodedItems = (List<Map<String, Object>>) decoded.get("items");
            assertEquals(100, decodedItems.size());
        }
        
        @Test
        @DisplayName("Array of primitives")
        void testArrayOfPrimitives() {
            List<String> data = Arrays.asList("apple", "banana", "cherry");
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            List<String> decoded = (List<String>) Zon.decode(encoded);
            
            assertEquals(3, decoded.size());
            assertEquals("apple", decoded.get(0));
            assertEquals("banana", decoded.get(1));
            assertEquals("cherry", decoded.get(2));
            
            // Should be encoded as array, not table
            assertTrue(encoded.startsWith("["));
        }
        
        @Test
        @DisplayName("Deeply nested objects")
        void testDeeplyNestedObjects() {
            Map<String, Object> level4 = new LinkedHashMap<>();
            level4.put("value", "deep");
            
            Map<String, Object> level3 = new LinkedHashMap<>();
            level3.put("level4", level4);
            
            Map<String, Object> level2 = new LinkedHashMap<>();
            level2.put("level3", level3);
            
            Map<String, Object> level1 = new LinkedHashMap<>();
            level1.put("level2", level2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("level1", level1);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertNotNull(decoded.get("level1"));
        }
    }
    
    @Nested
    @DisplayName("Data type preservation")
    class DataTypePreservationTests {
        
        @Test
        @DisplayName("Integer vs float distinction")
        void testIntegerFloatDistinction() {
            List<Map<String, Object>> items = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("intVal", 100);
            i1.put("floatVal", 100.5);
            items.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("intVal", 200);
            i2.put("floatVal", 200.0);
            items.add(i2);
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("integer", 42);
            data.put("float", 42.0);
            data.put("explicitFloat", 3.14);
            data.put("items", items);
            
            String encoded = Zon.encode(data);
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = (Map<String, Object>) Zon.decode(encoded);
            
            assertEquals(42L, ((Number) decoded.get("integer")).longValue());
            assertEquals(3.14, ((Number) decoded.get("explicitFloat")).doubleValue(), 0.001);
        }
        
        @Test
        @DisplayName("Boolean shorthand T/F")
        void testBooleanShorthand() {
            List<Map<String, Object>> data = new ArrayList<>();
            
            Map<String, Object> i1 = new LinkedHashMap<>();
            i1.put("id", 1);
            i1.put("flag", true);
            data.add(i1);
            
            Map<String, Object> i2 = new LinkedHashMap<>();
            i2.put("id", 2);
            i2.put("flag", false);
            data.add(i2);
            
            Map<String, Object> i3 = new LinkedHashMap<>();
            i3.put("id", 3);
            i3.put("flag", true);
            data.add(i3);
            
            String encoded = Zon.encode(data);
            
            // Check that booleans are encoded as T/F
            assertTrue(encoded.contains(",T") || encoded.contains("T,"));
            assertTrue(encoded.contains(",F") || encoded.contains("F,"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> decoded = (List<Map<String, Object>>) Zon.decode(encoded);
            assertEquals(true, decoded.get(0).get("flag"));
            assertEquals(false, decoded.get(1).get("flag"));
            assertEquals(true, decoded.get(2).get("flag"));
        }
    }
}
