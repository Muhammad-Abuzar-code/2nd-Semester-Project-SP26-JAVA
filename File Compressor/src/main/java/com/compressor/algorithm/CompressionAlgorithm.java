package com.compressor.algorithm;

public abstract class CompressionAlgorithm {

    // Every algorithm must have a name
    protected String name;

    public abstract byte[] compress(byte[] data);


    public abstract byte[] decompress(byte[] data);

    public String getName() {
        return name;
    }

    public static double calculateRatio(byte[] original, byte[] compressed) {
        return (1.0 - ((double) compressed.length / original.length)) * 100;
    }
}
