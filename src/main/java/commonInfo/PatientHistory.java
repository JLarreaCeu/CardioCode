package commonInfo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Historial completo de un paciente: sus datos, sus informes de síntomas
 * y sus grabaciones. No es una tabla de la base de datos: el servidor lo
 * monta juntando datos de varias tablas para responder a GET_PATIENT_HISTORY.
 */
public class PatientHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Patient patient;
    private ArrayList<SymptomReport> reports;
    private ArrayList<Recording> recordings;

    public PatientHistory() {
        this.reports = new ArrayList<>();
        this.recordings = new ArrayList<>();
    }

    public PatientHistory(Patient patient) {
        this.patient = patient;
        this.reports = new ArrayList<>();
        this.recordings = new ArrayList<>();
    }

    public PatientHistory(Patient patient, ArrayList<SymptomReport> reports,
                          ArrayList<Recording> recordings) {
        this.patient = patient;
        this.reports = reports;
        this.recordings = recordings;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public ArrayList<SymptomReport> getReports() {
        return reports;
    }

    public void setReports(ArrayList<SymptomReport> reports) {
        this.reports = reports;
    }

    public ArrayList<Recording> getRecordings() {
        return recordings;
    }

    public void setRecordings(ArrayList<Recording> recordings) {
        this.recordings = recordings;
    }

    public void addReport(SymptomReport report) {
        reports.add(report);
    }

    public void addRecording(Recording recording) {
        recordings.add(recording);
    }

    @Override
    public String toString() {
        return "PatientHistory of " + patient
                + " (" + reports.size() + " reports, "
                + recordings.size() + " recordings)";
    }
}