# ZON Syntax Cheatsheet

Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)

Quick reference for ZON format syntax. Cross-referenced with actual implementation in v1.0.5.

## Basic Types

### Primitives

```zon
# String (unquoted when safe)
name:Alice

# Number
score:98.5
count:42

# Boolean (T/F)
active:T
disabled:F

# Null
value:null
```

### Objects

```zon
# Simple object
name:ZON Format
version:1.0.5
active:T
score:98.5
```

**JSON equivalent:**
```json
{
  "name": "ZON Format",
  "version": "1.0.5",
  "active": true,
  "score": 98.5
}
```

### Nested Objects

**Colon-less Syntax (v2.0.5):**
```zon
# Colon is optional if value starts with { or [
config{database{host:localhost,port:5432},cache{ttl:3600,enabled:T}}
```

**Legacy Quoted (v1.x):**
```zon
config:"{database:{host:localhost,port:5432}}"
```

---

## Arrays

### Primitive Arrays (Inline)

```zon
tags:"[nodejs,typescript,llm]"
numbers:"[1,2,3,4,5]"
flags:"[T,F,T]"
```

### Tabular Arrays (Uniform Objects)

**Most efficient form - ZON's specialty**

```zon
users:@(3):active,id,name,role
T,1,Alice,admin
T,2,Bob,user
F,3,Carol,guest
```

**Breakdown:**
- `@(3)` = 3 rows
- `:active,id,name,role` = column headers (alphabetically sorted)
- Data rows follow

**JSON equivalent:**
```json
{
  "users": [
    { "id": 1, "name": "Alice", "role": "admin", "active": true },
    { "id": 2, "name": "Bob", "role": "user", "active": true },
    { "id": 3, "name": "Carol", "role": "guest", "active": false }
  ]
}
```

### Empty Containers

```zon
# Empty object
metadata:"{}"

# Empty array
tags:"[]"
```

---

## Quoting Rules

### When Strings NEED Quotes

1. **Contains special characters**:
   - Commas: `"hello, world"`
   - Brackets: `"[test]"`
   - Braces: `"{test}"`

2. **Looks like a literal**:
   - `"true"` (string, not boolean)
   - `"123"` (string, not number)
   - `"false"` (string, not boolean)
   - `"null"` (string, not null)

3. **Leading/trailing spaces**:
   - `"  padded  "`

4. **Empty string**:
   - `""` (MUST quote, otherwise parses as `null`)

### Safe Unquoted Strings

```zon
# Alphanumeric + dash, underscore, dot
name:john-doe
file:data_v1.json
host:api.example.com
```

---

## Table Headers

### Basic Header (with count)

```zon
users:@(2):id,name,active
1,Alice,T
2,Bob,F
```

**Best practice**: Always include count `@(N)` for explicit schema

---

## Type Conversions

| ZON | Java | Notes |
|-----|------|-------|
| `T` | `Boolean.TRUE` | Boolean true |
| `F` | `Boolean.FALSE` | Boolean false |
| `null` | `null` | Null value |
| `42` | `Long` | Integer number |
| `3.14` | `Double` | Decimal number |
| `hello` | `String` | Unquoted string |
| `"hello"` | `String` | Quoted string |

---

## Java Code Examples

### Encoding

```java
import com.zonformat.zon.Zon;
import java.util.*;

// Simple object
Map<String, Object> data = new LinkedHashMap<>();
data.put("name", "Alice");
data.put("age", 30);
data.put("active", true);

String zon = Zon.encode(data);
// active:T
// age:30
// name:Alice
```

### Decoding

```java
import com.zonformat.zon.Zon;

String zonData = """
    users:@(2):id,name
    1,Alice
    2,Bob
    """;

Object decoded = Zon.decode(zonData);
// Returns: {users=[{id=1, name=Alice}, {id=2, name=Bob}]}
```

### Error Handling

```java
import com.zonformat.zon.*;

try {
    Object data = Zon.decode(invalidZon);
} catch (ZonDecodeError e) {
    System.out.println("Error: " + e.getCode());
    System.out.println("Message: " + e.getMessage());
    System.out.println("Context: " + e.getContext());
}
```

---

## Escape Sequences

Within quoted strings:
- `""` - Double quote (CSV-style)
- `\n` - Newline
- `\r` - Carriage return
- `\t` - Tab
- `\\` - Backslash

**Example:**
```zon
message:"Line 1\nLine 2"
quote:"He said ""hello"""
```

---

## Complete Example

**JSON:**
```json
{
  "metadata": { "version": "1.0.5", "env": "production" },
  "users": [
    { "id": 1, "name": "Alice", "active": true, "loginCount": 42 },
    { "id": 2, "name": "Bob", "active": true, "loginCount": 17 },
    { "id": 3, "name": "Carol", "active": false, "loginCount": 3 }
  ],
  "config": { "database": { "host": "localhost", "port": 5432 } }
}
```

**ZON:**
```zon
metadata{version:1.0.5,env:production}
users:@(3):active,id,loginCount,name
T,1,42,Alice
T,2,17,Bob
F,3,3,Carol
config.database{host:localhost,port:5432}
```

**Token count:**
- JSON (formatted): 151 tokens
- ZON: 87 tokens  
- **Savings: 42% fewer tokens**

---

## Quick Reference

### Do's ✅
- Use code blocks for formatting
- Include `@(N)` row counts
- List column names explicitly
- Use `T`/`F` for booleans
- Use `null` for null values

### Don'ts ❌
- Don't explain ZON syntax (show, don't tell)
- Don't mix formats (stick to ZON)
- Don't omit row counts
- Don't use verbose field names unnecessarily

---

**See also:**
- [API Reference](./api-reference.md) - encode/decode functions
- [Format Specification](../SPEC.md) - Formal grammar
- [LLM Best Practices](./llm-best-practices.md) - Usage guide
