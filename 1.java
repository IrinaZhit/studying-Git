import java.io.*;
import java.util.*;

public class MainApp {
    private static final Map<Byte, String> dictionary = new HashMap<>();

    private static class TreeNode implements Comparable<TreeNode> {
        byte val;
        int freq;
        TreeNode left = null;
        TreeNode right = null;

        TreeNode(byte val, int freq) {
            this.val = val;
            this.freq = freq;
        }

        TreeNode(byte val, int freq, TreeNode left, TreeNode right) {
            this.val = val;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        public int compareTo(TreeNode other) {
            return Integer.compare(this.freq, other.freq);
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    public static void processFile(String mode, String inputFilePath, String outputFilePath) throws IOException {
        if (mode.equals("e")) {
            byte[] input = readBytesFromFile(inputFilePath);
            encode(input, outputFilePath);
        } else if (mode.equals("d")) {
            decode(inputFilePath, outputFilePath);
        } else {
            throw new IllegalArgumentException("Invalid mode. Use 'e' for encoding and 'd' for decoding.");
        }
    }

    private static void encode(byte[] data, String outputPath) throws IOException {
        TreeNode root = buildHuffmanTree(data);
        generateCodes(root, "");
        StringBuilder encodedData = new StringBuilder();
        for (byte b : data) {
            String code = dictionary.get(b);
            if (code == null) throw new IllegalArgumentException("Byte not found in code dictionary.");
            encodedData.append(code);
        }
        writeFile(outputPath, encodedData.toString());
    }

    private static void decode(String inputPath, String outputPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputPath); FileOutputStream fos = new FileOutputStream(outputPath)) {
            loadCodeDictionary(fis);
            String encodedString = readEncodedData(fis);
            byte[] decodedBytes = decodeData(encodedString);
            fos.write(decodedBytes);
        }
    }

    private static TreeNode buildHuffmanTree(byte[] data) {
        Map<Byte, Integer> frequencies = new HashMap<>();
        for (byte b : data) {
            frequencies.put(b, frequencies.getOrDefault(b, 0) + 1);
        }

        PriorityQueue<TreeNode> queue = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
            queue.add(new TreeNode(entry.getKey(), entry.getValue()));
        }

        while (queue.size() > 1) {
            TreeNode left = queue.poll();
            TreeNode right = queue.poll();
            TreeNode merged = new TreeNode((byte) 0, left.freq + right.freq, left, right);
            queue.add(merged);
        }

        return queue.peek();
    }

    private static void generateCodes(TreeNode node, String currentCode) {
        if (node == null) return;

        if (node.isLeaf()) {
            dictionary.put(node.val, currentCode.isEmpty() ? "0" : currentCode);
            return;
        }

        generateCodes(node.left, currentCode + "0");
        generateCodes(node.right, currentCode + "1");
    }

    private static void writeFile(String path, String encodedText) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            writeDictionary(fos);
            writeEncodedText(fos, encodedText);
        }
    }

    private static void writeDictionary(FileOutputStream fos) throws IOException {
        fos.write(dictionary.size());

        for (Map.Entry<Byte, String> entry : dictionary.entrySet()) {
            byte symbol = entry.getKey();
            String code = entry.getValue();

            fos.write(symbol);
            fos.write(code.length());

            int buffer = 0;
            int bitCount = 0;
            for (char c : code.toCharArray()) {
                if (c == '1') buffer |= (1 << (7 - bitCount));
                bitCount++;
                if (bitCount == 8) {
                    fos.write(buffer);
                    buffer = 0;
                    bitCount = 0;
                }
            }
            if (bitCount > 0) fos.write(buffer);
        }
    }

    private static void writeEncodedText(FileOutputStream fos, String encodedText) throws IOException {
        int len = encodedText.length();
        fos.write((len >>> 24) & 0xFF);
        fos.write((len >>> 16) & 0xFF);
        fos.write((len >>> 8) & 0xFF);
        fos.write(len & 0xFF);

        int buffer = 0;
        int bitCount = 0;
        for (char c : encodedText.toCharArray()) {
            if (c == '1') buffer |= (1 << (7 - bitCount));
            bitCount++;
            if (bitCount == 8) {
                fos.write(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }
        if (bitCount > 0) fos.write(buffer);
    }

    private static void loadCodeDictionary(FileInputStream fis) throws IOException {
        int entries = fis.read();
        for (int i = 0; i < entries; i++) {
            byte symbol = (byte) fis.read();
            int length = fis.read();
            StringBuilder code = new StringBuilder();
            int bitsRead = 0;
            int buffer = 0;
            while (bitsRead < length) {
                if (bitsRead % 8 == 0) buffer = fis.read();
                int bitPosition = 7 - (bitsRead % 8);
                char bit = ((buffer & (1 << bitPosition)) != 0) ? '1' : '0';
                code.append(bit);
                bitsRead++;
            }
            dictionary.put(symbol, code.toString());
        }
    }

    private static String readEncodedData(FileInputStream fis) throws IOException {
        byte[] lenBytes = new byte[4];
        int bytesRead = fis.read(lenBytes);
        if (bytesRead != 4) throw new IOException("Failed to read length bytes.");
        int totalBits = ((lenBytes[0] & 0xFF) << 24) | ((lenBytes[1] & 0xFF) << 16) | ((lenBytes[2] & 0xFF) << 8) | (lenBytes[3] & 0xFF);

        StringBuilder sb = new StringBuilder();
        int byteVal, bitsRead = 0;
        while ((byteVal = fis.read()) != -1 && bitsRead < totalBits) {
            for (int bit = 7; bit >= 0 && bitsRead < totalBits; bit--) {
                sb.append(((byteVal >> bit) & 1) == 1 ? '1' : '0');
                bitsRead++;
            }
        }
        return sb.toString();
    }

    private static byte[] decodeData(String encoded) {
        Map<String, Byte> reverseDict = new HashMap<>();
        for (Map.Entry<Byte, String> entry : dictionary.entrySet()) {
            reverseDict.put(entry.getValue(), entry.getKey());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder currentCode = new StringBuilder();
        for (char c : encoded.toCharArray()) {
            currentCode.append(c);
            if (reverseDict.containsKey(currentCode.toString())) {
                baos.write(reverseDict.get(currentCode.toString()));
                currentCode.setLength(0);
            }
        }

        return baos.toByteArray();
    }

    private static byte[] readBytesFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        long length = file.length();
        if (length > Integer.MAX_VALUE) throw new IOException("File is too large.");
        byte[] data = new byte[(int) length];
        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.read(data) != length) throw new IOException("Failed to read all file bytes.");
        }
        return data;
    }

    public static void main(String[] args) {
        if (args.length != 3 || (!args[0].equals("e") && !args[0].equals("d"))) {
            System.out.println("Usage: java MainApp <e|d> <inputFile> <outputFile>");
            return;
        }
        try {
            processFile(args[0], args[1], args[2]);
            System.out.println("Operation completed.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
