package server.db.interfaces;

import commonInfo.Doctor;

import java.util.List;

/**
 * Operaciones sobre los datos personales de los médicos
 */
public interface DoctorManager {

    /** Guarda un médico nuevo y le asigna el id se necesita el userId de su cuenta */
    void addDoctor(Doctor doctor);

    Doctor getDoctorById(Integer id);

    /** Para saber qué médico es quien acaba de hacer login */
    Doctor getDoctorByUserId(Integer userId);

    /** Todos los médicos, para que el paciente elija al registrarse */
    List<Doctor> getDoctors();

    void updateDoctor(Doctor doctor);
}