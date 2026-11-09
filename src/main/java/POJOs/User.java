package POJOs;
import java.io.Serializable;
import java.util.Objects;

/**
 * Cuenta de acceso: email + contraseña (guardada como hash) + tipo de cuenta.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String email;
    private byte[] password;   // el HASH de la contraseña, nunca la contraseña en texto
    private Role role;

    public User() {
    }

    public User(String email, byte[] password, Role role) {
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

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
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
        // Nunca imprimimos la contraseña, ni siquiera el hash
        return "User " + email + " (" + role + ")";
    }
}
