package com.compressor.core;

import java.io.*;

public class FileHeader {

    // Magic number identifies .mlc files
    // Prevents decompressing random files
    public static final byte[] MAGIC = {'M', 'L', 'C', '1'};

    private String pipelineName;      // which pipeline was used for compression
    private String originalExtension; // stores original file extension.pdf .txt .png etc
    private long originalSize;        // original file size
    private int checksum;             // CRC32 checksum used to check if file got corrupted in decompression process
    private long timestamp;           // Stores compression time

    public FileHeader(String pipelineName,
                      String originalExtension,
                      long originalSize,
                      int checksum) {
        this.pipelineName = pipelineName;
        this.originalExtension = originalExtension;
        this.originalSize = originalSize;
        this.checksum = checksum;
        this.timestamp = System.currentTimeMillis();
    }

    // Convert header to bytes to save in file
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(MAGIC);                          // 4 bytes magic
        dos.writeUTF(pipelineName);                // pipeline name
        dos.writeUTF(originalExtension);           // original extension
        dos.writeLong(originalSize);               // original size
        dos.writeInt(checksum);                    // checksum
        dos.writeLong(timestamp);                  // timestamp

        return baos.toByteArray();
    }

    // Read header back from bytes
    public static FileHeader deserialize(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(data)
        );

        // Verify magic number
        byte[] magic = new byte[4];
        dis.read(magic);
        for (int i = 0; i < 4; i++) {
            if (magic[i] != MAGIC[i]) {
                throw new IOException("Invalid .mlc file!");
            }
        }

        String pipeline  = dis.readUTF();
        String extension = dis.readUTF();
        long origSize    = dis.readLong();
        int chksum       = dis.readInt();
        long time        = dis.readLong();

        FileHeader header = new FileHeader(
                pipeline, extension, origSize, chksum
        );
        header.timestamp = time;
        return header;
    }

    // Getters
    public String getPipelineName()      { return pipelineName; }
    public String getOriginalExtension() { return originalExtension; }
    public long getOriginalSize()        { return originalSize; }
    public int getChecksum()             { return checksum; }
    public long getTimestamp()           { return timestamp; }
}