import java.io.*;
import java.util.*;

/**
 * Представление узла с частотой и символом для алгоритма Хаффмана.
 */
class TreeNode implements Comparable<TreeNode> {
    byte character;
    int frequency;
    TreeNode left, right;

    public TreeNode(byte character, int frequency) {
        this.character = character;
        this.frequency = frequency;
    }

    public TreeNode(int frequency, TreeNode left, TreeNode right) {
        this.character = 0;  // У узла без символа
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(TreeNode other) {
        return Integer.compare(this.frequency, other.frequency);  // Сравниваем по частоте
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }
}

/**
 * Основной класс для работы с алгоритмом Хаффмана, который поддерживает
 * как сжатие, так и разжатие.
 */
public class HuffmanProcessor {

    private static Map<Byte, String> encodingTable = new HashMap<>();
    
    // Кодирование данных из массива байтов и запись в файл
    public static void compress(byte[] data, String outputFile) throws IOException {
        TreeNode root = createHuffmanTree(data);
        buildEncodingTable(root, "");  // Создаем таблицу кодов для каждого символа

        StringBuilder encodedData = new StringBuilder();
        for (byte b : data) {
            encodedData.append(encodingTable.get(b));  // Получаем код символа
        }

        saveToFile(outputFile, encodedData.toString());  // Записываем результат в файл
    }

    // Декодирование данных из файла и запись в другой файл
    public static void decompress(String inputFile, String outputFile) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            loadEncodingTable(inputStream);  // Загружаем таблицу кодов
            String encodedText = readEncodedText(inputStream);  // Читаем закодированные данные

