# ZON API Reference for Java

Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)

Complete API documentation for `zon-java` v1.0.5.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.zonformat</groupId>
    <artifactId>zon-java</artifactId>
    <version>1.0.5</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.zonformat:zon-java:1.0.5'
```

---

## Main Classes

### `Zon`

Main entry point for ZON encoding and decoding operations.

#### Methods

##### `encode(Object data)`

Encodes Java data to ZON format.

**Parameters:**
- `data` (`Object`) - Java data to encode (Map, List, or primitive)

**Returns:** `String` - ZON-formatted string

**Throws:** `IllegalArgumentException` - if circular reference detected

**Example:**
```java
import com.zonformat.zon.Zon;
import java.util.*;

Map<String, Object> data = new LinkedHashMap<>();
data.put("name", "Alice");
data.put("age", 30);
data.put("active", true);

String encoded = Zon.encode(data);
// Output:
// active:T
// age:30
// name:Alice
```

##### `decode(String zonStr)`

Decodes a ZON format string to Java objects with strict mode enabled.

**Parameters:**
- `zonStr` (`String`) - The ZON-formatted string to decode

**Returns:** `Object` - Decoded Java object (Map, List, or primitive)

**Throws:** `ZonDecodeError` - if decoding fails or validation errors occur

##### `decode(String zonStr, boolean strict)`

Decodes a ZON format string to Java objects with specified strictness.

**Parameters:**
- `zonStr` (`String`) - The ZON-formatted string to decode
- `strict` (`boolean`) - Whether to enable strict validation

**Returns:** `Object` - Decoded Java object (Map, List, or primitive)

**Throws:** `ZonDecodeError` - if decoding fails

**Example:**
```java
import com.zonformat.zon.Zon;

String zonData = "users:@(2):id,name\n1,Alice\n2,Bob";
Object decoded = Zon.decode(zonData);

