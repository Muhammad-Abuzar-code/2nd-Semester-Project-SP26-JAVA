package com.compressor.algorithm;

import java.io.*;
import java.util.*;

public class Huffman_Compressor extends CompressionAlgorithm {

    // Inner class — Huffman Tree Node
    private static class HuffmanNode implements Comparable<HuffmanNode> {

        byte value;           // the actual byte this node represents
        int frequency;
        HuffmanNode left;
        HuffmanNode right;
        boolean isLeaf;

        // Constructor for leaf node (actual byte)
        HuffmanNode(byte value, int frequency) {
            this.value = value;
            this.frequency = frequency;
            this.isLeaf = true;
        }

        // Constructor for internal node (just combines two nodes)
        HuffmanNode(HuffmanNode left, HuffmanNode right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
            this.isLeaf = false;
        }

        // Priority queue needs this to sort by frequency
        @Override
        public int compareTo(HuffmanNode other) {
            return this.frequency - other.frequency;
        }
    }

    public Huffman_Compressor() {
        this.name = "Huffman";
    }

    @Override
    public byte[] compress(byte[] data) {

        if (data == null || data.length == 0) {
            return new byte[0];
        }

        // Counting the frequencies
        int[] frequency = new int[256];
        for (byte b : data) {
            frequency[b & 0xFF]++;
        }

        // Building the priority queue
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (frequency[i] > 0) {
                queue.add(new HuffmanNode((byte) i, frequency[i]));
            }
        }

        // Handle single unique byte case by adding a dummy node
        if (queue.size() == 1) {
            HuffmanNode only = queue.poll();
            HuffmanNode dummy = new HuffmanNode((byte)0, 0);
            queue.add(new HuffmanNode(dummy, only));
        }

        // Building the  tree
        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();   // smallest available in the list
            HuffmanNode right = queue.poll();  // second smallest available in the list
            queue.add(new HuffmanNode(left, right));
        }

        HuffmanNode root = queue.poll();

        // Generating codes for thr built tree
        HashMap<Byte, String> codes = new HashMap<>();
        generateCodes(root, "", codes);

        // Encoding data to bit string
        StringBuilder bitString = new StringBuilder();
        for (byte b : data) {
            bitString.append(codes.get(b));
        }

        // Pack bits into bytes and adding Padding so these can be easily packed into 1 byte (1byte = 8bits)
        // padding will be added at the very end after whole converion.
        int padding = 8 - (bitString.length() % 8);
        if (padding == 8) padding = 0;
        for (int i = 0; i < padding; i++) {
            bitString.append('0');
        }

        byte[] compressedData = new byte[bitString.length() / 8];
        for (int i = 0; i < compressedData.length; i++) {
            String byteStr = bitString.substring(i * 8, (i + 1) * 8);
            compressedData[i] = (byte) Integer.parseInt(byteStr, 2);
        }

        // Serialize tree and combine with data
        byte[] treeBytes = serializeTree(root);

        //  Writing tree, padding info and compressed bytes step by step
        ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(finalOutput);

        try {
            dos.writeInt(treeBytes.length);    // tree size
            dos.write(treeBytes);              // tree
            dos.write(padding);                // padding bits count
            dos.write(compressedData);         // compressed data
        } catch (IOException e) {
            e.printStackTrace();
        }

        return finalOutput.toByteArray();
    }

    // Method to generate codes by traversing through the tree in preorder traversal, It uses Recursion.
    private void generateCodes(HuffmanNode node, String code, HashMap<Byte, String> codes) {
        if (node.isLeaf) {
            codes.put(node.value, code.isEmpty() ? "0" : code);
            return;
        }
        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
    }

    // Saving tree as bytes because memory can not save tree as it is, so it needs to be converted to linear form.
    private byte[] serializeTree(HuffmanNode node) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializeHelper(node, output);
        return output.toByteArray();
    }

    private void serializeHelper(HuffmanNode node, ByteArrayOutputStream out) {
        if (node.isLeaf) {
            out.write(1);           // 1 means leaf
            out.write(node.value);  // the byte value
        } else {
            out.write(0);           // 0 means internal node
            serializeHelper(node.left, out);
            serializeHelper(node.right, out);
        }
    }

    @Override
    public byte[] decompress(byte[] data) {

        if (data == null || data.length == 0) {
            return new byte[0];
        }

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        try {
            // Read the tree
            int treeSize = dis.readInt();
            byte[] treeBytes = new byte[treeSize];
            dis.read(treeBytes);

            // Rebuilding the tree
            int[] index = {0};
            HuffmanNode root = deserializeTree(treeBytes, index);

            // Reading the padding
            int padding = dis.read();

            // Reading the compressed data
            byte[] compressedData = dis.readAllBytes();

            // Convert bytes back to bit string
            StringBuilder bitString = new StringBuilder();
            for (byte b : compressedData) {
                String bits = String.format("%8s",
                        Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                bitString.append(bits);
            }

            // Remove padding bits from end
            String bits = bitString.substring(0,
                    bitString.length() - padding);

            // Decode using tree
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            HuffmanNode current = root;

            for (char bit : bits.toCharArray()) {
                current = (bit == '0') ? current.left : current.right;
                if (current.isLeaf) {
                    output.write(current.value);
                    current = root;
                }
            }

            return output.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // Rebuild tree from bytes
    private HuffmanNode deserializeTree(byte[] data, int[] index) {
        if (data[index[0]] == 1) {
            index[0]++;
            byte value = data[index[0]++];
            return new HuffmanNode(value, 0);
        } else {
            index[0]++;
            HuffmanNode left = deserializeTree(data, index);
            HuffmanNode right = deserializeTree(data, index);
            return new HuffmanNode(left, right);
        }
    }
}