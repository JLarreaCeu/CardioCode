package commonInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Cuenta de acceso: email + contraseña + tipo de cuenta.
 * La contraseña viaja del cliente al servidor solo al hacer login o registrarse.
 * El servidor guarda únicamente su hash (PasswordHasher) y nunca la devuelve:
 * antes de enviar un User al cliente, pone password a null.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;        // null hasta que la base de datos le asigna uno
    private String email;
    private String password;   // contraseña escrita por el usuario, solo de cliente a servidor
    private Role role;

    public User() {
    }

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        return Objects.equals(id, other.id) && Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        // Nunca imprimimos la contraseña
        return "User " + email + " (" + role + ")";
    }
}