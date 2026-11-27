package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de RecordingFileWriter: abrir el fichero de una grabación, escribir bloques
 * de muestras (fila 0 = EMG, fila 1 = ECG), cerrarlo y volver a leerlo con readAll.
 *
 * RecordingFileWriter siempre escribe en la carpeta "recordings/" del directorio de trabajo,
 * así que cada test borra los ficheros que crea al terminar.
 */
public class RecordingFileWriterTest {

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
        String path = writer.open(name, "JUnitTest" + System.nanoTime());
        createdFiles.add(Paths.get(path));
        return path;
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
    void openCreatesEmptyFileInRecordingsFolder() throws IOException {
        RecordingFileWriter writer = newWriter();
        assertFalse(writer.isOpen());
        assertNull(writer.getFilePath());

        String path = open(writer, "Maria");

        assertTrue(writer.isOpen());
        assertEquals(path, writer.getFilePath());
        Path file = Paths.get(path);
        assertTrue(Files.exists(file));
        assertEquals(Paths.get("recordings"), file.getParent());
        assertEquals(0, Files.size(file));
    }

    @Test
    void fileNameHasCleanNameSurnameAndDate() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = writer.open("  María José ", "Cuevas Alonso");
        createdFiles.add(Paths.get(path));

        String fileName = Paths.get(path).getFileName().toString();
        // Sin tildes, espacios cambiados por guiones y fecha yyyy-MM-dd_HH-mm-ss
        assertTrue(fileName.matches("Maria-Jose_Cuevas-Alonso_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.txt"),
                "Unexpected file name: " + fileName);
    }

    @Test
    void nullOrSymbolOnlyNamesBecomeUnknown() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = writer.open(null, "@@@");
        createdFiles.add(Paths.get(path));

        String fileName = Paths.get(path).getFileName().toString();
        assertTrue(fileName.startsWith("unknown_unknown_"), "Unexpected file name: " + fileName);
    }

    @Test
    void openingTwiceWithoutClosingIsRejected() throws IOException {
        RecordingFileWriter writer = newWriter();
        open(writer, "Ana");

        assertThrows(IllegalStateException.class, () -> writer.open("Ana", "Other"));
    }

    // ---------- writeBlock ----------

    @Test
    void writeBlockBeforeOpenIsRejected() {
        RecordingFileWriter writer = newWriter();
        int[][] block = {{1, 2}, {3, 4}};

        assertThrows(IllegalStateException.class, () -> writer.writeBlock(block));
    }

    @Test
    void writeBlockWritesOneLineEmgCommaEcgPerSample() throws IOException {
        RecordingFileWriter writer = newWriter();
        String path = open(writer, "Luis");

        writer.writeBlock(new int[][]{{10, 20, 30}, {500, 510, 520}});
        writer.close();

        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        assertEquals(List.of("10,500", "20,510", "30,520"), lines);
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

        assertEquals(List.of("7,700"), Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8));
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

        assertEquals(0, Files.size(Paths.get(path)));
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
    void readAllSkipsBlankAndInvalidLines() throws IOException {
        Path file = tempDir.resolve("mixed.txt");
        Files.writeString(file, "EMG_adc, ECG_adc\n1,100\n\n  2 , 200 \nabc,def\n3,300\n");

        int[][] samples = RecordingFileWriter.readAll(file.toString());
        assertArrayEquals(new int[]{1, 2, 3}, samples[0]);
        assertArrayEquals(new int[]{100, 200, 300}, samples[1]);
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
