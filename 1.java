import java.io.*;
import java.util.*;

class TreeNode {
    char value;
    int frequency;
    TreeNode leftBranch;
    TreeNode rightBranch;

    TreeNode(char value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }

    TreeNode(char value, int frequency, TreeNode leftBranch, TreeNode rightBranch) {
        this.value = value;
        this.frequency = frequency;
        this.leftBranch = leftBranch;
        this.rightBranch = rightBranch;
    }
}

class HuffmanCoding {
    private static final Map<Character, String> codeTable = new HashMap<>();

    public static void compress(String inputText, String outputFilePath) throws IOException {
        TreeNode treeRoot = buildTree(inputText);
        generateCode(treeRoot, "", codeTable);

        StringBuilder encodedText = new StringBuilder();
        for (char character : inputText.toCharArray()) {
            encodedText.append(codeTable.get(character));
        }

        saveCompressedData(outputFilePath, encodedText.toString());
    }

    public static void decompress(String inputFilePath, String outputFilePath) throws IOException {
        try (FileInputStream inputFile = new FileInputStream(inputFilePath);
             FileOutputStream outputFile = new FileOutputStream(outputFilePath)) {

            restoreCodeTable(inputFile);
            String encodedData = readEncodedContent(inputFile);
            String decodedData = decodeData(encodedData);

            outputFile.write(decodedData.getBytes());
        }
    }

    private static void restoreCodeTable(FileInputStream inputStream) throws IOException {
        int mappingsCount = inputStream.read();
        for (int i = 0; i < mappingsCount; i++) {
            char character = (char) inputStream.read();
            int codeLength = inputStream.read();

            StringBuilder codeSequence = new StringBuilder();
            for (int j = 0; j < codeLength; j++) {
                if (j % 8 == 0) inputStream.read();
                codeSequence.append((inputStream.read() & (1 << (7 - (j % 8)))) != 0 ? '1' : '0');
            }

            codeTable.put(character, codeSequence.toString());
        }
    }

    private static String readEncodedContent(FileInputStream inputStream) throws IOException {
        int totalBits = inputStream.read();
        StringBuilder bitStream = new StringBuilder();
        int bytesRead, bitsProcessed = 0;

        while ((bytesRead = inputStream.read()) != -1) {
            for (int bit = 7; bit >= 0 && bitsProcessed < totalBits; bit--) {
                bitStream.append((bytesRead & (1 << bit)) != 0 ? '1' : '0');
                bitsProcessed++;
            }
        }

        return bitStream.toString();
    }

    private static String decodeData(String encodedData) {
        Map<String, Character> reverseTable = new HashMap<>();
        codeTable.forEach(reverseTable::put);

        StringBuilder decodedOutput = new StringBuilder();
        StringBuilder currentCode = new StringBuilder();

        for (char bit : encodedData.toCharArray()) {
            currentCode.append(bit);
            if (reverseTable.containsKey(currentCode.toString())) {
                decodedOutput.append(reverseTable.get(currentCode.toString()));
                currentCode.setLength(0);
            }
        }

        return decodedOutput.toString();
    }

    private static void saveCompressedData(String outputFilePath, String encodedData) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            saveCodeTable(outputStream);
            writeEncodedContent(outputStream, encodedData);
        }
    }

    private static void saveCodeTable(FileOutputStream outputStream) throws IOException {
        outputStream.write(codeTable.size());

        for (Map.Entry<Character, String> entry : codeTable.entrySet()) {
            char character = entry.getKey();
            String code = entry.getValue();

            outputStream.write(character);
            outputStream.write(code.length());

            int buffer = 0, bitCounter = 0;
            for (char bit : code.toCharArray()) {
                buffer = (buffer << 1) | (bit == '1' ? 1 : 0);
                bitCounter++;
                if (bitCounter == 8) {
                    outputStream.write(buffer);
                    buffer = 0;
                    bitCounter = 0;
                }
            }

            if (bitCounter > 0) {
                outputStream.write(buffer << (8 - bitCounter));
            }
        }
    }

    private static void writeEncodedContent(FileOutputStream outputStream, String encodedData) throws IOException {
        outputStream.write(encodedData.length());
        int buffer = 0, bitCount = 0;

        for (char bit : encodedData.toCharArray()) {
            buffer = (buffer << 1) | (bit == '1' ? 1 : 0);
            bitCount++;
            if (bitCount == 8) {
                outputStream.write(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }

        if (bitCount > 0) {
            outputStream.write(buffer << (8 - bitCount));
        }
    }

    private static TreeNode buildTree(String text) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (char character : text.toCharArray()) {
            frequencyMap.put(character, frequencyMap.getOrDefault(character, 0) + 1);
        }

        PriorityQueue<TreeNode> queue = new PriorityQueue<>(Comparator.comparingInt(node -> node.frequency));
        frequencyMap.forEach((character, frequency) -> queue.add(new TreeNode(character, frequency)));

        while (queue.size() > 1) {
            TreeNode left = queue.poll();
            TreeNode right = queue.poll();
            queue.add(new TreeNode('\0', left.frequency + right.frequency, left, right));
        }

        return queue.peek();
    }

    private static void generateCode(TreeNode node, String code, Map<Character, String> map) {
        if (node == null) return;

        if (node.leftBranch == null && node.rightBranch == null) {
            map.put(node.value, code.isEmpty() ? "0" : code);
        }

        generateCode(node.leftBranch, code + "0", map);
        generateCode(node.rightBranch, code + "1", map);
    }

    public static void main(String[] args) {
        if (args.length != 3 || (!args[0].equals("compress") && !args[0].equals("decompress"))) {
            System.out.println("Usage: <compress|decompress> <InputFilePath> <OutputFilePath>");
            return;
        }

        try {
            if ("compress".equals(args[0])) {
                compress(readFile(args[1]), args[2]);
            } else {
                decompress(args[1], args[2]);
            }
        } catch (IOException e) {
            System.err.println("File processing error: " + e.getMessage());
        }
    }

    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }
}
