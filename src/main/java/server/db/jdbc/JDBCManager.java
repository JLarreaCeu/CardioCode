package server.db.jdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea la base de datos SQLite y sus tablas, y da conexiones a los managers.
 * Cada operación abre su propia conexión, así varios clientes (hilos)
 * pueden usar la base de datos a la vez sin compartir una conexión.
 */
public class JDBCManager {

    private static final Path DB_PATH = Paths.get("db", "cardiocode.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                email    TEXT NOT NULL UNIQUE CHECK (email LIKE '%@%'),
                password BLOB NOT NULL,
                role     TEXT NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR'))
            )""";

    private static final String CREATE_DOCTORS = """
            CREATE TABLE IF NOT EXISTS doctors (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                name    TEXT NOT NULL,
                surname TEXT NOT NULL,
                phone   TEXT
            )""";

    private static final String CREATE_PATIENTS = """
            CREATE TABLE IF NOT EXISTS patients (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                name       TEXT NOT NULL,
                surname    TEXT NOT NULL,
                birth_date TEXT,
                sex        TEXT,
                phone      TEXT,
                doctor_id  INTEGER REFERENCES doctors(id)
            )""";

    private static final String CREATE_SYMPTOM_REPORTS = """
            CREATE TABLE IF NOT EXISTS symptom_reports (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                patient_id          INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                date_time           TEXT NOT NULL,
                palpitations        INTEGER NOT NULL CHECK (palpitations IN (0, 1)),
                dizziness           INTEGER NOT NULL CHECK (dizziness IN (0, 1)),
                shortness_of_breath INTEGER NOT NULL CHECK (shortness_of_breath IN (0, 1)),
                chest_pain          INTEGER NOT NULL CHECK (chest_pain IN (0, 1)),
                fatigue             INTEGER NOT NULL CHECK (fatigue IN (0, 1)),
                episode_minutes     INTEGER NOT NULL DEFAULT 0 CHECK (episode_minutes >= 0),
                doctor_comment      TEXT,
                doctor_comment_time TEXT
            )""";

    private static final String CREATE_RECORDINGS = """
            CREATE TABLE IF NOT EXISTS recordings (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                patient_id    INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                start_time    TEXT NOT NULL,
                end_time      TEXT,
                sampling_rate INTEGER NOT NULL CHECK (sampling_rate > 0),
                channel       TEXT NOT NULL,
                units         TEXT NOT NULL,
                file_path     TEXT
            )""";

    public JDBCManager() {
        try {
            Files.createDirectories(DB_PATH.getParent());
            createTables();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Could not initialize database", e);
        }
    }

    /**
     * Abre una conexión nueva. Quien la pide la cierra (con try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");   // SQLite no comprueba las claves foráneas si no se activa
            stmt.execute("PRAGMA busy_timeout = 5000"); // si otro hilo está escribiendo, espera hasta 5 s en vez de fallar
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // El orden importa: cada tabla se crea después de las tablas a las que apunta
            stmt.executeUpdate(CREATE_USERS);
            stmt.executeUpdate(CREATE_DOCTORS);
            stmt.executeUpdate(CREATE_PATIENTS);
            stmt.executeUpdate(CREATE_SYMPTOM_REPORTS);
            stmt.executeUpdate(CREATE_RECORDINGS);
        }
    }
}