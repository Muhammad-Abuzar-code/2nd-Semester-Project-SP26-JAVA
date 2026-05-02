package com.compressor.algorithm;

import java.io.ByteArrayOutputStream;

public class RLECompressor extends CompressionAlgorithm {
    public RLECompressor() {
        this.name = "RLE";
    }

    @Override
    public byte[] compress(byte[] data) {

        // Handle empty file
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int i = 0;

        while (i < data.length) {

            byte currentByte = data[i];
            int count = 1;

            // Count how many times this byte repeats
            while (i + count < data.length
                    && data[i + count] == currentByte
                    && count < 255) {
                count++;
            }

            // Write count then the byte value
            output.write(count);
            output.write(currentByte);

            // Move forward by count
            i += count;
        }

        return output.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] data) {

        if (data == null || data.length == 0) {
            return new byte[0];
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int i = 0;

        while (i < data.length) {

            // Read count
            int count = data[i] & 0xFF;
            i++;

            // Read the byte value
            byte value = data[i];
            i++;

            // Write that byte count times
            for (int j = 0; j < count; j++) {
                output.write(value);
            }
        }

        return output.toByteArray();
    }
}
