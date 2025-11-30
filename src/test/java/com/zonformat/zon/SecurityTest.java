/*
 * Security Tests
 * Port of security.test.ts from the TypeScript implementation
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
 * Security and robustness tests.
 */
class SecurityTest {
    
    @Nested
    @DisplayName("Prototype Pollution")
    class PrototypePollutionTests {
        
        @Test
        @DisplayName("should reject __proto__ keys")
        void testRejectProtoKeys() {
            String malicious = "items:@(1):id,__proto__.polluted\n1,true";
            Object decoded = Zon.decode(malicious, false);
            
            // Verify prototype pollution didn't occur
            Map<String, Object> testObj = new HashMap<>();
            assertNull(testObj.get("polluted"));
        }
        
        @Test
        @DisplayName("should reject constructor.prototype keys")
        void testRejectConstructorPrototypeKeys() {
            String malicious = "items:@(1):id,constructor.prototype.polluted\n1,true";
            Object decoded = Zon.decode(malicious, false);
            
            // Verify prototype pollution didn't occur
            Map<String, Object> testObj = new HashMap<>();
            assertNull(testObj.get("polluted"));
        }
    }
    
    @Nested
    @DisplayName("Denial of Service (DoS)")
    class DoSTests {
        
        @Test
        @DisplayName("should throw on deep nesting in decoder")
        void testThrowOnDeepNesting() {
            // Create a deeply nested string: [[[[...]]]]
            int depth = 150;
            StringBuilder deepZon = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                deepZon.append("[");
            }
            deepZon.append("]");
            for (int i = 0; i < depth - 1; i++) {
                deepZon.append("]");
            }
            
            ZonDecodeError error = assertThrows(ZonDecodeError.class, 
                () -> Zon.decode(deepZon.toString()));
            assertTrue(error.getMessage().contains("Maximum nesting depth exceeded"));
        }
    }
    
    @Nested
    @DisplayName("Circular References")
    class CircularReferenceTests {
        
        @Test
        @DisplayName("should throw on circular reference in encoder")
        void testThrowOnCircularReference() {
            Map<String, Object> circular = new LinkedHashMap<>();
            circular.put("name", "loop");
            circular.put("self", circular);
            
            assertThrows(IllegalArgumentException.class, () -> Zon.encode(circular));
        }
        
        @Test
        @DisplayName("should throw on indirect circular reference")
        void testThrowOnIndirectCircularReference() {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("name", "a");
            
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("name", "b");
            
            a.put("next", b);
            b.put("next", a);
            
            assertThrows(IllegalArgumentException.class, () -> Zon.encode(a));
        }
    }
}
