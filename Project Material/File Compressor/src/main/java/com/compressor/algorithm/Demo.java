package com.compressor.algorithm;

public class Demo {
    public static void main(String[] args) {

        RLECompressor rle = new RLECompressor();

        // Test 1 — simple text
        String original = "AAAABBBBCCCC";
        byte[] originalBytes = original.getBytes();

        byte[] compressed = rle.compress(originalBytes);
        byte[] decompressed = rle.decompress(compressed);

        String result = new String(decompressed);

        System.out.println("Original  : " + original);
        System.out.println("Result    : " + result);
        System.out.println("Match     : " + original.equals(result));
        System.out.println("Original size  : " + originalBytes.length);
        System.out.println("Compressed size: " + compressed.length);
    }
}
