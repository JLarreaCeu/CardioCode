package server;

import commonInfo.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;


public class SocketTest {

    private static final String HOST = Config.getServerHost();
    private static final int PORT = Config.getServerPort();

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); //no se cierra, cerraria System.in
            System.out.println("Email of an existing doctor (created from the admin console):");
            String doctorEmail = reader.readLine();
            System.out.println("Password:");
            String doctorPassword = reader.readLine();
            if (doctorEmail == null || doctorPassword == null) {
                System.out.println("No input");
                return;
            }

            //el paciente lo crea el propio test, con un email nuevo cada vez
            String patientEmail = "test" + System.currentTimeMillis() + "@test.com";
            String patientPassword = "testpassword";

            SocketConnection patientCon = new SocketConnection(new Socket(HOST, PORT));
            SocketConnection doctorCon = new SocketConnection(new Socket(HOST, PORT));
            System.out.println("Connected to " + HOST + ":" + PORT);
            System.out.println();

            // 1. PING
            patientCon.sendCommand(Commands.PING);
            check("PING answers OK", patientCon.receiveCommand() == Commands.OK);

            // 2. Comando de medico sin login
            patientCon.sendCommand(Commands.GET_PATIENTS);
            check("GET_PATIENTS without login answers ERROR", readError(patientCon));

            // 3. Algo que no es un comando
            patientCon.sendObject("this is not a command");
            check("Something that is not a command answers ERROR", readError(patientCon));

            patientCon.sendCommand(Commands.PING);
            check("The server is still alive after that", patientCon.receiveCommand() == Commands.OK);

            // 4. LOGIN del medico
            doctorCon.sendCommand(Commands.LOGIN);
            doctorCon.sendObject(doctorEmail.trim());
            doctorCon.sendObject(doctorPassword);
            Commands answer = doctorCon.receiveCommand();
            Object data = doctorCon.receiveObject();
            if (answer != Commands.OK) {
                check("Doctor LOGIN answers OK (server said: " + data + ")", false);
                System.out.println("Without a doctor the rest of the test can't run");
                return;
            }
            User doctorUser = (User) data;
            check("Doctor LOGIN answers OK + User with role DOCTOR", doctorUser.getRole() == Role.DOCTOR);

            // 5. GET_DOCTORS (sin login, lo usa la pantalla de registro)
            patientCon.sendCommand(Commands.GET_DOCTORS);
            Integer doctorId = null;
            if (patientCon.receiveCommand() == Commands.OK) {
                ArrayList<Doctor> doctors = (ArrayList<Doctor>) patientCon.receiveObject();
                for (Doctor doctor : doctors) {
                    if (doctor.getUserId().equals(doctorUser.getId())) {
                        doctorId = doctor.getId();
                    }
                }
                check("GET_DOCTORS answers OK + list with our doctor", doctorId != null);
            } else {
                patientCon.receiveObject();
                check("GET_DOCTORS answers OK + list with our doctor", false);
            }

            // 6. REGISTER_PATIENT con email de medico
            Patient fakePatient = new Patient(null, "Fake", "Doctor", LocalDate.of(2000, 1, 1), "M", "600000000", doctorId);
            patientCon.sendCommand(Commands.REGISTER_PATIENT);
            patientCon.sendObject("fake@doctor.com");
            patientCon.sendObject(patientPassword);
            patientCon.sendObject(fakePatient);
            check("REGISTER_PATIENT with a @doctor.com email answers ERROR", readError(patientCon));

            // 7. REGISTER_PATIENT bueno
            Patient newPatient = new Patient(null, "Test", "Patient", LocalDate.of(2000, 1, 1), "F", "600000000", doctorId);
            patientCon.sendCommand(Commands.REGISTER_PATIENT);
            patientCon.sendObject(patientEmail);
            patientCon.sendObject(patientPassword);
            patientCon.sendObject(newPatient);
            answer = patientCon.receiveCommand();
            data = patientCon.receiveObject();
            Integer patientId = null;
            if (answer == Commands.OK) {
                patientId = ((Patient) data).getId();
                check("REGISTER_PATIENT answers OK + Patient with id", patientId != null);
            } else {
                check("REGISTER_PATIENT answers OK + Patient (server said: " + data + ")", false);
                System.out.println("Without a patient the rest of the test can't run");
                return;
            }

            // 8. LOGIN con contraseña mala
            patientCon.sendCommand(Commands.LOGIN);
            patientCon.sendObject(patientEmail);
            patientCon.sendObject("wrong password");
            check("LOGIN with wrong password answers ERROR", readError(patientCon));

            // 9. LOGIN bueno
            patientCon.sendCommand(Commands.LOGIN);
            patientCon.sendObject(patientEmail);
            patientCon.sendObject(patientPassword);
            answer = patientCon.receiveCommand();
            data = patientCon.receiveObject();
            if (answer == Commands.OK) {
                User user = (User) data;
                check("LOGIN with right password answers OK + User", user.getEmail() != null);
                check("The User does not carry the password", user.getPassword() == null);
            } else {
                check("LOGIN with right password answers OK + User (server said: " + data + ")", false);
            }

            // 10. LOGIN otra vez
            patientCon.sendCommand(Commands.LOGIN);
            patientCon.sendObject(patientEmail);
            patientCon.sendObject(patientPassword);
            check("Second LOGIN answers ERROR", readError(patientCon));

            // 11. Comando de medico siendo paciente
            patientCon.sendCommand(Commands.GET_PATIENTS);
            check("GET_PATIENTS as a patient answers ERROR", readError(patientCon));

            // 12. SEND_SYMPTOMS
            SymptomReport report = new SymptomReport(null, LocalDateTime.now(), true, false, false, false, true, 10, "Test report");
            patientCon.sendCommand(Commands.SEND_SYMPTOMS);
            patientCon.sendObject(report);
            check("SEND_SYMPTOMS answers OK", readOk(patientCon));

            // 13. El paciente pide su historial (manda null)
            patientCon.sendCommand(Commands.GET_PATIENT_HISTORY);
            patientCon.sendObject(null);
            PatientHistory history = readHistory(patientCon);
            check("GET_PATIENT_HISTORY as patient answers OK + 1 report", history != null && history.getReports().size() == 1);

            // 14. ADD_NOTE siendo paciente
            patientCon.sendCommand(Commands.ADD_NOTE);
            patientCon.sendObject(1);
            patientCon.sendObject("I am not a doctor");
            check("ADD_NOTE as a patient answers ERROR", readError(patientCon));

            // 15. El medico pide sus pacientes
            doctorCon.sendCommand(Commands.GET_PATIENTS);
            boolean found = false;
            if (doctorCon.receiveCommand() == Commands.OK) {
                ArrayList<Patient> patients = (ArrayList<Patient>) doctorCon.receiveObject();
                for (Patient patient : patients) {
                    if (patient.getId().equals(patientId)) {
                        found = true;
                    }
                }
            } else {
                doctorCon.receiveObject();
            }
            check("GET_PATIENTS as doctor answers OK + list with the new patient", found);

            // 16. El medico pide el historial del paciente
            doctorCon.sendCommand(Commands.GET_PATIENT_HISTORY);
            doctorCon.sendObject(patientId);
            history = readHistory(doctorCon);
            Integer reportId = null;
            if (history != null && history.getReports().size() == 1) {
                reportId = history.getReports().get(0).getId();
            }
            check("GET_PATIENT_HISTORY as doctor answers OK + the report", reportId != null);

            // 17. Historial de un paciente que no existe o no es suyo
            doctorCon.sendCommand(Commands.GET_PATIENT_HISTORY);
            doctorCon.sendObject(999999);
            check("GET_PATIENT_HISTORY of a patient that is not his answers ERROR", readError(doctorCon));

            // 18. ADD_NOTE bueno
            doctorCon.sendCommand(Commands.ADD_NOTE);
            doctorCon.sendObject(reportId);
            doctorCon.sendObject("Test comment from the doctor");
            check("ADD_NOTE as doctor answers OK", readOk(doctorCon));

            // 19. ADD_NOTE en un informe que no es de sus pacientes
            doctorCon.sendCommand(Commands.ADD_NOTE);
            doctorCon.sendObject(999999);
            doctorCon.sendObject("This should fail");
            check("ADD_NOTE on a report that is not his answers ERROR", readError(doctorCon));

            // 20. El paciente ve el comentario del medico
            patientCon.sendCommand(Commands.GET_PATIENT_HISTORY);
            patientCon.sendObject(null);
            history = readHistory(patientCon);
            boolean hasComment = false;
            if (history != null && history.getReports().size() == 1) {
                hasComment = "Test comment from the doctor".equals(history.getReports().get(0).getDoctorComment());
            }
            check("The patient sees the doctor's comment", hasComment);

            // 21. UPDATE_PATIENT bueno
            Patient edited = new Patient(null, "Edited", "Patient", LocalDate.of(2000, 1, 1), "F", "611111111", doctorId);
            edited.setId(patientId);
            doctorCon.sendCommand(Commands.UPDATE_PATIENT);
            doctorCon.sendObject(edited);
            answer = doctorCon.receiveCommand();
            data = doctorCon.receiveObject();
            if (answer == Commands.OK) {
                check("UPDATE_PATIENT answers OK + Patient with the new name", "Edited".equals(((Patient) data).getName()));
            } else {
                check("UPDATE_PATIENT answers OK + Patient (server said: " + data + ")", false);
            }

            // 22. UPDATE_PATIENT de un paciente que no es suyo
            Patient notHis = new Patient(null, "Not", "His", LocalDate.of(2000, 1, 1), "M", "600000000", doctorId);
            notHis.setId(999999);
            doctorCon.sendCommand(Commands.UPDATE_PATIENT);
            doctorCon.sendObject(notHis);
            check("UPDATE_PATIENT of a patient that is not his answers ERROR", readError(doctorCon));

            // 23. LOGOUT
            patientCon.sendCommand(Commands.LOGOUT);
            patientCon.close();
            doctorCon.sendCommand(Commands.LOGOUT);
            doctorCon.close();

            System.out.println();
            System.out.println("Passed: " + passed + "  Failed: " + failed);
            System.out.println("Test patient created: " + patientEmail);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not talk to the server, is it running? " + e.getMessage());
        }
    }

    /**
     * Lee la respuesta y comprueba que es ERROR + motivo
     * @return true si el servidor respondio ERROR
     */
    private static boolean readError(SocketConnection connection) throws IOException, ClassNotFoundException {
        Commands answer = connection.receiveCommand();
        if (answer != Commands.ERROR) {
            return false;
        }
        Object reason = connection.receiveObject(); //despues de ERROR siempre va el motivo
        System.out.println("   server said: " + reason);
        return true;
    }

    /**
     * Lee la respuesta y comprueba que es OK. Si es ERROR tambien lee el motivo para no desincronizar
     * @return true si el servidor respondio OK
     */
    private static boolean readOk(SocketConnection connection) throws IOException, ClassNotFoundException {
        Commands answer = connection.receiveCommand();
        if (answer == Commands.OK) {
            return true;
        }
        if (answer == Commands.ERROR) {
            System.out.println("   server said: " + connection.receiveObject());
        }
        return false;
    }

    /**
     * Lee la respuesta a GET_PATIENT_HISTORY
     * @return el PatientHistory, o null si el servidor respondio ERROR
     */
    private static PatientHistory readHistory(SocketConnection connection) throws IOException, ClassNotFoundException {
        Commands answer = connection.receiveCommand();
        Object data = connection.receiveObject();
        if (answer == Commands.OK) {
            return (PatientHistory) data;
        }
        System.out.println("   server said: " + data);
        return null;
    }

    /**
     * Imprime si la prueba ha pasado o no y lleva la cuenta
     */
    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }
}
