package com.compressor.algorithm;

public abstract class CompressionAlgorithm {
    protected String name;

    public abstract byte[] compress(byte[] data);


    public abstract byte[] decompress(byte[] data);

}
