/*
 * ZON CLI - Command Line Interface
 * 
 * Copyright (c) 2025 ZON-FORMAT (Roni Bhakta)
 * MIT License
 */
package com.zonformat.zon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command Line Interface for ZON format conversion.
 * 
 * <h2>Usage</h2>
 * <pre>
 * java -jar zon-java-cli.jar encode &lt;file.json&gt;
 * java -jar zon-java-cli.jar decode &lt;file.zonf&gt;
 * </pre>
 * 
 * <h2>Examples</h2>
 * <pre>
 * # Convert JSON to ZON
 * java -jar zon-java-cli.jar encode data.json &gt; data.zonf
 * 
 * # Convert ZON back to JSON
 * java -jar zon-java-cli.jar decode data.zonf &gt; data.json
 * </pre>
 */
public class Cli {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        String inputFile = args[1];
        
        try {
            Path path = Paths.get(inputFile).toAbsolutePath();
            String content = Files.readString(path);
            
            switch (command.toLowerCase()) {
                case "encode":
                    encodeJson(content);
                    break;
                case "decode":
                    decodeZon(content);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void encodeJson(String jsonContent) {
        // Parse JSON to Java object
        Object data = gson.fromJson(jsonContent, Object.class);
        
        // Encode to ZON
        String zon = Zon.encode(data);
        System.out.println(zon);
    }
    
    private static void decodeZon(String zonContent) {
        // Decode ZON to Java object
        Object data = Zon.decode(zonContent);
        
        // Convert to pretty JSON
        String json = gson.toJson(data);
        System.out.println(json);
    }
    
    private static void printUsage() {
        System.err.println("Usage: zon <encode|decode> <file>");
        System.err.println("Example: zon encode data.json > data.zonf");
        System.err.println("         zon decode data.zonf > output.json");
    }
}
