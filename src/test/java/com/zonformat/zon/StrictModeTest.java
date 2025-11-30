/*
 * Strict Mode Validation Tests
 * Port of strict-mode.test.ts from the TypeScript implementation
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
 * Tests for strict mode validation.
 */
class StrictModeTest {
    
    @Nested
    @DisplayName("E001: Row Count Mismatch")
    class RowCountMismatchTests {
        
        @Test
        @DisplayName("should throw when table has fewer rows than declared (strict mode)")
        void testThrowOnFewerRows() {
            String zonData = "users:@(3):id,name\n1,Alice\n2,Bob";
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(zonData));
            assertTrue(error.getMessage().contains("Row count mismatch"));
            assertEquals("E001", error.getCode());
        }
        
        @Test
        @DisplayName("should allow row count mismatch in non-strict mode")
        void testAllowMismatchInNonStrictMode() {
            String zonData = "users:@(3):id,name\n1,Alice\n2,Bob";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData, false);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            // Non-strict mode allows fewer rows
            assertEquals(2, users.size());
        }
        
        @Test
        @DisplayName("should pass when row count matches (strict mode)")
        void testPassWhenRowCountMatches() {
            String zonData = "users:@(2):id,name\n1,Alice\n2,Bob";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            assertEquals(2, users.size());
            assertEquals(1L, ((Number) users.get(0).get("id")).longValue());
            assertEquals("Alice", users.get(0).get("name"));
        }
    }
    
    @Nested
    @DisplayName("E002: Field Count Mismatch")
    class FieldCountMismatchTests {
        
        @Test
        @DisplayName("should throw when row has fewer fields than declared columns (strict mode)")
        void testThrowOnFewerFields() {
            String zonData = "users:@(2):id,name,role\n1,Alice\n2,Bob,admin";
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(zonData));
            assertTrue(error.getMessage().contains("Field count mismatch"));
            assertEquals("E002", error.getCode());
        }
        
        @Test
        @DisplayName("should allow missing fields in non-strict mode")
        void testAllowMissingFieldsInNonStrictMode() {
            String zonData = "users:@(2):id,name,role\n1,Alice\n2,Bob,admin";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData, false);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            assertEquals(2, users.size());
            assertEquals(1L, ((Number) users.get(0).get("id")).longValue());
            assertEquals("Alice", users.get(0).get("name"));
            assertEquals("admin", users.get(1).get("role"));
        }
        
        @Test
        @DisplayName("should pass when all rows have correct field count (strict mode)")
        void testPassWhenFieldCountMatches() {
            String zonData = "users:@(2):id,name,role\n1,Alice,user\n2,Bob,admin";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            assertEquals(2, users.size());
            assertEquals("user", users.get(0).get("role"));
        }
        
        @Test
        @DisplayName("should allow sparse fields even in strict mode")
        void testAllowSparseFieldsInStrictMode() {
            String zonData = "users:@(2):id,name\n1,Alice,role:admin,score:98\n2,Bob";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            assertEquals(1L, ((Number) users.get(0).get("id")).longValue());
            assertEquals("Alice", users.get(0).get("name"));
            assertEquals("admin", users.get(0).get("role"));
            assertEquals(98L, ((Number) users.get(0).get("score")).longValue());
            assertEquals(2L, ((Number) users.get(1).get("id")).longValue());
            assertEquals("Bob", users.get(1).get("name"));
        }
    }
    
    @Nested
    @DisplayName("Error Details")
    class ErrorDetailsTests {
        
        @Test
        @DisplayName("should include error code in error object")
        void testErrorCode() {
            String zonData = "users:@(2):id,name\n1,Alice";
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(zonData));
            assertEquals("E001", error.getCode());
        }
        
        @Test
        @DisplayName("should include context in error message")
        void testErrorContext() {
            String zonData = "users:@(2):id,name\n1,Alice";
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(zonData));
            assertNotNull(error.getContext());
            assertTrue(error.toString().contains("Table: users"));
        }
    }
    
    @Nested
    @DisplayName("Default Behavior")
    class DefaultBehaviorTests {
        
        @Test
        @DisplayName("strict mode should be enabled by default")
        void testStrictModeEnabledByDefault() {
            String zonData = "users:@(2):id,name\n1,Alice";
            
            // Should throw because default is strict: true
            assertThrows(ZonDecodeError.class, () -> Zon.decode(zonData));
        }
        
        @Test
        @DisplayName("can explicitly enable strict mode")
        void testExplicitStrictMode() {
            String zonData = "users:@(2):id,name\n1,Alice";
            
            assertThrows(ZonDecodeError.class, () -> Zon.decode(zonData, true));
        }
    }
    
    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenariosTests {
        
        @Test
        @DisplayName("should validate multiple tables independently")
        void testMultipleTables() {
            String zonData = "users:@(2):id,name\n1,Alice\n2,Bob\nproducts:@(1):id,title\n100,Widget";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
            
            assertEquals(2, users.size());
            assertEquals(1, products.size());
        }
        
        @Test
        @DisplayName("should work with valid data across multiple tables")
        void testValidMultipleTables() {
            String zonData = "users:@(2):id,name\n1,Alice\n2,Bob\nproducts:@(2):id,title\n100,Widget\n200,Gadget";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) Zon.decode(zonData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
            
            assertEquals(2, users.size());
            assertEquals(2, products.size());
        }
    }
}
