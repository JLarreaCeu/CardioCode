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
import java.util.Locale;
import java.text.Normalizer;


public class RecordingFileWriter implements AutoCloseable {

    private static final Path RECORDINGS_DIR = Paths.get("recordings");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // Líneas de metadatos: empiezan por # y readAll se las salta
    public static final String COMMENT = "#";

    // Línea de columnas: tiempo desde el inicio de la sesión y las dos señales
    public static final String HEADER = "time_s,EMG_adc,ECG_adc";

    // Filas del int[][] que devuelve readAll (las mismas que BitalinoRecorder.EMG_ROW / ECG_ROW)
    public static final int EMG_ROW = 0;
    public static final int ECG_ROW = 1;

    private BufferedWriter writer;
    private Path filePath;
    private int samplingRate;
    private long sampleIndex;   // número de la siguiente muestra, para calcular su tiempo


    /**
     * Crea el fichero de una sesión de monitorización y escribe la cabecera con los metadatos:
     * paciente, sesión, hora de inicio, frecuencia, canales y unidades.
     * Nombre: recordings/P3_S12_Maria-Jose_Cuevas-Alonso_2026-11-22_19-12-05.txt
     *
     * @param patientId    id del paciente en la base de datos
     * @param sessionId    id de la grabación en la base de datos (identificador de la sesión)
     * @param startTime    hora de inicio de la sesión; el tiempo de cada muestra se cuenta desde aquí
     * @param samplingRate frecuencia de muestreo en Hz
     * @return ruta del fichero creado
     */
    public String open(int patientId, int sessionId, String name, String surname,
                       LocalDateTime startTime, int samplingRate) throws IOException {
        if (writer != null) {

            throw new IllegalStateException("A recording is already open: " + filePath);
        }
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        Files.createDirectories(RECORDINGS_DIR);

        String fileName = "P" + patientId + "_S" + sessionId + "_" + cleanName(name) + "_" + cleanName(surname)
                + "_" + startTime.format(FILE_DATE) + ".txt";
        filePath = RECORDINGS_DIR.resolve(fileName);

        writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        this.samplingRate = samplingRate;
        this.sampleIndex = 0;

        writeLine(COMMENT + " CardioCode recording");
        writeLine(COMMENT + " patient_id=" + patientId);
        writeLine(COMMENT + " session_id=" + sessionId);
        writeLine(COMMENT + " start_time=" + startTime);
        writeLine(COMMENT + " sampling_rate_hz=" + samplingRate);
        writeLine(COMMENT + " channels=EMG:A1,ECG:A2");
        writeLine(COMMENT + " units=time_s:s,EMG_adc:ADC 0-1023,ECG_adc:ADC 0-1023");
        writeLine(COMMENT + " time_reference=time_s is seconds since start_time (sample n -> n/sampling_rate_hz)");
        writeLine(COMMENT + " EMG_mV=(ADC/1024-0.5)*3.3/1009*1000");
        writeLine(COMMENT + " ECG_mV=(ADC/1024-0.5)*3.3/1100*1000");
        writeLine(HEADER);
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
            // Locale.ROOT: punto decimal siempre, aunque el ordenador esté en español
            String time = String.format(Locale.ROOT, "%.3f", sampleIndex / (double) samplingRate);
            writeLine(time + "," + samples[0][i] + "," + samples[1][i]);
            sampleIndex++;
        }
        writer.flush();
    }

    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
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
     * Las líneas de metadatos (empiezan por #) se saltan.
     * La cabecera del fichero (por ejemplo "time_s,EMG_adc,ECG_adc") dice en qué columna está cada señal,
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
            if (text.isEmpty() || text.startsWith(COMMENT)) {
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
