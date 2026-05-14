package com.compressor.core;

import com.compressor.algorithm.Huffman_Compressor;
import com.compressor.algorithm.LZW_Compressor;
import com.compressor.algorithm.RLE_Compressor;
import com.compressor.algorithm.*;
import com.compressor.io.FileHandler;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

public class MultiLevelCompressor {

    private FileHandler fileHandler;
    private List<CompressionPipeline> allPipelines;

    public MultiLevelCompressor() {
        fileHandler = new FileHandler();
        allPipelines = buildAllPipelines();
    }

    // Build all 6 pipeline combinations
    private List<CompressionPipeline> buildAllPipelines() {
        List<CompressionPipeline> pipelines = new ArrayList<>();

        // Pipeline 1 — RLE only
        CompressionPipeline p1 = new CompressionPipeline("RLE");
        p1.addStage(new RLE_Compressor());
        pipelines.add(p1);

        // Pipeline 2 — LZW only
        CompressionPipeline p2 = new CompressionPipeline("LZW");
        p2.addStage(new LZW_Compressor());
        pipelines.add(p2);

        // Pipeline 3 — Huffman only
        CompressionPipeline p3 = new CompressionPipeline("Huffman");
        p3.addStage(new Huffman_Compressor());
        pipelines.add(p3);

        // Pipeline 4 — RLE then Huffman
        CompressionPipeline p4 = new CompressionPipeline("RLE→Huffman");
        p4.addStage(new RLE_Compressor());
        p4.addStage(new Huffman_Compressor());
        pipelines.add(p4);

        // Pipeline 5 — LZW then Huffman
        CompressionPipeline p5 = new CompressionPipeline("LZW→Huffman");
        p5.addStage(new LZW_Compressor());
        p5.addStage(new Huffman_Compressor());
        pipelines.add(p5);

        // Pipeline 6 — All three
        CompressionPipeline p6 = new CompressionPipeline("RLE→LZW→Huffman");
        p6.addStage(new RLE_Compressor());
        p6.addStage(new LZW_Compressor());
        p6.addStage(new Huffman_Compressor());
        pipelines.add(p6);

        return pipelines;
    }

    // Compress a file with selected pipeline
    public void compressFile(String inputPath,
                             String outputPath,
                             String pipelineName) throws IOException {

        byte[] originalData = fileHandler.readFile(inputPath);

        // Find the selected pipeline
        CompressionPipeline pipeline = getPipeline(pipelineName);

        // Compress
        long startTime = System.currentTimeMillis();
        byte[] compressedData = pipeline.compress(originalData);
        long timeTaken = System.currentTimeMillis() - startTime;

        // Calculate checksum of original
        CRC32 crc = new CRC32();
        crc.update(originalData);
        int checksum = (int) crc.getValue();

        // Build header
        String extension = fileHandler.getExtension(inputPath);
        FileHeader header = new FileHeader(
                pipelineName, extension, originalData.length, checksum
        );

        // Save: header + compressed data together
        byte[] headerBytes = header.serialize();
        ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(finalOutput);
        dos.writeInt(headerBytes.length);  // header size
        dos.write(headerBytes);            // header
        dos.write(compressedData);         // compressed data

        fileHandler.writeFile(outputPath, finalOutput.toByteArray());

        System.out.println("Compressed in " + timeTaken + "ms");
        System.out.println("Ratio: " +
                pipeline.verifyIntegrity(originalData, pipeline.decompress(compressedData)));
    }

    // Decompress a .mlc file
    public void decompressFile(String inputPath,
                               String outputFolder) throws IOException {

        byte[] fileData = fileHandler.readFile(inputPath);
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(fileData)
        );

        // Read header
        int headerSize = dis.readInt();
        byte[] headerBytes = new byte[headerSize];
        dis.read(headerBytes);
        FileHeader header = FileHeader.deserialize(headerBytes);

        // Read compressed data
        byte[] compressedData = dis.readAllBytes();

        // Find correct pipeline
        CompressionPipeline pipeline =
                getPipeline(header.getPipelineName());

        // Decompress
        byte[] restoredData = pipeline.decompress(compressedData);

        // Verify checksum
        CRC32 crc = new CRC32();
        crc.update(restoredData);
        int checksum = (int) crc.getValue();

        if (checksum != header.getChecksum()) {
            throw new IOException("File corrupted! Checksum mismatch.");
        }

        // Save restored file
        String outputPath = outputFolder +
                "/restored" + header.getOriginalExtension();
        fileHandler.writeFile(outputPath, restoredData);

        System.out.println("Decompressed successfully: " + outputPath);
        System.out.println("Integrity: PASSED ✅");
    }

    // Benchmark all 6 pipelines on same file
    public List<CompressionResult> benchmarkAll(String filePath) throws IOException {

        byte[] originalData = fileHandler.readFile(filePath);
        List<CompressionResult> results = new ArrayList<>();

        for (CompressionPipeline pipeline : allPipelines) {

            long startCompress = System.currentTimeMillis();
            byte[] compressed = pipeline.compress(originalData);
            long compressTime = System.currentTimeMillis() - startCompress;

            long startDecompress = System.currentTimeMillis();
            byte[] decompressed = pipeline.decompress(compressed);
            long decompressTime = System.currentTimeMillis() - startDecompress;

            boolean integrity = pipeline.verifyIntegrity(originalData, decompressed);

            results.add(new CompressionResult(
                    pipeline.getPipelineName(),
                    originalData.length,
                    compressed.length,
                    compressTime,
                    decompressTime,
                    integrity
            ));
        }

        return results;
    }

    private CompressionPipeline getPipeline(String name) {

        for (CompressionPipeline pipeline : allPipelines) {

            if (pipeline.getPipelineName().equals(name)) {
                return pipeline;
            }
        }

        // if no pipeline matched, return default Huffman pipeline
        return allPipelines.get(2);
    }

    public List<CompressionPipeline> getAllPipelines() {
        return allPipelines;
    }
}