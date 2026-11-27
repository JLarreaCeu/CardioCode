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

    // Primera línea de cada fichero: dice qué señal hay en cada columna
    private static final String HEADER = "EMG_adc,ECG_adc";

    // Filas del int[][] que devuelve readAll (las mismas que BitalinoRecorder.EMG_ROW / ECG_ROW)
    public static final int EMG_ROW = 0;
    public static final int ECG_ROW = 1;

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
        writer.write(HEADER);
        writer.newLine();
        writer.flush();
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


    /**
     * Lee una grabación entera.
     * La cabecera del fichero (por ejemplo "EMG_adc,ECG_adc") dice en qué columna está cada señal,
     * así que da igual el orden en que estén guardadas.
     * Ficheros antiguos sin cabecera: 2 columnas = EMG,ECG; 1 columna = solo ECG.
     *
     * @return fila EMG_ROW con el EMG y fila ECG_ROW con el ECG (valores ADC 0-1023)
     */
    public static int[][] readAll(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        int[] emg = new int[lines.size()];
        int[] ecg = new int[lines.size()];

        // Columnas por defecto si el fichero no tiene cabecera
        int emgColumn = 0;
        int ecgColumn = 1;
        int count = 0;

        for (String line : lines) {
            String text = line.trim();
            if (text.isEmpty()) {
                continue;
            }
            String[] parts = text.split(",");

            if (isHeader(parts)) {
                emgColumn = columnOf(parts, "EMG");
                ecgColumn = columnOf(parts, "ECG");
                continue;
            }
            try {
                if (parts.length == 1) {
                    ecg[count] = Integer.parseInt(parts[0].trim());   // fichero antiguo, solo ECG
                } else {
                    emg[count] = valueAt(parts, emgColumn);
                    ecg[count] = valueAt(parts, ecgColumn);
                }
                count++;
            } catch (NumberFormatException e) {
                // línea que no son números: se salta
            }
        }
        int[][] samples = new int[2][];
        samples[EMG_ROW] = Arrays.copyOf(emg, count);
        samples[ECG_ROW] = Arrays.copyOf(ecg, count);
        return samples;
    }

    /** Una línea es cabecera si alguna columna se llama ECG... o EMG... */
    private static boolean isHeader(String[] parts) {
        return columnOf(parts, "ECG") != -1 || columnOf(parts, "EMG") != -1;
    }

    /** Posición de la columna cuyo nombre empieza por signal (ECG o EMG), o -1 si no está. */
    private static int columnOf(String[] parts, String signal) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().toUpperCase().startsWith(signal)) {
                return i;
            }
        }
        return -1;
    }

    /** Valor de una columna; 0 si esa señal no está en el fichero. */
    private static int valueAt(String[] parts, int column) {
        if (column < 0 || column >= parts.length) {
            return 0;
        }
        return Integer.parseInt(parts[column].trim());
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
