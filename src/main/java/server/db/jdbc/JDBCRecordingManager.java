package server.db.jdbc;

import commonInfo.Recording;
import server.db.interfaces.RecordingManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JDBCRecordingManager implements RecordingManager {

    private static final String SELECT_RECORDINGS =
            "SELECT id, patient_id, start_time, end_time, sampling_rate, channel, units, file_path "
                    + "FROM recordings ";

    @Override
    public void addRecording(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("Recording is required");
        }
        if (recording.getPatientId() == null) {
            throw new IllegalArgumentException("The recording needs a patient");
        }
        if (recording.getSamplingRate() <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        if (recording.getChannel() == null || recording.getChannel().isBlank()) {
            throw new IllegalArgumentException("Channel is required");
        }
        if (recording.getUnits() == null || recording.getUnits().isBlank()) {
            throw new IllegalArgumentException("Units are required");
        }
        if (recording.getStartTime() == null) {
            recording.setStartTime(LocalDateTime.now());
        }

        // end_time no se pone: la grabación acaba de empezar
        String sql = "INSERT INTO recordings (patient_id, start_time, sampling_rate, channel, units, file_path) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recording.getPatientId());
            ps.setString(2, recording.getStartTime().toString());
            ps.setInt(3, recording.getSamplingRate());
            ps.setString(4, recording.getChannel());
            ps.setString(5, recording.getUnits());
            ps.setString(6, recording.getFilePath());
            ps.executeUpdate();
            recording.setId(lastInsertedId(c));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the recording", e);
        }
    }

    @Override
    public void setEndTime(Integer recordingId, LocalDateTime endTime) {
        if (recordingId == null) {
            throw new IllegalArgumentException("Recording id is required");
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        String sql = "UPDATE recordings SET end_time = ? WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, endTime.toString());
            ps.setInt(2, recordingId);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                throw new IllegalArgumentException("Recording not found: " + recordingId);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the end time", e);
        }
    }

    @Override
    public Recording getRecordingById(Integer id) {
        if (id == null) {
            return null;
        }
        List<Recording> found = queryRecordings(SELECT_RECORDINGS + "WHERE id = ?", id);
        if (found.isEmpty()) {
            return null;
        }
        return found.get(0);
    }

    @Override
    public List<Recording> getRecordingsByPatient(Integer patientId) {
        if (patientId == null) {
            return new ArrayList<>();
        }
        return queryRecordings(SELECT_RECORDINGS + "WHERE patient_id = ? ORDER BY start_time DESC", patientId);
    }

    /** Ejecuta un SELECT de grabaciones con 1 parámetro y convierte cada fila en un Recording. */
    private List<Recording> queryRecordings(String sql, Integer param) {
        List<Recording> recordings = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recordings.add(readRecording(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return recordings;
    }

    /** Convierte la fila actual en un Recording. */
    private Recording readRecording(ResultSet rs) throws SQLException {
        Recording recording = new Recording();
        recording.setId(rs.getInt("id"));
        recording.setPatientId(rs.getInt("patient_id"));
        recording.setStartTime(LocalDateTime.parse(rs.getString("start_time")));

        String endTime = rs.getString("end_time");
        if (endTime != null) {   // null si la grabación no terminó (por ejemplo, se cortó)
            recording.setEndTime(LocalDateTime.parse(endTime));
        }

        recording.setSamplingRate(rs.getInt("sampling_rate"));
        recording.setChannel(rs.getString("channel"));
        recording.setUnits(rs.getString("units"));
        recording.setFilePath(rs.getString("file_path"));
        return recording;
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