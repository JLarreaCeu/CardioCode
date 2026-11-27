package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de RecordingFileWriter: abrir el fichero de una grabación, escribir bloques
 * de muestras (fila 0 = EMG, fila 1 = ECG), cerrarlo y volver a leerlo con readAll.
 *
 * Formato del fichero: líneas de metadatos que empiezan por # (paciente, sesión, hora de inicio,
 * frecuencia, canales, unidades), la cabecera "time_s,EMG_adc,ECG_adc" y una línea por muestra.
 *
 * RecordingFileWriter siempre escribe en la carpeta "recordings/" del directorio de trabajo,
 * así que cada test borra los ficheros que crea al terminar.
 */
public class RecordingFileWriterTest {

    private static final int RATE = 1000;
    private static final String HEADER = RecordingFileWriter.HEADER;

    private final List<RecordingFileWriter> writers = new ArrayList<>();
    private final List<Path> createdFiles = new ArrayList<>();

    @TempDir
    Path tempDir;

    /** Crea un writer y lo apunta para cerrarlo y borrar su fichero al final. */
    private RecordingFileWriter newWriter() {
        RecordingFileWriter writer = new RecordingFileWriter();
        writers.add(writer);
        return writer;
    }

    /** Abre un fichero con un nombre único para que no choque con grabaciones reales. */
    private String open(RecordingFileWriter writer, String name) throws IOException {
        return open(writer, name, RATE);
    }

    private String open(RecordingFileWriter writer, String name, int rate) throws IOException {
        String path = writer.open(1, 1, name, "JUnitTest" + System.nanoTime(), LocalDateTime.now(), rate);
        createdFiles.add(Paths.get(path));
        return path;
    }