// Non-strict mode
Object decoded = Zon.decode(zonData, false);
```

##### `getVersion()`

Gets the library version.

**Returns:** `String` - Version string (e.g., "1.0.5")

---

### `ZonEncoder`

Encoder class for converting Java objects to ZON format.

#### Constructors

##### `ZonEncoder()`

Creates a new ZonEncoder with default settings.

##### `ZonEncoder(int anchorInterval)`

Creates a new ZonEncoder with custom anchor interval.

**Parameters:**
- `anchorInterval` (`int`) - Anchor interval for large datasets

#### Methods

##### `encode(Object data)`

Encodes Java data to ZON format.

**Parameters:**
- `data` (`Object`) - Data to encode (Map, List, or primitive)

**Returns:** `String` - ZON-formatted string

**Throws:** `IllegalArgumentException` - if circular reference detected

---

### `ZonDecoder`

Decoder class for converting ZON format strings back to Java objects.

#### Constructors

##### `ZonDecoder()`

Creates a new ZonDecoder with strict mode enabled.

##### `ZonDecoder(boolean strict)`

Creates a new ZonDecoder with specified strictness.

**Parameters:**
- `strict` (`boolean`) - Whether to enable strict validation

#### Methods

##### `decode(String zonStr)`

Decodes a ZON format string to Java objects.

**Parameters:**
- `zonStr` (`String`) - ZON format string

**Returns:** `Object` - Decoded Java object (Map, List, or primitive)

**Throws:** `ZonDecodeError` - if decoding fails

---

### `ZonDecodeError`

Exception thrown when ZON decoding fails.

#### Constructors

##### `ZonDecodeError(String message)`

Creates a new ZonDecodeError with a message.

##### `ZonDecodeError(String message, String code)`

Creates a new ZonDecodeError with a message and error code.

##### `ZonDecodeError(String message, String code, Integer line, Integer column, String context)`

Creates a new ZonDecodeError with full details.

#### Methods

##### `getCode()`

Gets the error code.

**Returns:** `String` - Error code or null

##### `getLine()`

Gets the line number where the error occurred.

**Returns:** `Integer` - Line number or null

##### `getColumn()`

Gets the column position.

**Returns:** `Integer` - Column position or null

##### `getContext()`

Gets the context snippet.

**Returns:** `String` - Context or null

---

## Error Codes

| Code | Description | Example |
|------|-------------|---------|
| `E001` | Row count mismatch | Declared `@(3)` but only 2 rows provided |
| `E002` | Field count mismatch | Declared 3 columns but row has 2 values |
| `E301` | Document size exceeds 100MB | Prevents memory exhaustion |
| `E302` | Line length exceeds 1MB | Prevents buffer overflow |
| `E303` | Array length exceeds 1M items | Prevents excessive iteration |
| `E304` | Object key count exceeds 100K | Prevents hash collision |

---

## Constants

Located in `com.zonformat.zon.Constants`:

```java
public static final char TABLE_MARKER = '@';
public static final char META_SEPARATOR = ':';
public static final long MAX_DOCUMENT_SIZE = 100 * 1024 * 1024;  // 100 MB
public static final int MAX_LINE_LENGTH = 1024 * 1024;          // 1 MB
public static final int MAX_ARRAY_LENGTH = 1_000_000;           // 1M items
public static final int MAX_OBJECT_KEYS = 100_000;              // 100K keys
public static final int MAX_NESTING_DEPTH = 100;                // 100 levels
```

---

## Type Mapping

### Java to ZON

| Java Type | ZON Encoding |
|-----------|--------------|
| `Boolean` (true) | `T` |
| `Boolean` (false) | `F` |
| `null` | `null` |
| `Integer`, `Long` | Number without decimal |
| `Double`, `Float` | Number with decimal |
| `String` | Quoted or unquoted |
| `Map<String, Object>` | Object notation |
| `List<?>` | Array or table |

### ZON to Java

| ZON Value | Java Type |
|-----------|-----------|
| `T`, `true`, `TRUE` | `Boolean` (true) |
| `F`, `false`, `FALSE` | `Boolean` (false) |
| `null`, `none`, `nil` | `null` |
| Integer number | `Long` |
| Decimal number | `Double` |
| String | `String` |
| Object | `LinkedHashMap<String, Object>` |
| Array/Table | `ArrayList<Object>` |

---

## Complete Examples

### Example 1: Simple Object

```java
Map<String, Object> data = new LinkedHashMap<>();
data.put("name", "ZON Format");
data.put("version", "1.0.5");
data.put("active", true);
data.put("score", 98.5);

String encoded = Zon.encode(data);
// active:T
// name:ZON Format
// score:98.5
// version:"1.0.5"

Object decoded = Zon.decode(encoded);
```

### Example 2: Uniform Table

```java
List<Map<String, Object>> employees = new ArrayList<>();

Map<String, Object> e1 = new LinkedHashMap<>();
e1.put("id", 1);
e1.put("name", "Alice");
e1.put("dept", "Eng");
e1.put("salary", 85000);
employees.add(e1);

Map<String, Object> e2 = new LinkedHashMap<>();
e2.put("id", 2);
e2.put("name", "Bob");
e2.put("dept", "Sales");
e2.put("salary", 72000);
employees.add(e2);

Map<String, Object> data = new LinkedHashMap<>();
data.put("employees", employees);

String encoded = Zon.encode(data);
// employees:@(2):dept,id,name,salary
// Eng,1,Alice,85000
// Sales,2,Bob,72000
```

### Example 3: Error Handling

```java
try {
    String invalidZon = "users:@(3):id,name\n1,Alice";
    Object data = Zon.decode(invalidZon);
} catch (ZonDecodeError e) {
    System.out.println("Error: " + e.getMessage());
    System.out.println("Code: " + e.getCode());     // "E001"
    System.out.println("Context: " + e.getContext()); // "Table: users"
}
```

---

## See Also

- [Syntax Cheatsheet](./syntax-cheatsheet.md) - Quick reference
- [Format Specification](../SPEC.md) - Formal grammar
- [LLM Best Practices](./llm-best-practices.md) - Usage guide
- [GitHub Repository](https://github.com/ZON-Format/ZOD-JAVA)
