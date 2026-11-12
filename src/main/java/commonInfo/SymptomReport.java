package commonInfo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Síntomas que el paciente envía en un momento concreto.
 * Cubre lo que pide el enunciado: identificador del paciente,
 * fecha y hora, síntomas y los valores del modelo de monitorización.
 */
public class SymptomReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;                 // null hasta que la base de datos le asigna uno
    private Integer patientId;          // paciente que envía el informe
    private LocalDateTime dateTime;     // fecha y hora del envío

    // Síntomas típicos de la fibrilación auricular
    private boolean palpitations;
    private boolean dizziness;
    private boolean shortnessOfBreath;
    private boolean chestPain;
    private boolean fatigue;

    private int episodeMinutes;         // duración del episodio, en minutos
    private String comments;            // observaciones libres del paciente
    private String doctorComments;      // comentarios del médico (null hasta que lo revisa)

    public SymptomReport() {
    }

    public SymptomReport(Integer patientId, LocalDateTime dateTime,
                         boolean palpitations, boolean dizziness, boolean shortnessOfBreath,
                         boolean chestPain, boolean fatigue,
                         int episodeMinutes, String comments) {
        this.patientId = patientId;
        this.dateTime = dateTime;
        this.palpitations = palpitations;
        this.dizziness = dizziness;
        this.shortnessOfBreath = shortnessOfBreath;
        this.chestPain = chestPain;
        this.fatigue = fatigue;
        this.episodeMinutes = episodeMinutes;
        this.comments = comments;
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isPalpitations() {
        return palpitations;
    }

    public void setPalpitations(boolean palpitations) {
        this.palpitations = palpitations;
    }

    public boolean isDizziness() {
        return dizziness;
    }

    public void setDizziness(boolean dizziness) {
        this.dizziness = dizziness;
    }

    public boolean isShortnessOfBreath() {
        return shortnessOfBreath;
    }

    public void setShortnessOfBreath(boolean shortnessOfBreath) {
        this.shortnessOfBreath = shortnessOfBreath;
    }

    public boolean isChestPain() {
        return chestPain;
    }

    public void setChestPain(boolean chestPain) {
        this.chestPain = chestPain;
    }

    public boolean isFatigue() {
        return fatigue;
    }

    public void setFatigue(boolean fatigue) {
        this.fatigue = fatigue;
    }

    public int getEpisodeMinutes() {
        return episodeMinutes;
    }

    public void setEpisodeMinutes(int episodeMinutes) {
        this.episodeMinutes = episodeMinutes;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDoctorComments() {
        return doctorComments;
    }

    public void setDoctorComments(String doctorComments) {
        this.doctorComments = doctorComments;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SymptomReport other = (SymptomReport) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(patientId, other.patientId)
                && Objects.equals(dateTime, other.dateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, dateTime);
    }

    @Override
    public String toString() {
        return "SymptomReport " + id + " (patient " + patientId + ", " + dateTime + ")";
    }
}