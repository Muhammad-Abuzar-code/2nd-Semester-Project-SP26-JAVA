package com.compressor.algorithm;

import java.io.ByteArrayOutputStream;

public class RLE_Compressor extends CompressionAlgorithm {
    public RLE_Compressor() {
        this.name = "RLE";
    }

    @Override
    public byte[] compress(byte[] data) {

        // Handle empty file
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        //We use ByteArrayOutputStream instead of byte[] becasue we dont know
        //how much should be size of byte[]

        ByteArrayOutputStream compressed_output = new ByteArrayOutputStream();

        int i = 0;

        while (i < data.length) {

            byte currentByte = data[i];
            int count = 1;

            // This loop will count how many times the byte repeats
            while (i + count < data.length
                    && data[i + count] == currentByte
                    && count < 255) {
                count++;
            }

            // Write the count and then byte after it
            compressed_output.write(count);
            compressed_output.write(currentByte);

            //Moving to next byte. i+=count equals the next byte in data[].
            i += count;
        }

        return compressed_output.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] data) {

        if (data == null || data.length == 0) {
            return new byte[0];
        }

        ByteArrayOutputStream decompressed_output = new ByteArrayOutputStream();

        int i = 0;

        while (i < data.length) {

            // Read number of times value repeated and 0xFF convert the
            //signed byte stored in data[] while compression to unsigned
            int count = data[i] & 0xFF;
            i++;

            // Read the byte value
            byte value = data[i];
            i++;

            // Write that byte count times
            for (int j = 0; j < count; j++) {
                decompressed_output.write(value);
            }
        }

        return decompressed_output.toByteArray();
    }
}
