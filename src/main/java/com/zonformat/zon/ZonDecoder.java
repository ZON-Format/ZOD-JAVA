/*
 * ZON Decoder v1.0.5 - Compact Hybrid Format
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decoder for converting ZON format strings back to Java objects.
 * 
 * <p>Supports both strict and non-strict modes for validation:</p>
 * <ul>
 *   <li>Strict mode (default): Validates row and field counts</li>
 *   <li>Non-strict mode: Allows mismatches for lenient parsing</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * String zon = "users:@(2):id,name\n1,Alice\n2,Bob";
 * Object decoded = ZonDecoder.decode(zon);
 * // Returns: {users: [{id: 1, name: "Alice"}, {id: 2, name: "Bob"}]}
 * }</pre>
 */
public class ZonDecoder {
    
    private static final Pattern V2_NAMED_PATTERN = Pattern.compile("^@(\\w+)\\((\\d+)\\)(\\[\\w+\\])*:(.+)$");
    private static final Pattern V2_VALUE_PATTERN = Pattern.compile("^@\\((\\d+)\\)(\\[\\w+\\])*:(.+)$");
    private static final Pattern V2_PATTERN = Pattern.compile("^@(\\d+)(\\[\\w+\\])*:(.+)$");
    private static final Pattern V1_PATTERN = Pattern.compile("^@(\\w+)\\((\\d+)\\):(.+)$");
    private static final Pattern OMITTED_COL_PATTERN = Pattern.compile("\\[(\\w+)\\]");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}");
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z_]\\w*$");
    private static final Pattern BLOCK_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\s*[\\{\\[]");
    
    private final boolean strict;
    private int currentLine;
    
    /**
     * Creates a new ZonDecoder with strict mode enabled.
     */
    public ZonDecoder() {
        this(true);
    }
    
    /**
     * Creates a new ZonDecoder with specified strictness.
     * 
     * @param strict Whether to enable strict validation
     */
    public ZonDecoder(boolean strict) {
        this.strict = strict;
        this.currentLine = 0;
    }
    
    /**
     * Decodes a ZON format string to Java objects.
     * 
     * @param zonStr ZON format string
     * @return Decoded Java object (Map, List, or primitive)
     * @throws ZonDecodeError if decoding fails
     */
    public Object decode(String zonStr) {
        if (zonStr == null || zonStr.isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        // Security: Check document size
        if (zonStr.length() > Constants.MAX_DOCUMENT_SIZE) {
            throw new ZonDecodeError(
                "[E301] Document size exceeds maximum (" + Constants.MAX_DOCUMENT_SIZE + " bytes)",
                "E301"
            );
        }
        
        String[] lines = zonStr.trim().split("\n");
        if (lines.length == 0) {
            return new LinkedHashMap<>();
        }
        
        // Special case: Root-level ZON list
        if (lines.length == 1) {
            String line = lines[0].trim();
            if (line.startsWith("[")) {
                return parseZonNode(line, 0);
            }
            
            // Check for colon-less object/array pattern
            boolean hasBlock = BLOCK_PATTERN.matcher(line).find();
            
            if (!line.contains(String.valueOf(Constants.META_SEPARATOR)) && 
                !line.startsWith(String.valueOf(Constants.TABLE_MARKER)) && 
                !hasBlock) {
                return parsePrimitive(line);
            }
        }
        
        // Main decode loop
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, TableInfo> tables = new LinkedHashMap<>();
        TableInfo currentTable = null;
        String currentTableName = null;
        
        for (String line : lines) {
            String trimmedLine = line.stripTrailing();
            currentLine++;
            
            // Security: Check line length
            if (trimmedLine.length() > Constants.MAX_LINE_LENGTH) {
                throw new ZonDecodeError(
                    "[E302] Line length exceeds maximum (" + Constants.MAX_LINE_LENGTH + " chars)",
                    "E302", currentLine, null, null
                );
            }
            
            // Skip blank lines
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // Table header (Anonymous or Legacy): @...
            if (trimmedLine.startsWith(String.valueOf(Constants.TABLE_MARKER))) {
                Object[] parsed = parseTableHeader(trimmedLine);
                currentTableName = (String) parsed[0];
                currentTable = (TableInfo) parsed[1];
                tables.put(currentTableName, currentTable);
            }
            // Table row (if in a table and haven't read all rows)
            else if (currentTable != null && currentTable.rowIndex < currentTable.expectedRows) {
                Map<String, Object> row = parseTableRow(trimmedLine, currentTable);
                currentTable.rows.add(row);
                
                // If we've read all rows, exit table mode
                if (currentTable.rowIndex >= currentTable.expectedRows) {
                    currentTable = null;
                }
            }
            // Metadata line OR Named Table
            else {
                int splitIdx = -1;
                char splitChar = 0;
                int depth = 0;
                boolean inQuote = false;
                
                for (int i = 0; i < trimmedLine.length(); i++) {
                    char c = trimmedLine.charAt(i);
                    if (c == '"') inQuote = !inQuote;
                    if (!inQuote) {
                        if (c == '{' || c == '[') depth++;
                        if (c == '}' || c == ']') depth--;
                        
                        if (depth == 1 && (c == '{' || c == '[')) {
                            if (splitIdx == -1) {
                                splitIdx = i;
                                splitChar = c;
                                break;
                            }
                        }
                        if (c == ':' && depth == 0) {
                            splitIdx = i;
                            splitChar = ':';
                            break;
                        }
                    }
                }
                
                if (splitIdx != -1) {
                    String key;
                    String val;
                    
                    if (splitChar == ':') {
                        key = trimmedLine.substring(0, splitIdx).trim();
                        val = trimmedLine.substring(splitIdx + 1).trim();
                    } else {
                        key = trimmedLine.substring(0, splitIdx).trim();
                        val = trimmedLine.substring(splitIdx).trim();
                    }
                    
                    // Check if it's a named table start
                    if (val.startsWith(String.valueOf(Constants.TABLE_MARKER))) {
                        Object[] parsed = parseTableHeader(val);
                        currentTableName = key;
                        currentTable = (TableInfo) parsed[1];
                        tables.put(currentTableName, currentTable);
                    } else {
                        currentTable = null;
                        metadata.put(key, parseValue(val));
                    }
                }
            }
        }
        
        // Recombine tables into metadata
        for (Map.Entry<String, TableInfo> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            TableInfo table = entry.getValue();
            
            // Strict mode: validate row count
            if (strict && table.rows.size() != table.expectedRows) {
                throw new ZonDecodeError(
                    "[E001] Row count mismatch in table '" + tableName + 
                    "': expected " + table.expectedRows + ", got " + table.rows.size(),
                    "E001", null, null, "Table: " + tableName
                );
            }
            
            metadata.put(tableName, reconstructTable(table));
        }
        
        // Unflatten dotted keys
        Object result = unflatten(metadata);
        
        // Unwrap pure lists
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            if (resultMap.size() == 1 && resultMap.containsKey("data") && resultMap.get("data") instanceof List) {
                return resultMap.get("data");
            }
        }
        
        return result;
    }
    
    /**
     * Convenience static method to decode with default settings.
     * 
     * @param zonStr ZON format string
     * @return Decoded Java object
     */
    public static Object decodeStatic(String zonStr) {
        return new ZonDecoder().decode(zonStr);
    }
    
    /**
     * Convenience static method to decode with specified strictness.
     * 
     * @param zonStr ZON format string
     * @param strict Whether to enable strict validation
     * @return Decoded Java object
     */
    public static Object decodeStatic(String zonStr, boolean strict) {
        return new ZonDecoder(strict).decode(zonStr);
    }
    
    private Object[] parseTableHeader(String line) {
        // Try v2.0 format with name
        Matcher m = V2_NAMED_PATTERN.matcher(line);
        if (m.matches()) {
            String tableName = m.group(1);
            int count = Integer.parseInt(m.group(2));
            String omittedStr = m.group(3) != null ? m.group(3) : "";
            String colsStr = m.group(4);
            
            List<String> omittedCols = parseOmittedCols(omittedStr);
            List<String> cols = parseColumns(colsStr);
            
            return new Object[]{tableName, new TableInfo(cols, omittedCols, count)};
        }
        
        // Try v2.1 format (anonymous/value)
        m = V2_VALUE_PATTERN.matcher(line);
        if (m.matches()) {
            int count = Integer.parseInt(m.group(1));
            String omittedStr = m.group(2) != null ? m.group(2) : "";
            String colsStr = m.group(3);
            
            List<String> omittedCols = parseOmittedCols(omittedStr);
            List<String> cols = parseColumns(colsStr);
            
            return new Object[]{"data", new TableInfo(cols, omittedCols, count)};
        }
        
        // Try v2.0 format (anonymous)
        m = V2_PATTERN.matcher(line);
        if (m.matches()) {
            int count = Integer.parseInt(m.group(1));
            String omittedStr = m.group(2) != null ? m.group(2) : "";
            String colsStr = m.group(3);
            
            List<String> omittedCols = parseOmittedCols(omittedStr);
            List<String> cols = parseColumns(colsStr);
            
            return new Object[]{"data", new TableInfo(cols, omittedCols, count)};
        }
        
        // Try v1.x format
        m = V1_PATTERN.matcher(line);
        if (m.matches()) {
            String tableName = m.group(1);
            int count = Integer.parseInt(m.group(2));
            String colsStr = m.group(3);
            
            List<String> cols = parseColumns(colsStr);
            
            return new Object[]{tableName, new TableInfo(cols, Collections.emptyList(), count)};
        }
        
        throw new ZonDecodeError("Invalid table header: " + line);
    }
    
    private List<String> parseOmittedCols(String omittedStr) {
        List<String> cols = new ArrayList<>();
        if (omittedStr != null && !omittedStr.isEmpty()) {
            Matcher m = OMITTED_COL_PATTERN.matcher(omittedStr);
            while (m.find()) {
                cols.add(m.group(1));
            }
        }
        return cols;
    }
    
    private List<String> parseColumns(String colsStr) {
        String[] parts = colsStr.split(",");
        List<String> cols = new ArrayList<>();
        for (String part : parts) {
            cols.add(part.trim());
        }
        return cols;
    }
    
    private Map<String, Object> parseTableRow(String line, TableInfo table) {
        List<String> tokens = splitByDelimiter(line, ',');
        
        int coreFieldCount = tokens.size();
        int sparseFieldCount = 0;
        
        // Count sparse fields
        for (int i = table.cols.size(); i < tokens.size(); i++) {
            String tok = tokens.get(i);
            if (tok.contains(":") && !isURL(tok) && !isTimestamp(tok)) {
                sparseFieldCount++;
            }
        }
        
        int actualCoreFields = Math.min(coreFieldCount, table.cols.size());
        
        // Strict mode validation
        if (strict && coreFieldCount < table.cols.size() && sparseFieldCount == 0) {
            throw new ZonDecodeError(
                "[E002] Field count mismatch on row " + (table.rowIndex + 1) + 
                ": expected " + table.cols.size() + " fields, got " + coreFieldCount,
                "E002", currentLine, null,
                line.length() > 50 ? line.substring(0, 50) + "..." : line
            );
        }
        
        // Pad if needed
        while (tokens.size() < table.cols.size()) {
            tokens.add("");
        }
        
        Map<String, Object> row = new LinkedHashMap<>();
        int tokenIdx = 0;
        
        // Parse core columns
        for (String col : table.cols) {
            if (tokenIdx < tokens.size()) {
                String tok = tokens.get(tokenIdx);
                row.put(col, parseValue(tok));
                tokenIdx++;
            }
        }
        
        // Parse optional fields (sparse encoding)
        while (tokenIdx < tokens.size()) {
            String tok = tokens.get(tokenIdx);
            if (tok.contains(":") && !isURL(tok) && !isTimestamp(tok)) {
                int colonIdx = tok.indexOf(':');
                String key = tok.substring(0, colonIdx).trim();
                String val = tok.substring(colonIdx + 1).trim();
                
                if (KEY_PATTERN.matcher(key).matches()) {
                    row.put(key, parseValue(val));
                }
            }
            tokenIdx++;
        }
        
        // Reconstruct omitted sequential columns
        if (table.omittedCols != null) {
            for (String col : table.omittedCols) {
                row.put(col, table.rowIndex + 1);
            }
        }
        
        table.rowIndex++;
        return row;
    }
    
    private boolean isURL(String s) {
        return URL_PATTERN.matcher(s).find() || s.startsWith("/");
    }
    
    private boolean isTimestamp(String s) {
        return TIMESTAMP_PATTERN.matcher(s).find() || TIME_PATTERN.matcher(s).find();
    }
    
    private List<Map<String, Object>> reconstructTable(TableInfo table) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : table.rows) {
            result.add(unflattenMap(row));
        }
        return result;
    }
    
    private Object parseZonNode(String text, int depth) {
        if (depth > Constants.MAX_NESTING_DEPTH) {
            throw new ZonDecodeError("Maximum nesting depth exceeded (" + Constants.MAX_NESTING_DEPTH + ")");
        }
        
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        
        // Dict: {k:v,k:v}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String content = trimmed.substring(1, trimmed.length() - 1).trim();
            if (content.isEmpty()) {
                return new LinkedHashMap<>();
            }
            
            Map<String, Object> obj = new LinkedHashMap<>();
            List<String> pairs = splitByDelimiter(content, ',');
            
            // Security: Check object key count
            if (pairs.size() > Constants.MAX_OBJECT_KEYS) {
                throw new ZonDecodeError(
                    "[E304] Object key count exceeds maximum (" + Constants.MAX_OBJECT_KEYS + " keys)",
                    "E304"
                );
            }
            
            for (String pair : pairs) {
                int[] splitResult = findSplitPoint(pair);
                int splitIdx = splitResult[0];
                char splitChar = (char) splitResult[1];
                
                if (splitIdx != -1) {
                    String keyStr, valStr;
                    if (splitChar == ':') {
                        keyStr = pair.substring(0, splitIdx).trim();
                        valStr = pair.substring(splitIdx + 1).trim();
                    } else {
                        keyStr = pair.substring(0, splitIdx).trim();
                        valStr = pair.substring(splitIdx).trim();
                    }
                    
                    Object key = parsePrimitive(keyStr);
                    Object val = parseZonNode(valStr, depth + 1);
                    obj.put(String.valueOf(key), val);
                }
            }
            
            return obj;
        }
        
        // List: [v,v]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String content = trimmed.substring(1, trimmed.length() - 1).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<String> items = splitByDelimiter(content, ',');
            
            // Security: Check array length
            if (items.size() > Constants.MAX_ARRAY_LENGTH) {
                throw new ZonDecodeError(
                    "[E303] Array length exceeds maximum (" + Constants.MAX_ARRAY_LENGTH + " items)",
                    "E303"
                );
            }
            
            List<Object> result = new ArrayList<>();
            for (String item : items) {
                result.add(parseZonNode(item, depth + 1));
            }
            return result;
        }
        
        // Leaf node (primitive)
        return parsePrimitive(trimmed);
    }
    
    private int[] findSplitPoint(String pair) {
        int splitIdx = -1;
        char splitChar = 0;
        boolean inQuote = false;
        char quoteChar = 0;
        int depth = 0;
        
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            
            if (c == '\\' && i + 1 < pair.length()) {
                i++;
                continue;
            }
            
            if (c == '"' || c == '\'') {
                if (!inQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuote = false;
                }
            } else if (!inQuote) {
                if (c == ':') {
                    if (depth == 0) {
                        splitIdx = i;
                        splitChar = ':';
                        break;
                    }
                } else if (c == '{' || c == '[') {
                    if (depth == 0 && splitIdx == -1) {
                        splitIdx = i;
                        splitChar = c;
                        break;
                    }
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                }
            }
        }
        
        return new int[]{splitIdx, splitChar};
    }
    
    private List<String> splitByDelimiter(String text, char delim) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        int depth = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Handle escaped characters
            if (c == '\\' && i + 1 < text.length()) {
                current.append(c);
                current.append(text.charAt(++i));
                continue;
            }
            
            if (c == '"' || c == '\'') {
                if (!inQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuote = false;
                }
                current.append(c);
            } else if (!inQuote) {
                if (c == '{' || c == '[') {
                    depth++;
                    current.append(c);
                } else if (c == '}' || c == ']') {
                    depth--;
                    current.append(c);
                } else if (c == delim && depth == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
    
    private Object parsePrimitive(String val) {
        String trimmed = val.trim();
        String lower = trimmed.toLowerCase();
        
        // Booleans
        if (lower.equals("t") || lower.equals("true")) {
            return true;
        }
        if (lower.equals("f") || lower.equals("false")) {
            return false;
        }
        
        // Null
        if (lower.equals("null") || lower.equals("none") || lower.equals("nil")) {
            return null;
        }
        
        // Quoted string (JSON style)
        if (trimmed.startsWith("\"")) {
            try {
                return parseJsonString(trimmed);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Try number
        if (!trimmed.isEmpty()) {
            try {
                if (trimmed.contains(".") || trimmed.toLowerCase().contains("e")) {
                    return Double.parseDouble(trimmed);
                } else {
                    return Long.parseLong(trimmed);
                }
            } catch (NumberFormatException e) {
                // Not a number
            }
        }
        
        return trimmed;
    }
    
    private Object parseValue(String val) {
        String trimmed = val.trim();
        
        // Quoted string - check BEFORE primitives
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            String content = trimmed.substring(1, trimmed.length() - 1);
            String unquoted;
            
            // Check if it uses CSV-style escaping (doubled quotes) or JSON-style escaping (backslash)
            if (content.contains("\"\"")) {
                // CSV-style: "" becomes "
                unquoted = content.replace("\"\"", "\"");
            } else if (content.contains("\\")) {
                // JSON-style escaping - parse it
                try {
                    unquoted = (String) parseJsonString(trimmed);
                } catch (Exception e) {
                    unquoted = content;
                }
            } else {
                unquoted = content;
            }
            
            // Check if the unquoted content is a ZON structure
            String stripped = unquoted.trim();
            if (stripped.startsWith("{") || stripped.startsWith("[")) {
                return parseZonNode(stripped, 0);
            }
            
            return unquoted;
        }
        
        // Booleans
        String lower = trimmed.toLowerCase();
        if (lower.equals("t") || lower.equals("true")) {
            return true;
        }
        if (lower.equals("f") || lower.equals("false")) {
            return false;
        }
        
        // Null
        if (lower.equals("null") || lower.equals("none") || lower.equals("nil")) {
            return null;
        }
        
        // Check for ZON-style nested structures
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseZonNode(trimmed, 0);
        }
        
        // Try number
        if (!trimmed.isEmpty()) {
            try {
                if (trimmed.contains(".") || trimmed.toLowerCase().contains("e")) {
                    return Double.parseDouble(trimmed);
                } else {
                    return Long.parseLong(trimmed);
                }
            } catch (NumberFormatException e) {
                // Not a number
            }
        }
        
        return trimmed;
    }
    
    private Object parseJsonString(String s) {
        if (!s.startsWith("\"") || !s.endsWith("\"")) {
            throw new IllegalArgumentException("Not a JSON string");
        }
        
        String content = s.substring(1, s.length() - 1);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(++i);
                switch (next) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        if (i + 4 <= content.length()) {
                            String hex = content.substring(i + 1, i + 5);
                            result.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: result.append(next);
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> unflattenMap(Map<String, Object> d) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (!key.contains(".")) {
                result.put(key, value);
                continue;
            }
            
            String[] parts = key.split("\\.");
            
            // Security: Prevent prototype pollution
            boolean hasPrototypePollution = false;
            for (String part : parts) {
                if (part.equals("__proto__") || part.equals("constructor") || part.equals("prototype")) {
                    hasPrototypePollution = true;
                    break;
                }
            }
            if (hasPrototypePollution) {
                continue;
            }
            
            Map<String, Object> target = result;
            
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (!target.containsKey(part)) {
                    target.put(part, new LinkedHashMap<>());
                }
                Object next = target.get(part);
                if (next instanceof Map) {
                    target = (Map<String, Object>) next;
                } else {
                    break;
                }
            }
            
            String finalKey = parts[parts.length - 1];
            if (!finalKey.matches("\\d+")) {
                target.put(finalKey, value);
            }
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private Object unflatten(Map<String, Object> d) {
        return unflattenMap(d);
    }
    
    /**
     * Internal class to track table parsing state.
     */
    private static class TableInfo {
        List<String> cols;
        List<String> omittedCols;
        List<Map<String, Object>> rows;
        int rowIndex;
        int expectedRows;
        
        TableInfo(List<String> cols, List<String> omittedCols, int expectedRows) {
            this.cols = cols;
            this.omittedCols = omittedCols;
            this.rows = new ArrayList<>();
            this.rowIndex = 0;
            this.expectedRows = expectedRows;
        }
    }
}