    /** Líneas del fichero sin los metadatos (#): la cabecera de columnas y las muestras. */
    private static List<String> dataLines(String path) throws IOException {
        List<String> data = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
            if (!line.startsWith(RecordingFileWriter.COMMENT)) {
                data.add(line);
            }
        }
        return data;
    }

    @AfterEach
    void cleanUp() throws IOException {
        for (RecordingFileWriter writer : writers) {
            writer.close();
        }
        for (Path file : createdFiles) {
            Files.deleteIfExists(file);
        }
    }

    // ---------- open ----------

    @Test
    void openCreatesFileWithMetadataAndColumnHeader() throws IOException {
        RecordingFileWriter writer = newWriter();
        assertFalse(writer.isOpen());
        assertNull(writer.getFilePath());

        String path = open(writer, "Maria");

        assertTrue(writer.isOpen());
        assertEquals(path, writer.getFilePath());
        Path file = Paths.get(path);
        assertTrue(Files.exists(file));
        assertEquals(Paths.get("recordings"), file.getParent());
        assertEquals(List.of(HEADER), dataLines(path));
    }

    @Test
    void metadataHasIdsStartTimeRateChannelsAndUnits() throws IOException {
        RecordingFileWriter writer = newWriter();
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 10, 15, 2);
        String path = writer.open(7, 42, "Ana", "JUnitTest" + System.nanoTime(), start, RATE);
        createdFiles.add(Paths.get(path));

        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        assertTrue(lines.contains("# patient_id=7"));
        assertTrue(lines.contains("# session_id=42"));
        assertTrue(lines.contains("# start_time=2026-11-27T10:15:02"));
        assertTrue(lines.contains("# sampling_rate_hz=1000"));
        assertTrue(lines.contains("# channels=EMG:A1,ECG:A2"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("# units=") && l.contains("time_s:s") && l.contains("ADC")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("# time_reference=")));
        // Los metadatos van antes de la cabecera de columnas
        assertEquals(HEADER, lines.get(lines.size() - 1));
    }

    @Test
    void fileNameHasPatientAndSessionIdsCleanNameSurnameAndDate() throws IOException {
        RecordingFileWriter writer = newWriter();
        String surname = "Cuevas Alonso " + System.nanoTime();
        String path = writer.open(7, 42, "  María José ", surname, LocalDateTime.of(2026, 11, 22, 19, 12, 5), RATE);
        createdFiles.add(Paths.get(path));

        String fileName = Paths.get(path).getFileName().toString();
        // Sin tildes, espacios cambiados por guiones y la fecha de inicio de la sesión
        assertTrue(fileName.matches("P7_S42_Maria-Jose_Cuevas-Alonso-\\d+_2026-11-22_19-12-05\\.txt"),
                "Unexpected file name: " + fileName);
    }

    @Test
    void nullOrSymbolOnlyNamesBecomeUnknown() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = writer.open(3, 9, null, "@@@", LocalDateTime.now(), RATE);
        createdFiles.add(Paths.get(path));

        String fileName = Paths.get(path).getFileName().toString();
        assertTrue(fileName.startsWith("P3_S9_unknown_unknown_"), "Unexpected file name: " + fileName);
    }

    @Test
    void openingTwiceWithoutClosingIsRejected() throws IOException {
        RecordingFileWriter writer = newWriter();
        open(writer, "Ana");

        assertThrows(IllegalStateException.class,
                () -> writer.open(1, 2, "Ana", "Other", LocalDateTime.now(), RATE));
    }

    @Test
    void zeroOrNegativeSamplingRateIsRejected() {
        RecordingFileWriter writer = newWriter();

        assertThrows(IllegalArgumentException.class,
                () -> writer.open(1, 1, "Ana", "Rate", LocalDateTime.now(), 0));
        assertFalse(writer.isOpen());
    }

    // ---------- writeBlock ----------

    @Test
    void writeBlockBeforeOpenIsRejected() {
        RecordingFileWriter writer = newWriter();
        int[][] block = {{1, 2}, {3, 4}};

        assertThrows(IllegalStateException.class, () -> writer.writeBlock(block));
    }

    @Test
    void writeBlockWritesOneLineTimeEmgEcgPerSample() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Luis");

        writer.writeBlock(new int[][]{{10, 20, 30}, {500, 510, 520}});
        writer.close();

        assertEquals(List.of(HEADER, "0.000,10,500", "0.001,20,510", "0.002,30,520"), dataLines(path));
    }

    @Test
    void timeContinuesAcrossBlocksAndUsesTheSamplingRate() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Tiempo", 100);   // 100 Hz -> una muestra cada 0.01 s

        writer.writeBlock(new int[][]{{1, 2}, {101, 102}});
        writer.writeBlock(new int[][]{{3}, {103}});
        writer.close();

        assertEquals(List.of(HEADER, "0.000,1,101", "0.010,2,102", "0.020,3,103"), dataLines(path));
    }

    @Test
    void timeUsesADecimalPointEvenWithASpanishLocale() throws IOException {
        // En español el decimal es una coma y rompería las columnas del CSV
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("es-ES"));
        try {
            RecordingFileWriter writer = newWriter();
            String path = open(writer, "Locale");
            writer.writeBlock(new int[][]{{1, 2}, {101, 102}});
            writer.close();

            assertEquals(List.of(HEADER, "0.000,1,101", "0.001,2,102"), dataLines(path));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void severalBlocksAreAppendedInOrder() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Eva");

        writer.writeBlock(new int[][]{{1, 2}, {101, 102}});
        writer.writeBlock(new int[][]{{3}, {103}});
        writer.close();

        int[][] samples = RecordingFileWriter.readAll(path);
        assertArrayEquals(new int[]{1, 2, 3}, samples[0]);
        assertArrayEquals(new int[]{101, 102, 103}, samples[1]);
    }

    @Test
    void writeBlockIsFlushedBeforeClose() throws IOException {
        // Si la conexión se corta a mitad de grabación, lo recibido ya tiene que estar en disco
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Flush");

        writer.writeBlock(new int[][]{{7}, {700}});

        assertEquals(List.of(HEADER, "0.000,7,700"), dataLines(path));
    }

    @Test
    void rowsOfDifferentLengthOnlyWriteCommonSamples() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Pedro");

        writer.writeBlock(new int[][]{{1, 2, 3}, {100, 200}});
        writer.close();

        int[][] samples = RecordingFileWriter.readAll(path);
        assertArrayEquals(new int[]{1, 2}, samples[0]);
        assertArrayEquals(new int[]{100, 200}, samples[1]);
    }

    @Test
    void nullOrSingleRowBlocksAreIgnored() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Nada");

        writer.writeBlock(null);
        writer.writeBlock(new int[][]{{1, 2, 3}});
        writer.close();

        assertEquals(List.of(HEADER), dataLines(path));
    }

    // ---------- close ----------

    @Test
    void closeCanBeCalledTwiceAndAllowsOpeningANewFile() throws IOException {
        RecordingFileWriter writer = newWriter();
        open(writer, "Uno");
        writer.close();
        writer.close();
        assertFalse(writer.isOpen());

        open(writer, "Dos");
        assertTrue(writer.isOpen());
    }

    @Test
    void newFileStartsTheTimeAtZeroAgain() throws IOException {
        RecordingFileWriter writer = newWriter();
        open(writer, "Primera");
        writer.writeBlock(new int[][]{{1, 2}, {101, 102}});
        writer.close();

        String second = open(writer, "Segunda");
        writer.writeBlock(new int[][]{{3}, {103}});
        writer.close();

        assertEquals(List.of(HEADER, "0.000,3,103"), dataLines(second));
    }

    @Test
    void writeBlockAfterCloseIsRejected() throws IOException {
        RecordingFileWriter writer = newWriter();
        open(writer, "Cerrado");
        writer.close();

        assertThrows(IllegalStateException.class, () -> writer.writeBlock(new int[][]{{1}, {2}}));
    }

    // ---------- readAll ----------

    @Test
    void readAllOfEmptyRecordingReturnsTwoEmptyRows() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Vacio");
        writer.close();

        int[][] samples = RecordingFileWriter.readAll(path);
        assertEquals(2, samples.length);
        assertEquals(0, samples[0].length);
        assertEquals(0, samples[1].length);
    }

    @Test
    void readAllSkipsMetadataAndIgnoresTheTimeColumn() throws IOException {
        // Los metadatos dicen ECG y EMG, pero empiezan por # y no se toman como cabecera
        Path file = tempDir.resolve("with_metadata.txt");
        Files.writeString(file, "# channels=ECG:A2,EMG:A1\n# units=ECG_adc:ADC\ntime_s,EMG_adc,ECG_adc\n"
                + "0.000,1,100\n0.001,2,200\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{1, 2}, samples[RecordingFileWriter.EMG_ROW]);
        assertArrayEquals(new int[]{100, 200}, samples[RecordingFileWriter.ECG_ROW]);
    }

    @Test
    void readAllSkipsBlankAndInvalidLines() throws IOException {
        Path file = tempDir.resolve("mixed.txt");
        Files.writeString(file, "EMG_adc, ECG_adc\n1,100\n\n  2 , 200 \nabc,def\n3,300\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{1, 2, 3}, samples[0]);
        assertArrayEquals(new int[]{100, 200, 300}, samples[1]);
    }

    @Test
    void readAllReturnsEmgAndEcgInTheirRows() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Filas");
        writer.writeBlock(new int[][]{{11, 12}, {511, 512}});
        writer.close();

        int[][] samples = RecordingFileWriter.readAll(path);
        assertArrayEquals(new int[]{11, 12}, samples[RecordingFileWriter.EMG_ROW]);
        assertArrayEquals(new int[]{511, 512}, samples[RecordingFileWriter.ECG_ROW]);
    }

    @Test
    void readAllUsesTheHeaderToKnowWhichColumnIsEachSignal() throws IOException {
        // Columnas al revés que lo normal: la cabecera dice que la primera es el ECG
        Path file = tempDir.resolve("reversed.txt");
        Files.writeString(file, "ECG_adc,EMG_adc\n500,10\n510,20\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{10, 20}, samples[RecordingFileWriter.EMG_ROW]);
        assertArrayEquals(new int[]{500, 510}, samples[RecordingFileWriter.ECG_ROW]);
    }

    @Test
    void readAllWithOnlyEcgInTheHeaderLeavesEmgAtZero() throws IOException {
        Path file = tempDir.resolve("ecg_header.txt");
        Files.writeString(file, "ECG_adc\n500\n510\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{0, 0}, samples[RecordingFileWriter.EMG_ROW]);
        assertArrayEquals(new int[]{500, 510}, samples[RecordingFileWriter.ECG_ROW]);
    }

    @Test
    void readAllSupportsOldEcgOnlyFiles() throws IOException {
        // Grabaciones antiguas: una sola columna con el ECG; el EMG queda a 0
        Path file = tempDir.resolve("ecg_only.txt");
        Files.writeString(file, "512\n600\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{0, 0}, samples[0]);
        assertArrayEquals(new int[]{512, 600}, samples[1]);
    }

    @Test
    void readAllOfMissingFileThrowsIOException() {
        Path missing = tempDir.resolve("does_not_exist.txt");

        assertThrows(IOException.class, () -> RecordingFileWriter.readAll(missing.toString()));
    }
}
