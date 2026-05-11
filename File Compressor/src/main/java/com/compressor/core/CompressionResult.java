package com.compressor.core;

public class CompressionResult {
    private String pipelineName;
    private long originalSize;
    private long compressedSize;
    private long compressionTimeMs;
    private long decompressionTimeMs;
    private double compressionRatio;
    private boolean integrityPassed;

    public CompressionResult(String pipelineName,long originalSize, long compressedSize
                            ,long compressionTimeMs, long decompressionTimeMs, boolean integrityPassed){

        this.pipelineName = pipelineName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.compressionTimeMs = compressionTimeMs;
        this.decompressionTimeMs = decompressionTimeMs;
        this.integrityPassed = integrityPassed;
        this.compressionRatio =  (1.0 - ((double) compressedSize / originalSize)) * 100;
    }

    public String getPipelineName()     { return pipelineName; }
    public long getOriginalSize()       { return originalSize; }
    public long getCompressedSize()     { return compressedSize; }
    public long getCompressionTimeMs()  { return compressionTimeMs; }
    public double getCompressionRatio() { return compressionRatio; }
    public boolean isIntegrityPassed()  { return integrityPassed; }
    public long getSavedBytes()         { return originalSize - compressedSize; }

}
