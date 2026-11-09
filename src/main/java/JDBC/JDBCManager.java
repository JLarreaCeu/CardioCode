package JDBC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCManager {

        private static final Path DB_PATH = Paths.get("db", "base_datos.db");
        private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

        private static final String CREATE_ROLES = """
            CREATE TABLE IF NOT EXISTS Roles (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL UNIQUE
            )""";

        private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS Users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                email    TEXT NOT NULL UNIQUE CHECK (email LIKE '%@%'),
                password BLOB NOT NULL,
                role_id  INTEGER NOT NULL REFERENCES Roles(id)
            )""";

        private static final String CREATE_PATIENTS = """
            CREATE TABLE IF NOT EXISTS Patients (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                name    TEXT NOT NULL,
                phone   TEXT,
                address TEXT,
                email   TEXT NOT NULL UNIQUE CHECK (email LIKE '%@%')
            )""";

        private static final String CREATE_DOCTORS = """
            CREATE TABLE IF NOT EXISTS Doctors (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                name      TEXT NOT NULL,
                specialty TEXT,
                email     TEXT NOT NULL UNIQUE CHECK (email LIKE '%@%'),
                workplace TEXT,
                phone     TEXT
            )""";

        public JDBCManager() {
            try {
                Files.createDirectories(DB_PATH.getParent());
                createTables();
            } catch (IOException | SQLException e) {
                throw new IllegalStateException("Could not initialize database", e);
            }
        }

        public static Connection getConnection() throws SQLException {
            Connection conn = DriverManager.getConnection(JDBC_URL);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys=ON");
            } catch (SQLException e) {
                conn.close();
                throw e;
            }
            return conn;
        }

        private void createTables() throws SQLException {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate(CREATE_ROLES);
                stmt.executeUpdate(CREATE_USERS);
                stmt.executeUpdate(CREATE_PATIENTS);
                stmt.executeUpdate(CREATE_DOCTORS);
            }
        }}


