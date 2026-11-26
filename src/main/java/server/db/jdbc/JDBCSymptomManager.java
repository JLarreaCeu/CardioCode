package server.db.jdbc;

import commonInfo.SymptomReport;
import server.db.interfaces.SymptomManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JDBCSymptomManager implements SymptomManager {

    private static final String SELECT_REPORTS =
            "SELECT id, patient_id, date_time, palpitations, dizziness, shortness_of_breath, "
                    + "chest_pain, fatigue, episode_minutes, comments, doctor_comment, doctor_comment_time, session_id "
                    + "FROM symptom_reports ";

    @Override
    public void addReport(SymptomReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Report is required");
        }
        if (report.getPatientId() == null) {
            throw new IllegalArgumentException("The report needs a patient");
        }
        if (report.getEpisodeMinutes() < 0) {
            throw new IllegalArgumentException("Episode minutes cannot be negative");
        }
        if (report.getDateTime() == null) {
            report.setDateTime(LocalDateTime.now());   // si el cliente no la puso, la hora de llegada
        }

        String sql = "INSERT INTO symptom_reports (patient_id, date_time, palpitations, dizziness, "
                + "shortness_of_breath, chest_pain, fatigue, episode_minutes, comments, session_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, report.getPatientId());
            ps.setString(2, report.getDateTime().toString());   // guarda "2026-11-12T15:08:04"
            ps.setInt(3, toInt(report.isPalpitations()));
            ps.setInt(4, toInt(report.isDizziness()));
            ps.setInt(5, toInt(report.isShortnessOfBreath()));
            ps.setInt(6, toInt(report.isChestPain()));
            ps.setInt(7, toInt(report.isFatigue()));
            ps.setInt(8, report.getEpisodeMinutes());
            ps.setString(9, report.getComments());
            ps.setInt(10, report.getSessionId());
            ps.executeUpdate();
            report.setId(lastInsertedId(c));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the symptom report", e);
        }
    }

    @Override
    public List<SymptomReport> getReportsByPatient(Integer patientId) {
        List<SymptomReport> reports = new ArrayList<>();
        if (patientId == null) {
            return reports;
        }
        String sql = SELECT_REPORTS + "WHERE patient_id = ? ORDER BY date_time DESC";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(readReport(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    @Override
    public void addDoctorComment(Integer reportId, String comment) {
        if (reportId == null) {
            throw new IllegalArgumentException("Report id is required");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment is required");
        }
        String sql = "UPDATE symptom_reports SET doctor_comment = ?, doctor_comment_time = ? WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, comment);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, reportId);
            int changed = ps.executeUpdate();   // cuántas filas se han modificado
            if (changed == 0) {
                throw new IllegalArgumentException("Report not found: " + reportId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the doctor's comment", e);
        }
    }

    /** Convierte la fila actual en un SymptomReport. */
    private SymptomReport readReport(ResultSet rs) throws SQLException {
        SymptomReport report = new SymptomReport();
        report.setId(rs.getInt("id"));
        report.setPatientId(rs.getInt("patient_id"));
        report.setDateTime(LocalDateTime.parse(rs.getString("date_time")));
        report.setPalpitations(rs.getInt("palpitations") == 1);
        report.setDizziness(rs.getInt("dizziness") == 1);
        report.setShortnessOfBreath(rs.getInt("shortness_of_breath") == 1);
        report.setChestPain(rs.getInt("chest_pain") == 1);
        report.setFatigue(rs.getInt("fatigue") == 1);
        report.setEpisodeMinutes(rs.getInt("episode_minutes"));
        report.setComments(rs.getString("comments"));
        report.setDoctorComment(rs.getString("doctor_comment"));
        int sessionId = rs.getInt("session_id");
        if (!rs.wasNull()) {
            report.setSessionId(sessionId);
        }
        String commentTime = rs.getString("doctor_comment_time");
        if (commentTime != null) {
            report.setDoctorCommentTime(LocalDateTime.parse(commentTime));
        }
        return report;
    }

    /** SQLite no tiene booleanos: true se guarda como 1 y false como 0. */
    private int toInt(boolean value) {
        if (value) {
            return 1;
        }
        return 0;
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