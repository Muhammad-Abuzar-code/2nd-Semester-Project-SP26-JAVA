package com.compressor.core;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.compressor.algorithm.CompressionAlgorithm;

import javax.print.attribute.standard.Compression;

public class CompressionPipeline {
    private String pipelineName;
    private List<CompressionAlgorithm> stages;

    public CompressionPipeline(){
        this.pipelineName = pipelineName;
        this.stages = new ArrayList<>();
    }
    public CompressionPipeline(String pipeline){
        this.pipelineName = pipeline;
        this.stages = new ArrayList<>();
    }

    public void addStage(CompressionAlgorithm algorithm){
        stages.add(algorithm);
    }

    public byte[] compress(byte[] data){
        byte[] current = data;
        for(CompressionAlgorithm alg : stages){
            current = alg.compress(current);
        }
        return current;
    }

    public byte[] decompress(byte[] data){
        byte[] current = data;
        for(int i = stages.size() - 1 ; i>=0 ; i++){
            current = stages.get(i).decompress(current);
        }
        return current;
    }

    public String getPipelineName(){
        return pipelineName;
    }

    public Boolean verifyIntegrity(byte[] original, byte[]decompressed ){
        return Arrays.equals(original,decompressed);
    }
}
