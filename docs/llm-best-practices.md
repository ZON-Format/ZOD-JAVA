# Using ZON with LLMs - Best Practices

Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)

Guide for maximizing ZON's effectiveness in LLM applications.

## Why ZON for LLMs?

LLM API costs are directly tied to token count. ZON reduces tokens by **23.8% vs JSON** while achieving **100% retrieval accuracy**.

**Key Benefits:**
- 💰 **Lower costs**: Fewer tokens = lower API bills
- 🎯 **Better accuracy**: 100% vs JSON's 91.7%
- 📊 **Self-documenting**: Explicit headers `@(N):columns`
- 🔍 **Human-readable**: Easy to debug and verify

---

## Sending ZON as Input

### Basic Pattern

Wrap ZON data in code blocks with format label:

````markdown
Here's the user data in ZON format:

```zon
users:@(3):active,id,name,role
T,1,Alice,admin
T,2,Bob,user
F,3,Carol,guest
```

Question: How many active users are there?
````

**Why this works:**
- ✅ Code blocks prevent formatting issues
- ✅ `zon` label helps model recognize format
- ✅ Explicit headers (`@(3):columns`) give clear schema

---

## Java Integration Examples

### Building LLM Prompts

```java
import com.zonformat.zon.Zon;
import java.util.*;

public class LLMPromptBuilder {
    public static String buildPrompt(List<Map<String, Object>> data, String question) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("data", data);
        
        String zonData = Zon.encode(wrapper);
        
        return String.format("""
            Here's the data in ZON format:
            
            ```zon
            %s
            ```
            
            Question: %s
            """, zonData, question);
    }
}
```

### Parsing LLM Responses

```java
import com.zonformat.zon.Zon;
import java.util.regex.*;

public class LLMResponseParser {
    private static final Pattern ZON_BLOCK = Pattern.compile("```zon\\n([\\s\\S]*?)```");
    
    public static Object extractZonData(String llmResponse) {
        Matcher matcher = ZON_BLOCK.matcher(llmResponse);
        if (matcher.find()) {
            String zonData = matcher.group(1);
            return Zon.decode(zonData);
        }
        return null;
    }
}
```

---

## Prompting Strategies

### Strategy 1: Show the Format (No Explanation)

**Best approach** - Let the model infer the structure:

````
```zon
products:@(4):category,id,name,price,stock
Electronics,1,Laptop,999,45
Books,2,Python Guide,29.99,120
Electronics,3,Mouse,19.99,200
Books,4,JavaScript Basics,24.95,85
```

Find products with stock below 100.
````

**Why it works:** The explicit headers (`@(4):category,id,name,price,stock`) are self-documenting.

### Strategy 2: Minimal Context

For complex queries, add brief context:

````
Data format: ZON (tabular)  
@(N) = row count  
Column names listed in header

```zon
logs:@(100):level,message,timestamp,userId
ERROR,Database timeout,2025-01-15T10:30:00Z,1001
WARN,High memory usage,2025-01-15T10:31:15Z,1002
ERROR,API rate limit,2025-01-15T10:32:45Z,1001
...
```

How many ERROR logs are from userId 1001?
````

---

## Common Use Cases

### 1. Data Retrieval Questions

**Perfect for ZON** - table format excels here:

```java
List<Map<String, Object>> employees = buildEmployeeList();
Map<String, Object> data = new LinkedHashMap<>();
data.put("employees", employees);

String zon = Zon.encode(data);
String prompt = String.format("""
    ```zon
    %s
    ```
    
    Questions:
    1. What's the average salary in Engineering?
    2. How many inactive employees are there?
    3. List all Sales department employees.
    """, zon);
```

### 2. Configuration Files

```java
// Parse ZON config
String config = """
    environment:production
    database{host:localhost,port:5432,ssl:T}
    cache{ttl:3600,enabled:T}
    features[darkMode,betaAccess,newUI]
    """;

Map<String, Object> settings = (Map<String, Object>) Zon.decode(config);
```

### 3. Structured Logging

```java
public void logEvent(String level, String message, Map<String, Object> context) {
    context.put("level", level);
    context.put("message", message);
    context.put("timestamp", Instant.now().toString());
    
    String zonLog = Zon.encode(context);
    logger.info(zonLog);
}
```

---

## Token Efficiency Tips

### Tip 1: Use Compact Field Names

```zon
# Good ✅ (shorter column names)
u:@(100):id,n,e,a
1,Alice,alice@ex.com,T
2,Bob,bob@ex.com,F

# Acceptable ❌ (verbose names)
users:@(100):userId,fullName,emailAddress,isActive
1,Alice,alice@ex.com,true
2,Bob,bob@ex.com,false
```

**Token savings:** ~20% with compact names

### Tip 2: Boolean Shorthand

ZON uses `T`/`F` instead of `true`/`false`:

```zon
users:@(100):id,name,active,verified
1,Alice,T,T
2,Bob,F,T
3,Carol,T,F
```

**Token savings:** ~40% on boolean fields

---

## Benchmark Results

### Token Efficiency Comparison

| Format | Tokens | Savings vs JSON |
|--------|--------|-----------------|
| JSON (formatted) | 1,300 | - |
| JSON (compact) | 802 | 38% |
| TOON | 874 | 33% |
| CSV | 714 | 45% |
| **ZON** | **692** | **47%** |

### LLM Accuracy (GPT-4o)

| Format | Accuracy | Efficiency Score |
|--------|----------|------------------|
| **ZON** | **99.0%** | **1430.6** |
| CSV | 99.0% | 1386.5 |
| JSON compact | 91.7% | 1143.4 |
| TOON | 99.0% | 1132.7 |
| JSON | 96.8% | 744.6 |

*Efficiency score = (Accuracy % ÷ Tokens) × 10,000. Higher is better.*

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
- [Syntax Cheatsheet](./syntax-cheatsheet.md) - Quick reference
- [API Reference](./api-reference.md) - encode/decode functions
- [Format Specification](../SPEC.md) - Formal grammar
