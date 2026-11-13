package server.db.jdbc;

import server.db.interfaces.UserManager;
import commonInfo.Role;
import commonInfo.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBCUserManager implements UserManager {

    public static final String ROLE_PATIENT = "patient";
    public static final String ROLE_DOCTOR = "doctor";

    private static final String SELECT_USERS =
            "SELECT u.id, u.email, u.password, r.id AS role_id, r.description "
                    + "FROM Users u JOIN Roles r ON u.role_id = r.id ";

    @Override
    public void connect() {
        new JDBCManager(); // crea db/base_datos.db y las tablas si no existen
        String sql = "INSERT OR IGNORE INTO Roles (description) VALUES (?)"; // OR IGNORE: si ya existe, no hace nada
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (String description : new String[]{ROLE_PATIENT, ROLE_DOCTOR}) {
                ps.setString(1, description);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create the default roles", e);
        }
    }


    public static byte[] hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public User checkPassword(String email, String pass) {
        if (email == null || pass == null) return null;
        User user = getUserByEmail(email);
        if (user == null) return null;
        boolean ok = MessageDigest.isEqual(hashPassword(pass), user.getPassword());
        return ok ? user : null;
    }

    @Override
    public void changePassword(User user, String new_passwd) {
        byte[] hash = hashPassword(new_passwd);
        String sql = "UPDATE Users SET password = ? WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, hash);
            ps.setInt(2, user.getId());
            ps.executeUpdate();
            user.setPassword(hash);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change the password", e);
        }
    }


    @Override
    public void newRole(Role role) {
        String sql = "INSERT INTO Roles (description) VALUES (?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.getDescription());
            ps.executeUpdate();
            role.setId(lastInsertedId(c));
        } catch (SQLException e) {
            throw new IllegalArgumentException("Could not create role '" + role.getDescription() + "'", e);
        }
    }

    @Override
    public List<Role> getRoles() {
        List<Role> roles = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, description FROM Roles")) {
            while (rs.next()) {
                roles.add(readRole(rs.getInt("id"), rs.getString("description")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    @Override
    public Role getRole(Integer id) {
        String sql = "SELECT id, description FROM Roles WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return readRole(rs.getInt("id"), rs.getString("description"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Extra (no está en la interfaz): buscar un rol por nombre, p. ej. "patient". */
    public Role getRoleByDescription(String description) {
        String sql = "SELECT id, description FROM Roles WHERE description = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return readRole(rs.getInt("id"), rs.getString("description"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void newUser(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@"))
            throw new IllegalArgumentException("Invalid email");
        if (user.getPassword() == null)
            throw new IllegalArgumentException("Password is required");
        if (user.getRole() == null || user.getRole().getId() == null)
            throw new IllegalArgumentException("The user needs a role saved in the database");

        String email = normalize(user.getEmail());
        if (getUserByEmail(email) != null)
            throw new IllegalArgumentException("Email already registered: " + email);

        String sql = "INSERT INTO Users (email, password, role_id) VALUES (?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setBytes(2, user.getPassword());
            ps.setInt(3, user.getRole().getId());
            ps.executeUpdate();
            user.setId(lastInsertedId(c));
            user.setEmail(email);
        } catch (SQLException e) {
            throw new IllegalArgumentException("Could not create user " + email, e);
        }
    }

    @Override
    public List<User> getUsers() {
        return queryUsers(SELECT_USERS, null);
    }

    @Override
    public User getUserByEmail(String email) {
        if (email == null) return null;
        List<User> found = queryUsers(SELECT_USERS + "WHERE u.email = ?", normalize(email));
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public User getUserById(Integer id) {
        if (id == null) return null;
        List<User> found = queryUsers(SELECT_USERS + "WHERE u.id = ?", id);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public String getUserMailFromId(Integer id) {
        User user = getUserById(id);
        return user == null ? null : user.getEmail();
    }

    @Override
    public List<User> getListOfUsersByRoleId(Integer role_id) {
        return queryUsers(SELECT_USERS + "WHERE r.id = ?", role_id);
    }

    @Override
    public void deleteUserById(int id) {
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Users WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete user " + id, e);
        }
    }

    @Override
    public void deleteUser(User user) {
        if (user != null && user.getId() != null) deleteUserById(user.getId());
    }


    /** Ejecuta un SELECT de usuarios con 0 o 1 parámetro y convierte cada fila en un User. */
    private List<User> queryUsers(String sql, Object param) {
        List<User> users = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User(rs.getString("email"), rs.getBytes("password"),
                            readRole(rs.getInt("role_id"), rs.getString("description")));
                    user.setId(rs.getInt("id"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private Role readRole(int id, String description) {
        Role role = new Role(description);
        role.setId(id);
        return role;
    }

    private int lastInsertedId(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
