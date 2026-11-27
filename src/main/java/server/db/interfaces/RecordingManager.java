package server.db.interfaces;

import commonInfo.Recording;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Operaciones sobre las sesiones de grabación (tabla recordings).
 * Las muestras de la señal no van aquí: están en el fichero de filePath.
 */
public interface RecordingManager {

    /** Guarda una grabación al empezar (START_RECORDING) y le asigna el id de sesión. */
    void addRecording(Recording recording);

    /** Guarda la ruta del fichero de muestras (se crea después de tener el id de sesión). */
    void setFilePath(Integer recordingId, String filePath);

    /** Guarda la hora a la que termina la grabación (STOP_RECORDING). */
    void setEndTime(Integer recordingId, LocalDateTime endTime);

    Recording getRecordingById(Integer id);

    /** Todas las grabaciones de un paciente, de la más reciente a la más antigua. */
    List<Recording> getRecordingsByPatient(Integer patientId);
}