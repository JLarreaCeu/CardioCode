package server.db.interfaces;

import commonInfo.SymptomReport;

import java.util.List;

/**
 * Operaciones sobre los informes de síntomas (tabla symptom_reports).
 */
public interface SymptomManager {

    /** Guarda el informe que envía el paciente (SEND_SYMPTOMS) y le asigna el id. */
    void addReport(SymptomReport report);

    /** Todos los informes de un paciente, del más reciente al más antiguo. */
    List<SymptomReport> getReportsByPatient(Integer patientId);

    /** Guarda el comentario del médico en un informe, con la hora actual. */
    void addDoctorComment(Integer reportId, String comment);
}