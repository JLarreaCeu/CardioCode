package server.db.jdbc;

import commonInfo.Patient;
import server.db.interfaces.PatientManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JDBCPatientManager implements PatientManager {

    private static final String SELECT_PATIENTS =
            "SELECT id, user_id, name, surname, birth_date, sex, phone, doctor_id FROM patients ";

    @Override
    public void addPatient(Patient patient) {
        validate(patient);
        if (patient.getUserId() == null) {
            throw new IllegalArgumentException("The patient needs a user account");
        }
        String sql = "INSERT INTO patients (user_id, name, surname, birth_date, sex, phone, doctor_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, patient.getUserId());
            setPersonalData(ps, patient, 2);   // rellena los parámetros 2 a 7
            ps.executeUpdate();
            patient.setId(lastInsertedId(c));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save patient " + patient.getName(), e);
        }
    }

    @Override
    public Patient getPatientById(Integer id) {
        if (id == null) {
            return null;
        }
        return firstOrNull(queryPatients(SELECT_PATIENTS + "WHERE id = ?", id));
    }

    @Override
    public Patient getPatientByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        return firstOrNull(queryPatients(SELECT_PATIENTS + "WHERE user_id = ?", userId));
    }

    @Override
    public List<Patient> getPatients() {
        return queryPatients(SELECT_PATIENTS, null);
    }

    @Override
    public List<Patient> getPatientsByDoctor(Integer doctorId) {
        if (doctorId == null) {
            return new ArrayList<>();
        }
        return queryPatients(SELECT_PATIENTS + "WHERE doctor_id = ?", doctorId);
    }

    @Override
    public void updatePatient(Patient patient) {
        validate(patient);
        if (patient.getId() == null) {
            throw new IllegalArgumentException("The patient is not saved yet");
        }
        String sql = "UPDATE patients SET name = ?, surname = ?, birth_date = ?, sex = ?, "
                + "phone = ?, doctor_id = ? WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setPersonalData(ps, patient, 1);   // rellena los parámetros 1 a 6
            ps.setInt(7, patient.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update patient " + patient.getId(), e);
        }
    }

    /** Comprueba los datos obligatorios antes de guardar. */
    private void validate(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (patient.getName() == null || patient.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (patient.getSurname() == null || patient.getSurname().isBlank()) {
            throw new IllegalArgumentException("Surname is required");
        }
    }

    /**
     * Rellena name, surname, birth_date, sex, phone y doctor_id (6 parámetros seguidos)
     * empezando en la posición first. Lo usan addPatient y updatePatient.
     */
    private void setPersonalData(PreparedStatement ps, Patient patient, int first) throws SQLException {
        ps.setString(first, patient.getName());
        ps.setString(first + 1, patient.getSurname());
        if (patient.getBirthDate() == null) {
            ps.setNull(first + 2, Types.VARCHAR);
        } else {
            ps.setString(first + 2, patient.getBirthDate().toString());   // guarda "2001-05-23"
        }
        ps.setString(first + 3, patient.getSex());
        ps.setString(first + 4, patient.getPhone());
        if (patient.getDoctorId() == null) {
            ps.setNull(first + 5, Types.INTEGER);
        } else {
            ps.setInt(first + 5, patient.getDoctorId());
        }
    }

    /** Ejecuta un SELECT de pacientes con 0 o 1 parámetro y convierte cada fila en un Patient. */
    private List<Patient> queryPatients(String sql, Object param) {
        List<Patient> patients = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    patients.add(readPatient(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    /** Convierte la fila actual en un Patient. */
    private Patient readPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        patient.setUserId(rs.getInt("user_id"));
        patient.setName(rs.getString("name"));
        patient.setSurname(rs.getString("surname"));

        String birthDate = rs.getString("birth_date");
        if (birthDate != null) {
            patient.setBirthDate(LocalDate.parse(birthDate));   // convierte "2001-05-23" en LocalDate
        }

        patient.setSex(rs.getString("sex"));
        patient.setPhone(rs.getString("phone"));

        int doctorId = rs.getInt("doctor_id");
        if (!rs.wasNull()) {   // getInt devuelve 0 si la columna es NULL; wasNull lo distingue
            patient.setDoctorId(doctorId);
        }
        return patient;
    }

    private Patient firstOrNull(List<Patient> patients) {
        if (patients.isEmpty()) {
            return null;
        }
        return patients.get(0);
    }

    private int lastInsertedId(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        }
    }
}