            byte[] decodedBytes = decode(encodedText);  // Декодируем данные
            outputStream.write(decodedBytes);  // Записываем результат в файл
        }
    }

    // Построение дерева Хаффмана из входных данных
    private static TreeNode createHuffmanTree(byte[] data) {
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);  // Подсчет частот
        }

        PriorityQueue<TreeNode> priorityQueue = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            priorityQueue.add(new TreeNode(entry.getKey(), entry.getValue()));  // Заполнение очереди
        }

        while (priorityQueue.size() > 1) {
            TreeNode left = priorityQueue.poll();
            TreeNode right = priorityQueue.poll();
            TreeNode mergedNode = new TreeNode(left.frequency + right.frequency, left, right);
            priorityQueue.add(mergedNode);  // Объединяем два узла в новый
        }

        return priorityQueue.peek();  // Корень дерева
    }

    // Рекурсивное построение таблицы кодов
    private static void buildEncodingTable(TreeNode node, String code) {
        if (node == null) return;

        if (node.isLeaf()) {
            encodingTable.put(node.character, code);  // Присваиваем код символу
        }

        buildEncodingTable(node.left, code + "0");  // Левый узел - 0
        buildEncodingTable(node.right, code + "1");  // Правый узел - 1
    }

    // Запись закодированных данных в файл
    private static void saveToFile(String filePath, String encodedData) throws IOException {
        try (FileOutputStream fileOutput = new FileOutputStream(filePath)) {
            saveEncodingTable(fileOutput);  // Записываем таблицу кодов
            saveEncodedText(fileOutput, encodedData);  // Записываем закодированные данные
        }
    }

    // Запись таблицы кодов в файл
    private static void saveEncodingTable(FileOutputStream fileOutput) throws IOException {
        fileOutput.write(encodingTable.size());  // Записываем количество записей в таблице

        for (Map.Entry<Byte, String> entry : encodingTable.entrySet()) {
            byte character = entry.getKey();
            String code = entry.getValue();

            fileOutput.write(character);  // Записываем символ
            fileOutput.write(code.length());  // Записываем длину кода

            int byteBuffer = 0;
            int bitCount = 0;

            for (char c : code.toCharArray()) {
                if (c == '1') {
                    byteBuffer |= (1 << (7 - bitCount));  // Добавляем бит
                }
                bitCount++;
                if (bitCount == 8) {  // Если собрали 8 бит, записываем
                    fileOutput.write(byteBuffer);
                    byteBuffer = 0;
                    bitCount = 0;
                }
            }

            if (bitCount > 0) {
                fileOutput.write(byteBuffer);  // Записываем остаточные биты
            }
        }
    }

    // Запись закодированных данных в файл
    private static void saveEncodedText(FileOutputStream fileOutput, String encodedData) throws IOException {
        int length = encodedData.length();
        fileOutput.write((length >>> 24) & 0xFF);
        fileOutput.write((length >>> 16) & 0xFF);
        fileOutput.write((length >>> 8) & 0xFF);
        fileOutput.write(length & 0xFF);

        int byteBuffer = 0;
        int bitCounter = 0;

        for (char bit : encodedData.toCharArray()) {
            if (bit == '1') byteBuffer |= (1 << (7 - bitCounter));
            bitCounter++;

            if (bitCounter == 8) {
                fileOutput.write(byteBuffer);
                byteBuffer = 0;
                bitCounter = 0;
            }
        }

        if (bitCounter > 0) {
            fileOutput.write(byteBuffer);  // Записываем остаточный байт
        }
    }

    // Загрузка таблицы кодов из файла
    private static void loadEncodingTable(FileInputStream inputStream) throws IOException {
        int entryCount = inputStream.read();
        if (entryCount == -1) throw new IOException("Ошибка при чтении таблицы кодов.");

        for (int i = 0; i < entryCount; i++) {
            int character = inputStream.read();
            if (character == -1) throw new IOException("Ошибка при чтении символа.");

            byte byteChar = (byte) character;
            int codeLength = inputStream.read();
            if (codeLength == -1) throw new IOException("Ошибка при чтении длины кода.");

            StringBuilder codeBuilder = new StringBuilder();
            int byteValue = 0;
            int bitIndex = 0;

            while (bitIndex < codeLength) {
                if (bitIndex % 8 == 0) {
                    byteValue = inputStream.read();
                    if (byteValue == -1) throw new IOException("Ошибка при чтении бита.");
                }

                int currentBit = (byteValue >> (7 - (bitIndex % 8))) & 1;
                codeBuilder.append(currentBit == 1 ? '1' : '0');
                bitIndex++;
            }

            encodingTable.put(byteChar, codeBuilder.toString());  // Сохраняем код
        }
    }

    // Чтение закодированных данных из файла
    private static String readEncodedText(FileInputStream inputStream) throws IOException {
        byte[] sizeBytes = new byte[4];
        inputStream.read(sizeBytes);

        int totalBits = ((sizeBytes[0] & 0xFF) << 24) |
                        ((sizeBytes[1] & 0xFF) << 16) |
                        ((sizeBytes[2] & 0xFF) << 8) |
                        (sizeBytes[3] & 0xFF);

        StringBuilder encodedText = new StringBuilder();
        int byteValue;
        int bitsRead = 0;

        while ((byteValue = inputStream.read()) != -1 && bitsRead < totalBits) {
            for (int i = 7; i >= 0 && bitsRead < totalBits; i--) {
                encodedText.append(((byteValue >> i) & 1) == 1 ? '1' : '0');
                bitsRead++;
            }
        }

        return encodedText.toString();
    }

    // Декодирование строки с использованием таблицы кодов
    private static byte[] decode(String encodedData) {
        Map<String, Byte> reversedTable = new HashMap<>();
        for (Map.Entry<Byte, String> entry : encodingTable.entrySet()) {
            reversedTable.put(entry.getValue(), entry.getKey());  // Обратная таблица
        }

        List<Byte> decodedBytes = new ArrayList<>();
        StringBuilder currentCode = new StringBuilder();

        for (char bit : encodedData.toCharArray()) {
            currentCode.append(bit);
            if (reversedTable.containsKey(currentCode.toString())) {
                decodedBytes.add(reversedTable.get(currentCode.toString()));  // Добавляем символ
                currentCode.setLength(0);  // Сбрасываем текущий код
            }
        }

        byte[] result = new byte[decodedBytes.size()];
        for (int i = 0; i < decodedBytes.size(); i++) {
            result[i] = decodedBytes.get(i);
        }

        return result;
    }

    // Главная функция для работы с программой
    public static void main(String[] args) {
        if (args.length != 3 || (!args[0].equals("compress") && !args[0].equals("decompress"))) {
            System.out.println("Неверный формат. Использование: java HuffmanProcessor <compress|decompress> <InputFile> <OutputFile>");
            return;
        }

        String operation = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        try {
            if (operation.equals("compress")) {
                byte[] fileData = loadFile(inputFile);
                compress(fileData, outputFile);  // Кодируем файл
            } else {
                decompress(inputFile, outputFile);  // Декодируем файл
            }
            System.out.println("Операция выполнена.");
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    // Загрузка файла в массив байтов
    private static byte[] loadFile(String filePath) throws IOException {
        File file = new File(filePath);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Файл слишком велик.");
        }

        byte[] fileData = new byte[(int) length];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int bytesRead = fileInputStream.read(fileData);
            if (bytesRead != length) {
                throw new IOException("Ошибка чтения файла.");
            }
        }
        return fileData;
    }
}
