package commonInfo;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Datos personales de un paciente.
 * Se enlaza con su cuenta de acceso (User) mediante userId
 * y con su médico (Doctor) mediante doctorId.
 */
public class Patient implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;          // null hasta que la base de datos le asigna uno
    private Integer userId;      // id de su User (email, contraseña y rol)
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String sex;
    private String phone;
    private Integer doctorId;    // id del médico que lo lleva

    public Patient() {
    }

    public Patient(Integer userId, String name, String surname, LocalDate birthDate,
                   String sex, String phone, Integer doctorId) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.sex = sex;
        this.phone = phone;
        this.doctorId = doctorId;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Patient other = (Patient) obj;
        return Objects.equals(id, other.id) && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "Patient " + id + ": " + name + " " + surname;
    }
}