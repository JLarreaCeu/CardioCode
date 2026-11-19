package server.db.jdbc;

import commonInfo.Doctor;
import server.db.interfaces.DoctorManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JDBCDoctorManager implements DoctorManager {

    private static final String SELECT_DOCTORS =
            "SELECT id, user_id, name, surname, phone FROM doctors ";

    @Override
    public void addDoctor(Doctor doctor) {
        validate(doctor);
        if (doctor.getUserId() == null) {
            throw new IllegalArgumentException("The doctor needs a user account");
        }
        String sql = "INSERT INTO doctors (user_id, name, surname, phone) VALUES (?, ?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctor.getUserId());
            ps.setString(2, doctor.getName());
            ps.setString(3, doctor.getSurname());
            ps.setString(4, doctor.getPhone());
            ps.executeUpdate();
            doctor.setId(lastInsertedId(c));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save doctor " + doctor.getName(), e);
        }
    }

    @Override
    public Doctor getDoctorById(Integer id) {
        if (id == null) {
            return null;
        }
        return firstOrNull(queryDoctors(SELECT_DOCTORS + "WHERE id = ?", id));
    }

    @Override
    public Doctor getDoctorByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        return firstOrNull(queryDoctors(SELECT_DOCTORS + "WHERE user_id = ?", userId));
    }

    @Override
    public List<Doctor> getDoctors() {
        return queryDoctors(SELECT_DOCTORS + "ORDER BY surname, name", null);
    }



    /** Comprueba los datos obligatorios antes de guardar. */
    private void validate(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor is required");
        }
        if (doctor.getName() == null || doctor.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (doctor.getSurname() == null || doctor.getSurname().isBlank()) {
            throw new IllegalArgumentException("Surname is required");
        }
    }

    /** Ejecuta un SELECT de médicos con 0 o 1 parámetro y convierte cada fila en un Doctor. */
    private List<Doctor> queryDoctors(String sql, Object param) {
        List<Doctor> doctors = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    doctors.add(readDoctor(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctors;
    }

    /** Convierte la fila actual en un Doctor. */
    private Doctor readDoctor(ResultSet rs) throws SQLException {
        Doctor doctor = new Doctor();
        doctor.setId(rs.getInt("id"));
        doctor.setUserId(rs.getInt("user_id"));
        doctor.setName(rs.getString("name"));
        doctor.setSurname(rs.getString("surname"));
        doctor.setPhone(rs.getString("phone"));
        return doctor;
    }

    private Doctor firstOrNull(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            return null;
        }
        return doctors.get(0);
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