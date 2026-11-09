package commonInfo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Datos de una sesión de monitorización con el Bitalino.
 * Las muestras de la señal NO van aquí: se guardan en un fichero
 * (RecordingFileWriter) y este objeto guarda la ruta en filePath.
 * El id de la grabación sirve también como identificador de la sesión.
 */
public class Recording implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;                 // null hasta que la base de datos le asigna uno
    private Integer patientId;          // paciente que graba
    private LocalDateTime startTime;    // inicio de la sesión
    private LocalDateTime endTime;      // fin de la sesión (null mientras está en curso)
    private int samplingRate;           // frecuencia de muestreo, en Hz
    private String channel;             // canal(es) del Bitalino, por ejemplo "A2"
    private String units;               // unidades de la señal, por ejemplo "mV"
    private String filePath;            // ruta del fichero con las muestras (la pone el servidor)

    public Recording() {
    }

    /**
     * Constructor para empezar una grabación (START_RECORDING).
     * endTime y filePath se rellenan después: endTime al parar
     * y filePath cuando el servidor crea el fichero.
     */
    public Recording(Integer patientId, LocalDateTime startTime,
                     int samplingRate, String channel, String units) {
        this.patientId = patientId;
        this.startTime = startTime;
        this.samplingRate = samplingRate;
        this.channel = channel;
        this.units = units;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Recording other = (Recording) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(patientId, other.patientId)
                && Objects.equals(startTime, other.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, startTime);
    }

    @Override
    public String toString() {
        return "Recording " + id + " (patient " + patientId + ", " + startTime + " - " + endTime + ")";
    }
}