/*
 * ZON Encoder v1.0.5 - Compact Hybrid Format
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Encoder for converting Java objects to ZON format.
 * 
 * <p>ZON (Zero Overhead Notation) is a compact, human-readable data format
 * optimized for LLM token efficiency. It achieves 35-50% token reduction
 * vs JSON through tabular encoding, single-character primitives, and
 * intelligent compression while maintaining 100% data fidelity.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Map<String, Object> data = new HashMap<>();
 * data.put("name", "Alice");
 * data.put("age", 30);
 * data.put("active", true);
 * 
 * String encoded = ZonEncoder.encode(data);
 * // Output:
 * // active:T
 * // age:30
 * // name:Alice
 * }</pre>
 */
public class ZonEncoder {
    
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");
    private static final Pattern ISO_DATE_FULL = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$");
    private static final Pattern ISO_DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_ONLY = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");
    private static final Pattern PURE_INTEGER = Pattern.compile("^-?\\d+$");
    private static final Pattern PURE_DECIMAL = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final Pattern SCIENTIFIC_NOTATION = Pattern.compile("^-?\\d+(\\.\\d+)?[eE][+-]?\\d+$");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1f]");
    
    private final int anchorInterval;
    private final Set<Object> visited = new HashSet<>();
    
    /**
     * Creates a new ZonEncoder with default settings.
     */
    public ZonEncoder() {
        this(Constants.DEFAULT_ANCHOR_INTERVAL);
    }
    
    /**
     * Creates a new ZonEncoder with custom anchor interval.
     * 
     * @param anchorInterval Anchor interval for large datasets
     */
    public ZonEncoder(int anchorInterval) {
        this.anchorInterval = anchorInterval;
    }
    
    /**
     * Encodes Java data to ZON format.
     * 
     * @param data Data to encode (Map, List, or primitive)
     * @return ZON-formatted string
     * @throws IllegalArgumentException if circular reference detected
     */
    public String encode(Object data) {
        visited.clear();
        
        if (data == null) {
            return "null";
        }
        
        // Handle primitives at root level
        if (isPrimitive(data)) {
            return formatValue(data);
        }
        
        // Handle arrays at root level
        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) data;
            
            if (list.isEmpty()) {
                return "[]";
            }
            
            // Check if it's a uniform array of objects (table candidate)
            if (isUniformObjectArray(list)) {
                double irregularity = calculateIrregularity(list);
                if (irregularity <= 0.6) {
                    // Use table format
                    List<String> output = writeTable(list, null);
                    return String.join("\n", output);
                }
            }
            
            // Otherwise use list format
            return formatZonNode(data);
        }
        
        // Handle maps/objects
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            
            if (map.isEmpty()) {
                return "";
            }
            
            // Extract primary stream (table) from map
            Object[] extraction = extractPrimaryStream(map);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> streamData = (List<Map<String, Object>>) extraction[0];
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) extraction[1];
            String streamKey = (String) extraction[2];
            
            List<String> output = new ArrayList<>();
            
            // Write metadata
            if (!metadata.isEmpty()) {
                output.addAll(writeMetadata(metadata));
            }
            
            // Write table if exists
            if (streamData != null && streamKey != null) {
                if (!output.isEmpty()) {
                    output.add("");  // Blank line separator
                }
                output.addAll(writeTable(streamData, streamKey));
            }
            
            return String.join("\n", output);
        }
        
        // Fallback
        return formatZonNode(data);
    }
    
    /**
     * Convenience static method to encode data.
     * 
     * @param data Data to encode
     * @return ZON-formatted string
     */
    public static String encodeStatic(Object data) {
        return new ZonEncoder().encode(data);
    }
    
    private boolean isPrimitive(Object data) {
        return data == null ||
               data instanceof String ||
               data instanceof Number ||
               data instanceof Boolean;
    }
    
    @SuppressWarnings("unchecked")
    private boolean isUniformObjectArray(List<Object> list) {
        if (list.isEmpty()) return false;
        
        for (Object item : list) {
            if (!(item instanceof Map)) {
                return false;
            }
        }
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private double calculateIrregularity(List<Object> data) {
        if (data.isEmpty()) return 0;
        
        Set<String> allKeys = new HashSet<>();
        List<Set<String>> keySets = new ArrayList<>();
        
        for (Object item : data) {
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                Set<String> keys = new HashSet<>(map.keySet());
                keySets.add(keys);
                allKeys.addAll(keys);
            }
        }
        
        if (allKeys.isEmpty()) return 0;
        
        // Calculate average Jaccard similarity
        double totalOverlap = 0;
        int comparisons = 0;
        
        for (int i = 0; i < keySets.size(); i++) {
            for (int j = i + 1; j < keySets.size(); j++) {
                Set<String> keys1 = keySets.get(i);
                Set<String> keys2 = keySets.get(j);
                
                // Count shared keys
                Set<String> intersection = new HashSet<>(keys1);
                intersection.retainAll(keys2);
                int shared = intersection.size();
                
                // Jaccard similarity
                int union = keys1.size() + keys2.size() - shared;
                double similarity = union > 0 ? (double) shared / union : 1.0;
                
                totalOverlap += similarity;
                comparisons++;
            }
        }
        
        if (comparisons == 0) return 0;
        
        double avgSimilarity = totalOverlap / comparisons;
        return 1.0 - avgSimilarity;
    }
    
    @SuppressWarnings("unchecked")
    private Object[] extractPrimaryStream(Map<String, Object> data) {
        // Find largest list of objects
        List<Object[]> candidates = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    // Score = rows * columns
                    Map<String, Object> first = (Map<String, Object>) list.get(0);
                    int score = list.size() * first.size();
                    candidates.add(new Object[]{key, list, score});
                }
            }
        }
        
        if (!candidates.isEmpty()) {
            // Sort by score (descending), then alphabetically by key
            candidates.sort((a, b) -> {
                int scoreCompare = Integer.compare((Integer) b[2], (Integer) a[2]);
                if (scoreCompare != 0) return scoreCompare;
                return ((String) a[0]).compareTo((String) b[0]);
            });
            
            String streamKey = (String) candidates.get(0)[0];
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stream = (List<Map<String, Object>>) candidates.get(0)[1];
            
            Map<String, Object> meta = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!entry.getKey().equals(streamKey)) {
                    meta.put(entry.getKey(), entry.getValue());
                }
            }
            
            return new Object[]{stream, meta, streamKey};
        }
        
        return new Object[]{null, data, null};
    }
    
    private List<String> writeMetadata(Map<String, Object> metadata) {
        List<String> lines = new ArrayList<>();
        
        // Flatten top-level objects (depth 1)
        Map<String, Object> flattened = flatten(metadata, "", ".", 1, 0);
        
        List<String> sortedKeys = new ArrayList<>(flattened.keySet());
        Collections.sort(sortedKeys);
        
        for (String key : sortedKeys) {
            Object val = flattened.get(key);
            String valStr = formatValue(val);
            
            // Colon-less syntax for objects/arrays
            if (valStr.startsWith("{") || valStr.startsWith("[")) {
                lines.add(key + valStr);
            } else {
                lines.add(key + Constants.META_SEPARATOR + valStr);
            }
        }
        
        return lines;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> writeTable(List<?> stream, String key) {
        if (stream == null || stream.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> lines = new ArrayList<>();
        
        // Flatten all rows
        List<Map<String, Object>> flatStream = new ArrayList<>();
        for (Object row : stream) {
            if (row instanceof Map) {
                flatStream.add(flatten((Map<String, Object>) row, "", ".", 0, 0));
            }
        }
        
        // Get all column names
        Set<String> allKeysSet = new LinkedHashSet<>();
        for (Map<String, Object> row : flatStream) {
            allKeysSet.addAll(row.keySet());
        }
        List<String> cols = new ArrayList<>(allKeysSet);
        Collections.sort(cols);
        
        // Build header
        StringBuilder header = new StringBuilder();
        if (key != null && !key.equals("data")) {
            header.append(key).append(Constants.META_SEPARATOR);
        }
        header.append(Constants.TABLE_MARKER).append("(").append(stream.size()).append(")");
        header.append(Constants.META_SEPARATOR).append(String.join(",", cols));
        lines.add(header.toString());
        
        // Write rows
        for (Map<String, Object> row : flatStream) {
            List<String> tokens = new ArrayList<>();
            for (String col : cols) {
                Object val = row.get(col);
                if (val == null) {
                    tokens.add("null");
                } else {
                    tokens.add(formatValue(val));
                }
            }
            lines.add(String.join(",", tokens));
        }
        
        return lines;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Map<String, Object> d, String parent, String sep, int maxDepth, int currentDepth) {
        checkCircular(d);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            String newKey = parent.isEmpty() ? k : parent + sep + k;
            
            if (v instanceof Map && currentDepth < maxDepth) {
                Map<String, Object> nested = flatten((Map<String, Object>) v, newKey, sep, maxDepth, currentDepth + 1);
                result.putAll(nested);
            } else {
                result.put(newKey, v);
            }
        }
        
        return result;
    }
    
    private void checkCircular(Object obj) {
        if (obj != null && (obj instanceof Map || obj instanceof List)) {
            if (visited.contains(System.identityHashCode(obj))) {
                throw new IllegalArgumentException("Circular reference detected");
            }
            visited.add(System.identityHashCode(obj));
        }
    }
    
    private String formatValue(Object val) {
        if (val == null) {
            return "null";
        }
        
        if (val instanceof Boolean) {
            return (Boolean) val ? "T" : "F";
        }
        
        if (val instanceof Number) {
            Number num = (Number) val;
            
            // Handle special values
            if (val instanceof Double) {
                double d = (Double) val;
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return "null";
                }
            }
            if (val instanceof Float) {
                float f = (Float) val;
                if (Float.isNaN(f) || Float.isInfinite(f)) {
                    return "null";
                }
            }
            
            // Check if integer
            if (val instanceof Integer || val instanceof Long || val instanceof Short || val instanceof Byte) {
                return String.valueOf(num.longValue());
            }
            
            // Handle floats - avoid scientific notation
            double d = num.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            
            String s = String.valueOf(d);
            // Avoid scientific notation
            if (s.contains("E") || s.contains("e")) {
                // Convert to fixed-point
                s = String.format("%.15f", d).replaceAll("0+$", "").replaceAll("\\.$", ".0");
            }
            // Ensure decimal point for floats (this only runs for true floats, as integers return earlier)
            if (!s.contains(".")) {
                s += ".0";
            }
            return s;
        }
        
        if (val instanceof Map || val instanceof List) {
            return formatZonNode(val);
        }
        
        // String formatting
        String s = String.valueOf(val);
        
        // Check for newlines - must escape and quote
        if (s.contains("\n") || s.contains("\r")) {
            // Use JSON-style escaping inside CSV quotes
            StringBuilder sb = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\\': sb.append("\\\\"); break;
                    case '"': sb.append("\"\""); break;  // CSV-style quote doubling
                    default:
                        if (c < 32) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            sb.append("\"");
            return sb.toString();
        }
        
        // ISO Date detection
        if (isISODate(s)) {
            return s;
        }
        
        // Check if needs type protection (looks like number, boolean, null, etc.)
        if (needsTypeProtection(s)) {
            // Just use simple CSV quoting
            return csvQuote(s);
        }
        
        // Check if needs CSV quoting (contains special chars)
        if (needsQuotes(s)) {
            return csvQuote(s);
        }
        
        return s;
    }
    
    @SuppressWarnings("unchecked")
    private String formatZonNode(Object val) {
        checkCircular(val);
        
        if (val == null) {
            return "null";
        }
        
        if (val instanceof Boolean) {
            return (Boolean) val ? "T" : "F";
        }
        
        if (val instanceof Number) {
            return formatValue(val);
        }
        
        if (val instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) val;
            if (map.isEmpty()) {
                return "{}";
            }
            
            List<String> items = new ArrayList<>();
            List<String> sortedKeys = new ArrayList<>(map.keySet());
            Collections.sort(sortedKeys);
            
            for (String k : sortedKeys) {
                Object v = map.get(k);
                String kStr = k;
                if (Pattern.compile("[,:{}\\[\\]\"]").matcher(k).find()) {
                    kStr = jsonEscape(k);
                }
                
                String vStr = formatZonNode(v);
                
                // Colon-less syntax for nested objects/arrays
                if (vStr.startsWith("{") || vStr.startsWith("[")) {
                    items.add(kStr + vStr);
                } else {
                    items.add(kStr + ":" + vStr);
                }
            }
            
            return "{" + String.join(",", items) + "}";
        }
        
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            if (list.isEmpty()) {
                return "[]";
            }
            
            List<String> items = new ArrayList<>();
            for (Object item : list) {
                items.add(formatZonNode(item));
            }
            return "[" + String.join(",", items) + "]";
        }
        
        // String
        String s = String.valueOf(val);
        
        if (s.contains("\n") || s.contains("\r")) {
            return jsonEscape(s);
        }
        
        if (isISODate(s)) {
            return s;
        }
        
        if (needsTypeProtection(s)) {
            return jsonEscape(s);
        }
        
        if (s.isEmpty() || !s.trim().equals(s)) {
            return jsonEscape(s);
        }
        
        if (Pattern.compile("[,{}\\[\\]\"]").matcher(s).find()) {
            return jsonEscape(s);
        }
        
        return s;
    }
    
    private boolean isISODate(String s) {
        return ISO_DATE_FULL.matcher(s).matches() ||
               ISO_DATE_ONLY.matcher(s).matches() ||
               TIME_ONLY.matcher(s).matches();
    }
    
    private boolean needsTypeProtection(String s) {
        String lower = s.toLowerCase();
        
        // Reserved words
        if (Arrays.asList("t", "f", "true", "false", "null", "none", "nil").contains(lower)) {
            return true;
        }
        
        // Gas/Liquid tokens
        if (s.equals(Constants.GAS_TOKEN) || s.equals(Constants.LIQUID_TOKEN)) {
            return true;
        }
        
        // Leading/trailing whitespace
        if (!s.trim().equals(s)) {
            return true;
        }
        
        // Control characters
        if (CONTROL_CHARS.matcher(s).find()) {
            return true;
        }
        
        // Pure numbers
        if (PURE_INTEGER.matcher(s).matches() || 
            PURE_DECIMAL.matcher(s).matches() ||
            SCIENTIFIC_NOTATION.matcher(s).matches()) {
            return true;
        }
        
        return false;
    }
    
    private boolean needsQuotes(String s) {
        if (s.isEmpty()) {
            return true;
        }
        
        if (Arrays.asList("T", "F", "null", Constants.GAS_TOKEN, Constants.LIQUID_TOKEN).contains(s)) {
            return true;
        }
        
        if (PURE_INTEGER.matcher(s).matches()) {
            return true;
        }
        
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        if (!s.trim().equals(s)) {
            return true;
        }
        
        if (Pattern.compile("[,\\n\\r\\t\"\\[\\]|;]").matcher(s).find()) {
            return true;
        }
        
        return false;
    }
    
    private String csvQuote(String s) {
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
    
    private String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
