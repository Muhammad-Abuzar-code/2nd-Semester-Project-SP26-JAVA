package com.compressor.core;

import com.compressor.algorithm.Huffman_Compressor;
import com.compressor.algorithm.LZW_Compressor;
import com.compressor.algorithm.RLE_Compressor;
import com.compressor.io.FileHandler;
import java.util.concurrent.atomic.AtomicReference;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class MultiLevelCompressor {

    private FileHandler       fileHandler;
    private RLE_Compressor    rle;
    private LZW_Compressor    lzw;
    private Huffman_Compressor huffman;
    private CompressionStats lastStats;

    public MultiLevelCompressor() {
        fileHandler = new FileHandler();
        rle     = new RLE_Compressor();
        lzw     = new LZW_Compressor();
        huffman = new Huffman_Compressor();
    }

    private static final String ALGO_RLE     = "RLE";
    private static final String ALGO_LZW     = "LZW";
    private static final String ALGO_HUFFMAN = "Huffman";
    private static final String ALGO_LZW_HUFFMAN = "LZW+Huffman";

    public void compressFile(String inputPath, String outputPath) throws IOException {
        byte[] originalData = fileHandler.readFile(inputPath);
        BestResult best = pickBest(originalData);

        CRC32 crc = new CRC32();
        crc.update(originalData);
        int checksum = (int) crc.getValue();

        String     extension = fileHandler.getExtension(inputPath);
        FileHeader header    = new FileHeader(best.name, extension, originalData.length, checksum);

        writeRazFile(outputPath, header, best.data);

        double ratio = (1.0 - ((double) best.data.length / originalData.length)) * 100;

        lastStats = new CompressionStats();
        lastStats.originalSize = originalData.length;
        lastStats.compressedSize = best.data.length;
        lastStats.winnerAlgo = best.name;
        lastStats.ratio = ratio;
        lastStats.fileName = new File(inputPath).getName();

        System.out.printf("[File] %s  →  winner: %s  (%.2f%%)%n",
                new File(inputPath).getName(), best.name, ratio);
    }

    /**
     * Updated to track aggregate statistics for folder compression.
     */
    public void compressFolder(String folderPath, String outputZipPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) throw new IOException("Not a folder: " + folderPath);

        List<File> allFiles = new ArrayList<>();
        collectFiles(folder, allFiles);
        if (allFiles.isEmpty()) throw new IOException("Folder is empty: " + folderPath);

        Path folderRoot = folder.toPath();
        long totalOriginalSize = 0;
        long totalCompressedSize = 0;

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputZipPath)))) {

            for (File file : allFiles) {
                Path   relativePath = folderRoot.relativize(file.toPath()); // gives folder in which file is placed but not the root folder
                String entryName    = relativePath.toString().replace(File.separatorChar, '/') + ".raz";// zip expects / but window expects \

                byte[] razBytes = compressToBytes(file); // stores the compressed file data

                // Track aggregate sizes
                totalOriginalSize += file.length();
                totalCompressedSize += razBytes.length;

                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(razBytes);
                zos.closeEntry();

            }
        }

        // Set stats for the folder
        lastStats = new CompressionStats();
        lastStats.originalSize = totalOriginalSize;
        lastStats.compressedSize = totalCompressedSize;
        lastStats.winnerAlgo = "Multi-Algorithm (Adaptive)";
        lastStats.ratio = (1.0 - ((double) totalCompressedSize / totalOriginalSize)) * 100;
        lastStats.fileName = folder.getName();

    }

    public String decompressFile(String inputPath, String outputFolder) throws IOException {
        byte[]     fileData    = fileHandler.readFile(inputPath); // will store compressed file
        byte[]     restored    = decompressRazBytes(fileData); // will store only decompressed data excluding header
        FileHeader header      = readHeaderFromBytes(fileData); // will store header data

        String baseName   = fileHandler.getFileName(inputPath); // will store compressed file name
        String outputPath = outputFolder + File.separator
                + "restored_" + baseName + header.getOriginalExtension();
        fileHandler.writeFile(outputPath, restored);

        return outputPath;

    }

    public String decompressFolder(String zipPath, String outputFolder) throws IOException {
        File   zipFile = new File(zipPath);
        String zipName = zipFile.getName();
        if      (zipName.endsWith(".raz.zip")) zipName = zipName.substring(0, zipName.length() - 8);
        else if (zipName.endsWith(".zip"))     zipName = zipName.substring(0, zipName.length() - 4);

        String restoredFolderPath = outputFolder + File.separator + "restored_" + zipName;
        File   restoredRoot       = new File(restoredFolderPath);

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipPath)))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }

                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[8192];
                int len;
                while ((len = zis.read(tmp)) != -1) buf.write(tmp, 0, len);
                byte[] razBytes = buf.toByteArray();

                byte[] restored = decompressRazBytes(razBytes);

                String entryName       = entry.getName();
                String originalRelPath = entryName.endsWith(".raz")
                        ? entryName.substring(0, entryName.length() - 4)
                        : entryName;

                File outFile = new File(restoredRoot, originalRelPath);
                outFile.getParentFile().mkdirs();
                Files.write(outFile.toPath(), restored);

                zis.closeEntry();
            }
        }

        return restoredFolderPath;
    }

    private static class BestResult {
        final byte[] data;
        final String name;
        BestResult(byte[] data, String name) {
            this.data = data;
            this.name = name;
        }
    }

    private BestResult pickBest(byte[] original) {
        byte[] rleOut     = rle.compress(original);
        byte[] lzwOut     = lzw.compress(original);
        byte[] huffmanOut = huffman.compress(original);
        byte[] lzwThenHuffmanOut = huffman.compress(lzwOut);

        BestResult best = new BestResult(rleOut, ALGO_RLE);
        if (lzwOut.length < best.data.length) best = new BestResult(lzwOut, ALGO_LZW);
        if (huffmanOut.length < best.data.length) best = new BestResult(huffmanOut, ALGO_HUFFMAN);
        if (lzwThenHuffmanOut.length < best.data.length) best = new BestResult(lzwThenHuffmanOut, ALGO_LZW_HUFFMAN);

        return best;
    }

    private byte[] decompressWithAlgorithm(String name, byte[] data) throws IOException {
        switch (name) {
            case ALGO_RLE: return rle.decompress(data);
            case ALGO_LZW: return lzw.decompress(data);
            case ALGO_HUFFMAN: return huffman.decompress(data);
            case ALGO_LZW_HUFFMAN:
                byte[] afterHuffman = huffman.decompress(data);
                return lzw.decompress(afterHuffman);
            default: throw new IOException("Unknown algorithm in header: " + name);
        }
    }

    private byte[] compressToBytes(File file) throws IOException {
        byte[]     original  = Files.readAllBytes(file.toPath());
        BestResult best      = pickBest(original);

        CRC32 crc = new CRC32();
        crc.update(original);
        int checksum = (int) crc.getValue();

        String     extension = fileHandler.getExtension(file.getName());
        FileHeader header    = new FileHeader(best.name, extension, original.length, checksum);

        byte[]                headerBytes = header.serialize();
        ByteArrayOutputStream out         = new ByteArrayOutputStream();
        DataOutputStream      dos         = new DataOutputStream(out);
        dos.writeInt(headerBytes.length);
        dos.write(headerBytes);
        dos.write(best.data);
        dos.flush();
        return out.toByteArray();
    }

    private byte[] decompressRazBytes(byte[] razBytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(razBytes));
        int    headerSize   = dis.readInt();
        byte[] headerBytes  = new byte[headerSize];
        dis.readFully(headerBytes);

        FileHeader header        = FileHeader.deserialize(headerBytes);
        int        dataStart     = 4 + headerSize;
        byte[]     compressedData = Arrays.copyOfRange(razBytes, dataStart, razBytes.length);
        byte[]     restored      = decompressWithAlgorithm(header.getPipelineName(), compressedData);

        CRC32 crc = new CRC32();
        crc.update(restored);
        if ((int) crc.getValue() != header.getChecksum())
            throw new IOException("Checksum mismatch — file corrupted!");

        return restored;
    }

    private FileHeader readHeaderFromBytes(byte[] razBytes) throws IOException {
        DataInputStream dis        = new DataInputStream(new ByteArrayInputStream(razBytes));
        int             headerSize = dis.readInt();
        byte[]          headerBytes = new byte[headerSize];
        dis.readFully(headerBytes);
        return FileHeader.deserialize(headerBytes);
    }

    private void writeRazFile(String outputPath, FileHeader header, byte[] compressedData)
            throws IOException {
        byte[]                headerBytes = header.serialize();
        ByteArrayOutputStream out         = new ByteArrayOutputStream();
        DataOutputStream      dos         = new DataOutputStream(out);
        dos.writeInt(headerBytes.length);
        dos.write(headerBytes);
        dos.write(compressedData);
        dos.flush();
        fileHandler.writeFile(outputPath, out.toByteArray());
    }

    private void collectFiles(File dir, List<File> result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) collectFiles(child, result);
            else result.add(child);
        }
    }

    public static class CompressionStats {
        public long originalSize;
        public long compressedSize;
        public String winnerAlgo;
        public double ratio;
        public String fileName;
    }

    public CompressionStats getLastStats() {
        return lastStats;
    }
}