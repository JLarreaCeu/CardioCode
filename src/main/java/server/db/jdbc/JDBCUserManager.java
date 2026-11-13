package server.db.jdbc;

import commonInfo.Role;
import commonInfo.User;
import server.PasswordHasher;
import server.db.interfaces.UserManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuentas de acceso guardadas en la tabla users.
 * El rol se guarda como texto ("PATIENT" o "DOCTOR") y la contraseña
 * solo como hash (PasswordHasher). Los User devueltos nunca llevan contraseña.
 */
public class JDBCUserManager implements UserManager {

    // Sin la columna password: los User que se devuelven nunca llevan la contraseña
    private static final String SELECT_USERS = "SELECT id, email, role FROM users ";

    @Override
    public void newUser(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        String email = normalize(user.getEmail());
        if (getUserByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        String sql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setBytes(2, PasswordHasher.hash(user.getPassword()));
            ps.setString(3, user.getRole().name());   // guarda "PATIENT" o "DOCTOR"
            ps.executeUpdate();
            user.setId(lastInsertedId(c));
            user.setEmail(email);
            user.setPassword(null);                    // la contraseña ya no hace falta
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create user " + email, e);
        }
    }

    @Override
    public List<User> getUsers() {
        return queryUsers(SELECT_USERS, null);
    }

    @Override
    public User checkPassword(String email, String pass) {
        if (email == null || pass == null) {
            return null;
        }
        String sql = "SELECT id, email, password, role FROM users WHERE email = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalize(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] storedHash = rs.getBytes("password");
                    if (PasswordHasher.verify(pass, storedHash)) {
                        return readUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void changePassword(User user, String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, PasswordHasher.hash(newPassword));
            ps.setInt(2, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change the password", e);
        }
    }

    @Override
    public String getUserMailFromId(Integer id) {
        User user = getUserById(id);
        if (user == null) {
            return null;
        }
        return user.getEmail();
    }

    @Override
    public User getUserById(Integer id) {
        if (id == null) {
            return null;
        }
        return firstOrNull(queryUsers(SELECT_USERS + "WHERE id = ?", id));
    }

    @Override
    public void deleteUserById(int id) {
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete user " + id, e);
        }
    }

    @Override
    public void deleteUser(User user) {
        if (user != null && user.getId() != null) {
            deleteUserById(user.getId());
        }
    }

    @Override
    public User getUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        return firstOrNull(queryUsers(SELECT_USERS + "WHERE email = ?", normalize(email)));
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        if (role == null) {
            return new ArrayList<>();
        }
        return queryUsers(SELECT_USERS + "WHERE role = ?", role.name());
    }

    /** Ejecuta un SELECT de usuarios con 0 o 1 parámetro y convierte cada fila en un User. */
    private List<User> queryUsers(String sql, Object param) {
        List<User> users = new ArrayList<>();
        try (Connection c = JDBCManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(readUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /** Convierte la fila actual en un User, sin contraseña. */
    private User readUser(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));   // convierte "PATIENT" en Role.PATIENT
        User user = new User(rs.getString("email"), null, role);
        user.setId(rs.getInt("id"));
        return user;
    }

    private User firstOrNull(List<User> users) {
        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
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

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}