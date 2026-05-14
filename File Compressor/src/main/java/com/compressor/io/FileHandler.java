package com.compressor.io;

import java.io.*;
import java.nio.file.*;

public class FileHandler {

    // Read any file as raw bytes
    public byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    // Write bytes to any file
    public void writeFile(String filePath, byte[] data) throws IOException {
        Files.write(Paths.get(filePath), data);
    }

    // Get file size in KB
    public long getFileSizeKB(String filePath) {
        return new File(filePath).length() / 1024;
    }

    // Get file extension (.pdf, .txt, .png)
    public String getExtension(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return (dot >= 0) ? filePath.substring(dot) : "";
    }

    // Get file name without extension
    public String getFileName(String filePath) {
        File f = new File(filePath);
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return (dot >= 0) ? name.substring(0, dot) : name;
    }
}