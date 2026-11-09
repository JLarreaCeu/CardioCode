package commonInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Datos personales de un médico.
 * Se enlaza con su cuenta de acceso (User) mediante userId.
 */
public class Doctor implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;          // null hasta que la base de datos le asigna uno
    private Integer userId;      // id de su User (email, contraseña y rol)
    private String name;
    private String surname;
    private String phone;

    public Doctor() {
    }

    public Doctor(Integer userId, String name, String surname, String phone) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.phone = phone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Doctor other = (Doctor) obj;
        return Objects.equals(id, other.id) && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "Dr. " + name + " " + surname;
    }
}