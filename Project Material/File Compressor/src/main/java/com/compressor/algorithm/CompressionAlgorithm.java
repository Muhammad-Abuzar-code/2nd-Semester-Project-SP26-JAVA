package com.compressor.algorithm;

public abstract class CompressionAlgorithm {

    // Every algorithm must have a name
    protected String name;

    // Every algorithm MUST implement these two methods
    // compress takes bytes and returns compressed bytes
    public abstract byte[] compress(byte[] data);

    // decompress takes compressed bytes and returns original bytes
    public abstract byte[] decompress(byte[] data);

    // Every algorithm can use this — no need to rewrite
    public String getName() {
        return name;
    }

    // Calculate how much space was saved
    public double calculateRatio(byte[] original, byte[] compressed) {
        return (1.0 - ((double) compressed.length / original.length)) * 100;
    }
}
