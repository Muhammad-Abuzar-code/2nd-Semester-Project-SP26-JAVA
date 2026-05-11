package com.compressor.algorithm;

public class Demo {
    public static void main(String[] args) {

        RLE_Compressor rle = new RLE_Compressor();

        // Test 1 — Testing RLE using Simple String of Letters
        String original = "AAAABBBBCCCC";
        byte[] originalBytes = original.getBytes();

        byte[] compressed = rle.compress(originalBytes);
        byte[] decompressed = rle.decompress(compressed);

        String result = new String(decompressed);

        int RLEcompressedRatio = (int)CompressionAlgorithm.calculateRatio(decompressed,compressed);

        System.out.println("Original  : " + original);
        System.out.println("Decompressed    : " + result);
        System.out.println("Orginal and Decompressed Match: " + original.equals(result));
        System.out.println("Original size  : " + originalBytes.length);
        System.out.println("Compressed size: " + compressed.length);
        System.out.println("File Compressed by RLE %: " + RLEcompressedRatio + "%");

        LZW_Compressor lzw = new LZW_Compressor();
        String Original = "ABABABABAABABABABABAAB";
        byte[] LZWcompressed = lzw.compress(Original.getBytes());
        byte[] LZWdecompressed = lzw.decompress(LZWcompressed);
        String LZWresult = new String(LZWdecompressed);
        int LZWcompressedRatio = (int) CompressionAlgorithm.calculateRatio(LZWdecompressed,LZWcompressed);

        System.out.println();
        System.out.println("Original  : " + Original);
        System.out.println("Decompressed    : " + LZWresult);
        System.out.println("Orginal and Decompressed Match: " + Original.equals(LZWresult));
        System.out.println("Original size  : " + Original.getBytes().length);
        System.out.println("Compressed size: " + LZWcompressed.length);
        System.out.println("File Compressed by LZW  : " + LZWcompressedRatio + "%");
    }
}
