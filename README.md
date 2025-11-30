# ZON Format for Java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 11+](https://img.shields.io/badge/Java-11%2B-blue)](https://www.java.com/)

**Zero Overhead Notation** - A compact, human-readable data serialization format optimized for LLM token efficiency.

Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)

## Overview

ZON achieves **23.8% token reduction** compared to JSON while maintaining 100% data fidelity and LLM retrieval accuracy. It uses:

- **Single-character primitives**: `T`/`F` for booleans
- **Table format**: Uniform arrays encoded as column headers + data rows
- **Minimal quoting**: Unquoted safe strings

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

## Quick Start

```java
import com.zonformat.zon.Zon;
import java.util.*;

public class Example {
    public static void main(String[] args) {
        // Create data
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Alice");
        data.put("age", 30);
        data.put("active", true);
        
        // Encode to ZON
        String zon = Zon.encode(data);
        System.out.println(zon);
        // Output:
        // active:T
        // age:30
        // name:Alice
        
        // Decode back to Java
        Object decoded = Zon.decode(zon);
    }
}
```

## Table Format

Arrays of uniform objects are encoded efficiently:

```java
List<Map<String, Object>> users = new ArrayList<>();

Map<String, Object> user1 = new LinkedHashMap<>();
user1.put("id", 1);
user1.put("name", "Alice");
user1.put("active", true);
users.add(user1);

Map<String, Object> user2 = new LinkedHashMap<>();
user2.put("id", 2);
user2.put("name", "Bob");
user2.put("active", false);
users.add(user2);

Map<String, Object> data = new LinkedHashMap<>();
data.put("users", users);

String zon = Zon.encode(data);
// Output:
// users:@(2):active,id,name
// T,1,Alice
// F,2,Bob
```

## API Reference

### Encoding

```java
// Basic encoding
String zon = Zon.encode(data);

// Using encoder instance
ZonEncoder encoder = new ZonEncoder();
String zon = encoder.encode(data);
```

### Decoding

```java
// Strict mode (default) - validates row/field counts
Object data = Zon.decode(zonString);

// Non-strict mode - tolerates mismatches
Object data = Zon.decode(zonString, false);

// Using decoder instance
ZonDecoder decoder = new ZonDecoder(false);
Object data = decoder.decode(zonString);
```

### Error Handling

```java
try {
    Object data = Zon.decode(invalidZon);
} catch (ZonDecodeError e) {
    System.out.println("Error code: " + e.getCode());
    System.out.println("Line: " + e.getLine());
    System.out.println("Context: " + e.getContext());
}
```

## CLI Usage

Build the CLI jar:

```bash
mvn package
```

Use the CLI:

```bash
# Encode JSON to ZON
java -jar target/zon-java-1.0.5-cli.jar encode data.json > data.zonf

# Decode ZON to JSON
java -jar target/zon-java-1.0.5-cli.jar decode data.zonf > data.json
```

## Token Efficiency

| Format | Tokens | Savings |
|--------|--------|---------|
| JSON (formatted) | 1,300 | - |
| JSON (compact) | 802 | 38% |
| ZON | 692 | 47% |

## Use Cases

✅ **Use ZON for:**
- LLM prompt contexts (RAG, few-shot examples)
- Configuration files
- Log storage and analysis
- Tabular data interchange

❌ **Don't use ZON for:**
- Public REST APIs (use JSON for compatibility)
- Real-time streaming (not yet supported)
- Files requiring comments (use YAML/JSONC)

## Format Specification

See [SPEC.md](SPEC.md) for the complete ZON format specification.

## License

MIT License - Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)

See [LICENSE](LICENSE) for details.

## Related Projects

- [zon-ts](https://github.com/ZON-Format/zon-TS) - TypeScript implementation
- [ZON Specification](SPEC.md) - Format specification
