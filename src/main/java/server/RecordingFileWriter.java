package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


public class RecordingFileWriter implements AutoCloseable {

    private static final Path RECORDINGS_DIR = Paths.get("recordings");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private BufferedWriter writer;
    private Path filePath;


    public String open(int patientId) throws IOException {
        if (writer != null) {

            throw new IllegalStateException("A recording is already open: " + filePath);
        }
        Files.createDirectories(RECORDINGS_DIR);

        // recordings/patient3_2026-11-22_19-12-05.txt
        String fileName = "patient" + patientId + "_" + LocalDateTime.now().format(FILE_DATE) + ".txt";
        filePath = RECORDINGS_DIR.resolve(fileName);

        writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return filePath.toString();
    }

    public void writeBlock(int[] samples) throws IOException {
        if (writer == null) {
            throw new IllegalStateException("No recording is open");

        }
        if (samples == null) {
            return;
        }
        for (int sample : samples) {
            writer.write(Integer.toString(sample));
            writer.newLine();
        }
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            try {

                writer.close();
            } finally {

                writer = null;
            }
        }
    }

    public boolean isOpen() {
        return writer != null;
    }

    public String getFilePath() {
        if (filePath == null) {
            return null;
        }
        return filePath.toString();
    }


    public static int[] readAll(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        int[] samples = new int[lines.size()];

        int count = 0;

        for (String line : lines) {
            String text = line.trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                samples[count] = Integer.parseInt(text);
                count++;
            } catch (NumberFormatException e) {
            }
        }
        return Arrays.copyOf(samples, count);
    }
}