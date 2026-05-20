package com.compressor.core;

import java.io.*;

public class FileHeader {

    //Magic number to identify .raz files
    public static final byte[] MAGIC = {'R', 'A', 'Z', '!'};

    private String pipelineName;
    private String originalExtension;
    private long originalSize;
    private int checksum;

    public FileHeader(String pipelineName,
                      String originalExtension,
                      long originalSize,
                      int checksum) {
        this.pipelineName = pipelineName;
        this.originalExtension = originalExtension;
        this.originalSize = originalSize;
        this.checksum = checksum;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(MAGIC);
        dos.writeUTF(pipelineName);
        dos.writeUTF(originalExtension);
        dos.writeLong(originalSize);
        dos.writeInt(checksum);

        return baos.toByteArray();
    }

    public static FileHeader deserialize(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(data)
        );

        byte[] magic = new byte[4];
        dis.read(magic);
        for (int i = 0; i < 4; i++) {
            if (magic[i] != MAGIC[i]) {
                throw new IOException("Invalid .raz file!");
            }
        }

        String pipeline = dis.readUTF();
        String extension = dis.readUTF();
        long origSize = dis.readLong();
        int chksum = dis.readInt();

        FileHeader header = new FileHeader(
                pipeline, extension, origSize, chksum
        );
        return header;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getOriginalExtension() {
        return originalExtension;
    }

    public int getChecksum() {
        return checksum;
    }
}