package com.compressor.algorithm;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LZW_Compressor extends CompressionAlgorithm {

    public LZW_Compressor(){
        this.name = "LZW";
    }

    @Override
    public byte[] compress(byte[] data){

        if(data == null || data.length == 0){
            return new byte[0];
        }

        HashMap<String,Integer> dictionary = new HashMap<>();

        //Filling Dictionary with 256 ASCII characters
        for(int i=0; i < 256 ; i++ ){
            dictionary.put(String.valueOf((char) i),i);
        }

        int dictSize = 256;
        final int MAX_DICT_SIZE = 4096;
        String current = String.valueOf((char) (data[0] & 0xFF));
        List<Integer> Compressed_Data_List = new ArrayList<>();

        for(int i=1 ; i < data.length ; i++){
            String next = String.valueOf((char)( data[i] & 0xFF));
            String combined = current + next;

            if(dictionary.containsKey(combined)){
                current = combined;
            }
            else{
                Compressed_Data_List.add(dictionary.get(current));
                dictionary.put(combined,dictSize++);
                current = next;
            }

            if(dictSize > MAX_DICT_SIZE){

                dictionary.clear();
                for(int j=0; j < 256; j++){
                    dictionary.put(String.valueOf((char) j), j);
                }
                dictSize = 256;

                current = String.valueOf((char)(data[i] & 0xFF));
            }
        }

        Compressed_Data_List.add(dictionary.get(current));

        ByteArrayOutputStream CompressedOutput = new ByteArrayOutputStream();

        for(int code: Compressed_Data_List){
            CompressedOutput.write((code >> 8) & 0xFF);
            CompressedOutput.write(code & 0xFF);
        }

        return CompressedOutput.toByteArray();
    };

    public byte[] decompress(byte[] data){

        if(data == null || data.length == 0){
            return new byte[0];
        }

        HashMap<Integer,String> dictionary = new HashMap<>();

        for(int i=0 ; i<256 ; i++){
            dictionary.put(i, String.valueOf((char) i));
        }

        int dictSize = 256;

        List<Integer> ByteToInt_List = new ArrayList<>();

        for(int i=0; i< data.length -1 ; i+=2){
            int code = ((data[i] & 0xFF) << 8) | (data[i+1] & 0xFF);
            ByteToInt_List.add(code);
        }

        StringBuilder Decompressed_String_output = new StringBuilder();

        String current = dictionary.get(ByteToInt_List.get(0));

        Decompressed_String_output.append(current);

        for(int i =1 ; i< ByteToInt_List.size() ; i++){
            int nextCode = ByteToInt_List.get(i);
            String next;

            if(dictionary.containsKey(nextCode)){
                next = dictionary.get(nextCode);
            }
            else{
                next = current + current.charAt(0);
            }

            Decompressed_String_output.append(next);

            dictionary.put(dictSize++, current + next.charAt(0));
            current = next;
        }
        byte[] Decompressed_output = new byte[Decompressed_String_output.length()];

        for(int i=0; i< Decompressed_String_output.length(); i++){
            Decompressed_output[i] = (byte) Decompressed_String_output.charAt(i);
        }

        return Decompressed_output;
    };
}
