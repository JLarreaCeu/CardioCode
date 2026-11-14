package server.db.interfaces;

import commonInfo.Patient;

import java.util.List;

/**
 * Operaciones sobre los datos personales de los pacientes (tabla patients).
 */
public interface PatientManager {

    /** Guarda un paciente nuevo y le asigna el id. Necesita el userId de su cuenta. */
    void addPatient(Patient patient);

    Patient getPatientById(Integer id);

    /** Para saber qué paciente es quien acaba de hacer login. */
    Patient getPatientByUserId(Integer userId);

    List<Patient> getPatients();

    /** Los pacientes que lleva un médico (GET_PATIENTS). */
    List<Patient> getPatientsByDoctor(Integer doctorId);

    /** Cambia los datos personales (UPDATE_PATIENT). No cambia su cuenta (userId). */
    void updatePatient(Patient patient);
}