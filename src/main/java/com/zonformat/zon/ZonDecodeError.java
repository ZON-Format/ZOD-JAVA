/*
 * ZON Exceptions
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

/**
 * Exception thrown when ZON decoding fails.
 */
public class ZonDecodeError extends RuntimeException {
    
    private final String code;
    private final Integer line;
    private final Integer column;
    private final String context;
    
    /**
     * Creates a new ZonDecodeError with a message.
     * 
     * @param message Error message
     */
    public ZonDecodeError(String message) {
        this(message, null, null, null, null);
    }
    
    /**
     * Creates a new ZonDecodeError with a message and error code.
     * 
     * @param message Error message
     * @param code Error code (e.g., "E001", "E002")
     */
    public ZonDecodeError(String message, String code) {
        this(message, code, null, null, null);
    }
    
    /**
     * Creates a new ZonDecodeError with full details.
     * 
     * @param message Error message
     * @param code Error code
     * @param line Line number where error occurred
     * @param column Column position
     * @param context Relevant context snippet
     */
    public ZonDecodeError(String message, String code, Integer line, Integer column, String context) {
        super(message);
        this.code = code;
        this.line = line;
        this.column = column;
        this.context = context;
    }
    
    /**
     * Gets the error code.
     * 
     * @return Error code or null
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the line number where the error occurred.
     * 
     * @return Line number or null
     */
    public Integer getLine() {
        return line;
    }
    
    /**
     * Gets the column position.
     * 
     * @return Column position or null
     */
    public Integer getColumn() {
        return column;
    }
    
    /**
     * Gets the context snippet.
     * 
     * @return Context or null
     */
    public String getContext() {
        return context;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ZonDecodeError");
        if (code != null) {
            sb.append(" [").append(code).append("]");
        }
        sb.append(": ").append(getMessage());
        if (line != null) {
            sb.append(" (line ").append(line).append(")");
        }
        if (context != null) {
            sb.append("\n  Context: ").append(context);
        }
        return sb.toString();
    }
}
