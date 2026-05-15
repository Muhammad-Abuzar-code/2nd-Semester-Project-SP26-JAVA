package com.compressor.core;

import com.compressor.algorithm.Huffman_Compressor;
import com.compressor.algorithm.LZW_Compressor;
import com.compressor.algorithm.RLE_Compressor;
import com.compressor.io.FileHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class MultiLevelCompressor {

    private FileHandler       fileHandler;
    private RLE_Compressor    rle;
    private LZW_Compressor    lzw;
    private Huffman_Compressor huffman;

    public MultiLevelCompressor() {
        fileHandler = new FileHandler();
        rle     = new RLE_Compressor();
        lzw     = new LZW_Compressor();
        huffman = new Huffman_Compressor();
    }

    // ─────────────────────────────────────────────────────────────
    //  ALGORITHM NAMES — used in header and switch
    // ─────────────────────────────────────────────────────────────

    // Single-algorithm names
    private static final String ALGO_RLE     = "RLE";
    private static final String ALGO_LZW     = "LZW";
    private static final String ALGO_HUFFMAN = "Huffman";

    // Pipeline name — LZW output fed into Huffman
    private static final String ALGO_LZW_HUFFMAN = "LZW+Huffman";

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC ENTRY POINTS
    // ─────────────────────────────────────────────────────────────

    /**
     * Compress a single file.
     * Runs 4 strategies on the original data:
     *   1. RLE alone
     *   2. LZW alone
     *   3. Huffman alone
     *   4. LZW output → Huffman (pipeline)
     * The strategy that produces the smallest output wins.
     * The winner name is stored in the header so decompressFile knows
     * exactly how to reverse it.
     */
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
        System.out.printf("[File] %s  →  winner: %s  (%.2f%%)%n",
                new File(inputPath).getName(), best.name, ratio);
    }

    /**
     * Compress an entire folder recursively.
     * Each file is compressed with the best-of-4 strategy.
     * All .raz entries are packed into a single ZIP at outputZipPath,
     * preserving the original folder structure.
     */
    public void compressFolder(String folderPath, String outputZipPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) throw new IOException("Not a folder: " + folderPath);

        List<File> allFiles = new ArrayList<>();
        collectFiles(folder, allFiles);
        if (allFiles.isEmpty()) throw new IOException("Folder is empty: " + folderPath);

        Path folderRoot = folder.toPath();

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputZipPath)))) {

            for (File file : allFiles) {
                Path   relativePath = folderRoot.relativize(file.toPath());
                String entryName    = relativePath.toString().replace(File.separatorChar, '/') + ".raz";

                byte[] razBytes = compressToBytes(file);
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(razBytes);
                zos.closeEntry();

                System.out.println("[Folder] Packed: " + entryName);
            }
        }
        System.out.println("Folder archive written: " + outputZipPath);
    }

    /**
     * Decompress a single .raz file.
     * Reads the algorithm name from the header and reverses exactly that strategy.
     */
    public void decompressFile(String inputPath, String outputFolder) throws IOException {
        byte[]     fileData    = fileHandler.readFile(inputPath);
        byte[]     restored    = decompressRazBytes(fileData);
        FileHeader header      = readHeaderFromBytes(fileData);

        String baseName   = fileHandler.getFileName(inputPath);
        String outputPath = outputFolder + File.separator
                + "restored_" + baseName + header.getOriginalExtension();
        fileHandler.writeFile(outputPath, restored);
        System.out.println("Decompressed: " + outputPath);
    }

    /**
     * Decompress a folder archive (.raz.zip) produced by compressFolder.
     * Restores the original folder structure under outputFolder.
     * Returns the path to the restored root folder.
     */
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

                System.out.println("[Folder] Restored: " + outFile.getPath());
                zis.closeEntry();
            }
        }

        System.out.println("Folder restored to: " + restoredFolderPath);
        return restoredFolderPath;
    }

    // ─────────────────────────────────────────────────────────────
    //  HEADER PEEK  (UI uses this for filename suggestions)
    // ─────────────────────────────────────────────────────────────

    public String getOriginalExtensionFromHeader(String razFilePath) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(razFilePath))) {
            dis.readInt();
            byte[] magic = new byte[4];
            dis.read(magic);
            dis.readUTF();       // algorithm name
            return dis.readUTF(); // original extension
        } catch (IOException e) {
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CORE: BEST-OF-4 SELECTION
    // ─────────────────────────────────────────────────────────────

    /** Holds the compressed bytes and the name of the winning algorithm. */
    private static class BestResult {
        final byte[] data;
        final String name;
        BestResult(byte[] data, String name) {
            this.data = data;
            this.name = name;
        }
    }

    /**
     * Runs all 4 strategies and returns the one with the smallest output.
     *
     * Strategy 1 — RLE alone         → stored as "RLE"
     * Strategy 2 — LZW alone         → stored as "LZW"
     * Strategy 3 — Huffman alone     → stored as "Huffman"
     * Strategy 4 — LZW then Huffman  → stored as "LZW+Huffman"
     *
     * The name stored in the header tells decompressWithAlgorithm()
     * exactly which steps to reverse and in what order.
     */
    private BestResult pickBest(byte[] original) {

        // Strategy 1: RLE
        byte[] rleOut     = rle.compress(original);

        // Strategy 2: LZW
        byte[] lzwOut     = lzw.compress(original);

        // Strategy 3: Huffman
        byte[] huffmanOut = huffman.compress(original);

        // Strategy 4: LZW → Huffman  (LZW output is the input to Huffman)
        byte[] lzwThenHuffmanOut = huffman.compress(lzwOut);

        System.out.println("RLE         : " + rleOut.length             + " bytes");
        System.out.println("LZW         : " + lzwOut.length             + " bytes");
        System.out.println("Huffman     : " + huffmanOut.length          + " bytes");
        System.out.println("LZW+Huffman : " + lzwThenHuffmanOut.length  + " bytes");

        // Start with RLE as default, then challenge with each other
        BestResult best = new BestResult(rleOut, ALGO_RLE);

        if (lzwOut.length < best.data.length)
            best = new BestResult(lzwOut, ALGO_LZW);

        if (huffmanOut.length < best.data.length)
            best = new BestResult(huffmanOut, ALGO_HUFFMAN);

        if (lzwThenHuffmanOut.length < best.data.length)
            best = new BestResult(lzwThenHuffmanOut, ALGO_LZW_HUFFMAN);

        System.out.println("Winner      : " + best.name);
        return best;
    }

    // ─────────────────────────────────────────────────────────────
    //  DECOMPRESS BY ALGORITHM NAME
    // ─────────────────────────────────────────────────────────────

    /**
     * Reverses the algorithm(s) identified by name.
     * For "LZW+Huffman" the decompression order is the exact reverse
     * of compression: Huffman first, then LZW.
     */
    private byte[] decompressWithAlgorithm(String name, byte[] data) throws IOException {
        switch (name) {
            case ALGO_RLE:
                return rle.decompress(data);

            case ALGO_LZW:
                return lzw.decompress(data);

            case ALGO_HUFFMAN:
                return huffman.decompress(data);

            case ALGO_LZW_HUFFMAN:
                // Compressed as LZW → Huffman, so decompress as Huffman → LZW
                byte[] afterHuffman = huffman.decompress(data);
                return lzw.decompress(afterHuffman);

            default:
                throw new IOException("Unknown algorithm in header: " + name);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Compress a File to a .raz byte array in memory (used when packing folders). */
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

    /** Decompress a raw .raz byte array, verify checksum, return original data. */
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

    /** Read just the FileHeader from a raw .raz byte array. */
    private FileHeader readHeaderFromBytes(byte[] razBytes) throws IOException {
        DataInputStream dis        = new DataInputStream(new ByteArrayInputStream(razBytes));
        int             headerSize = dis.readInt();
        byte[]          headerBytes = new byte[headerSize];
        dis.readFully(headerBytes);
        return FileHeader.deserialize(headerBytes);
    }

    /** Write a complete .raz file to disk. */
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

    /** Recursively collect all files (not directories) under a root folder. */
    private void collectFiles(File dir, List<File> result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) collectFiles(child, result);
            else result.add(child);
        }
    }
}