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
import java.text.Normalizer;


public class RecordingFileWriter implements AutoCloseable {

    private static final Path RECORDINGS_DIR = Paths.get("recordings");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final String HEADER= "EMG_adc, ECG_adc";

    private BufferedWriter writer;
    private Path filePath;


    public String open(String name, String surname) throws IOException {
        if (writer != null) {

            throw new IllegalStateException("A recording is already open: " + filePath);
        }
        Files.createDirectories(RECORDINGS_DIR);

        // recordings/Maria-Jose_Cuevas-Alonso_2026-11-22_19-12-05.txt
        String fileName = cleanName(name) + "_" + cleanName(surname) + "_" + LocalDateTime.now().format(FILE_DATE) + ".txt";
        filePath = RECORDINGS_DIR.resolve(fileName);

        writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return filePath.toString();
    }

    public void writeBlock(int[][] samples) throws IOException {
        if (writer == null) {
            throw new IllegalStateException("No recording is open");

        }
        if (samples == null || samples.length<2) {
            return;
        }
        int n=Math.min(samples[0].length,samples[1].length);
        for (int i=0; i<n; i++) {
            writer.write(samples[0][i]+","+samples[1][i]);
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


    public static int[][] readAll(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        int[] emg = new int[lines.size()];
        int[] ecg = new int[lines.size()];

        int count = 0;

        for (String line : lines) {
            String text = line.trim();
            if (text.isEmpty()) {
                continue;
            }
            String[]parts=text.split(",");
            try {
                if(parts.length>=2){
                    emg[count]=Integer.parseInt(parts[0].trim());
                    ecg[count]=Integer.parseInt(parts[1].trim());
                }else{
                    ecg[count]=Integer.parseInt(parts[0].trim());
                }
                count++;
            } catch (NumberFormatException e) {
            }
        }
        return new int [][]{Arrays.copyOf(emg, count),Arrays.copyOf(ecg,count)};
    }

    private String cleanName(String text) {
        if (text == null) {
            return "unknown";
        }
        String clean = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        clean = clean.replaceAll("\\p{M}", "");
        clean = clean.replaceAll("[^A-Za-z0-9]+", "-");
        clean = clean.replaceAll("^-|-$", "");
        if (clean.isEmpty()) {
            return "unknown";
        }
        return clean;
    }
}
