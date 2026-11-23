package server;

import commonInfo.*;
import server.db.interfaces.DoctorManager;
import server.db.interfaces.PatientManager;
import server.db.interfaces.RecordingManager;
import server.db.interfaces.SymptomManager;
import server.db.interfaces.UserManager;
import server.db.jdbc.JDBCDoctorManager;
import server.db.jdbc.JDBCPatientManager;
import server.db.jdbc.JDBCRecordingManager;
import server.db.jdbc.JDBCSymptomManager;
import server.db.jdbc.JDBCUserManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {

    private Socket socket;
    private SocketConnection connection;
    private  HospitalServer server;
    private UserManager userManager;
    private PatientManager patientManager;
    private SymptomManager symptomManager;
    private DoctorManager doctorManager;
    private RecordingManager recordingManager;
    private User loggedUser;
    private RecordingFileWriter recordingFile;
    private Integer recordingId;
    private static final int SAMPLING_RATE = 1000;
    private static final String CHANNEL = "ECG";
    private static final String UNITS = "ADC";

    public ClientHandler(Socket socket, HospitalServer server) {
        this.socket = socket;
        this.server = server;
        this.userManager = new JDBCUserManager();
        this.patientManager = new JDBCPatientManager();
        this.doctorManager = new JDBCDoctorManager();
        this.symptomManager = new JDBCSymptomManager();
        this.recordingManager = new JDBCRecordingManager();
        this.recordingFile = new RecordingFileWriter();
    }

    public void closeConnection(){
        try{
            socket.close();
        }catch (IOException e){
            System.out.println("Error closing client " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            connection = new SocketConnection(socket);
            System.out.println("Client connected from " + socket.getInetAddress());

            boolean connected = true;
            while(connected) {
                Object received = connection.receiveObject();

                if(!(received instanceof Commands)){
                    sendError("Invalid command");
                } else {
                    Commands command = (Commands) received;

                    if(command == Commands.LOGOUT) {
                        connected = false;
                    } else if(command == Commands.PING) {
                        connection.sendCommand(Commands.OK);
                    } else if(command == Commands.LOGIN) {
                        handleLogin();
                    } else if(command == Commands.REGISTER_PATIENT) {
                        handleRegisterPatient();
                    } else if(command == Commands.GET_DOCTORS) {
                        handleGetDoctors();
                    } else if (command == Commands.SEND_SYMPTOMS) {
                        handleSendSymptoms();
                    } else if (command == Commands.GET_PATIENTS){
                        handleGetPatients();
                    } else if (command == Commands.GET_PATIENT_HISTORY){
                        handleGetPatientHistory();
                    } else if (command == Commands.ADD_NOTE){
                        handleAddNote();
                    } else if (command == Commands.UPDATE_PATIENT){
                        handleUpdatePatient();
                    } else if (command == Commands.START_RECORDING){
                        handleStartRecording();
                    } else if (command == Commands.SIGNAL_DATA){
                        handleSignalData();
                    } else if (command == Commands.STOP_RECORDING){
                        handleStopRecording();
                    } else if (command == Commands.GET_RECORDING_DATA){
                        handleGetRecordingData();
                    } else {
                        System.out.println("Received: " + command);
                        sendError("not avlabl ");
                    }
                }
            }
            System.out.println("Client logged out");

        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error with client: " + e.getMessage());
        } finally {
            finishRecording();
            server.removeClient(this);
            if (connection != null) {
                connection.close();
            } else {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Envia ERROR + motivo asi el cliente sabe que ha fallado
     * @param message
     * @throws IOException
     */
    private void sendError(String message) throws IOException {
        connection.sendCommand(Commands.ERROR);
        connection.sendObject(message);
    }

    /**
     * LOGIN recibe email y contraseña. Responde Ok + User o ERROR + motivo
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleLogin() throws IOException, ClassNotFoundException {
        Object emailData = connection.receiveObject();
        Object passwordData = connection.receiveObject();

        if (loggedUser != null) {
            sendError("Already logged in");
        } else if (!(emailData instanceof String) || !(passwordData instanceof String)) {
            sendError("Invalid login data");
        } else {
            String email = (String) emailData;
            String password = (String) passwordData;
            User user = userManager.checkPassword(email, password);

            if (user == null) {

                sendError ("Wrong email or password");
            } else {
                loggedUser = user;
                connection.sendCommand(Commands.OK);
                connection.sendObject(user);
                System.out.println(user + " logged in");
            }
        }
    }

    /**
     * SEND_SYMPTOMS recibe un SymptomReport y lo guarda. Responde OK o ERROR .
     */
    private void handleSendSymptoms() throws IOException, ClassNotFoundException {
        Object data = connection.receiveObject();

        if (!hasRole(Role.PATIENT)) {
            return;
        }
        if (!(data instanceof SymptomReport)) {
            sendError("Invalid symptom report");
            return;
        }

        Patient patient = patientManager.getPatientByUserId(loggedUser.getId());
        if (patient == null) {
            sendError("There is no patient profile for this account");
            return;
        }

        SymptomReport report = (SymptomReport) data;
        report.setPatientId(patient.getId());
        try {
            symptomManager.addReport(report);
            connection.sendCommand(Commands.OK);
            System.out.println("Symptom report " + report.getId() + " saved for patient " + patient.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Could not save symptom report: " + e.getMessage());
            sendError("Could not save the symptom report");
        }
    }

    private boolean hasRole(Role role) throws IOException {
        if(loggedUser == null) {
            sendError("You must log in first");
            return false;
        }
        if(loggedUser.getRole() != role) {
            sendError("This action is not allowed for a " + loggedUser.getRole());
            return false;
        }
        return true;
    }

    /**
     * REGISTER_PATIENT recibe email y contraseña y Patient. Responde OK + Patient guardado o ERROR + motivo
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleRegisterPatient() throws IOException, ClassNotFoundException {
        Object emailData = connection.receiveObject();
        Object passwordData = connection.receiveObject();
        Object patientData = connection.receiveObject();

        if (loggedUser != null) {
            sendError("Log out before creating a new account");
        } else if (!(emailData instanceof String) || !(passwordData instanceof String) || !(patientData instanceof Patient)) {
            sendError("Invalid register data");
        } else {
            Patient patient = (Patient) patientData;
            if (((String) emailData).trim().toLowerCase().endsWith("@doctor.com")) {
                sendError("This email is reserved for doctors");
                return;
            }
            if (patient.getDoctorId() != null && doctorManager.getDoctorById(patient.getDoctorId()) == null) {
                sendError("The chosen doctor does not exist");
                return;
            }

            User user = new User((String) emailData, (String) passwordData, Role.PATIENT);
            try {
                userManager.newUser(user);
            } catch (IllegalArgumentException | IllegalStateException e) {
                sendError(e.getMessage());
                return;
            }

            try {
                patient.setUserId(user.getId());
                patientManager.addPatient(patient);
            } catch (IllegalArgumentException | IllegalStateException e) {
                userManager.deleteUser(user);
                sendError(e.getMessage());
                return;
            }
            connection.sendCommand(Commands.OK);
            connection.sendObject(patient);
            System.out.println("New patient registered: " + user.getEmail());
        }
    }

    /**
     * GET_DOCTORS no recibe nada. Responde OK + lista de médicos o ERROR + motivo
     * @throws IOException
     */
    private void handleGetDoctors() throws IOException {
        try {
            ArrayList<Doctor> doctors = new ArrayList<>(doctorManager.getDoctors());
            connection.sendCommand(Commands.OK);
            connection.sendObject(doctors);
        } catch(IllegalStateException e) {
            sendError(e.getMessage());
        }
    }

    /**
     * GET_PATIENTS no recibe nada. Responde OK + lista de pacientes del medico o ERROR + motivo
     * @throws IOException
     */
    private void handleGetPatients() throws IOException {
        if (!hasRole(Role.DOCTOR)) {
            return;
        }
        Doctor doctor = doctorManager.getDoctorByUserId(loggedUser.getId());
        if (doctor == null) {
            sendError("There is no doctor profile for this account");
            return;
        }
        try {
            ArrayList<Patient> patients = new ArrayList<>(patientManager.getPatientsByDoctor(doctor.getId()));
            connection.sendCommand(Commands.OK);
            connection.sendObject(patients);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(e.getMessage());
        }
    }

    /**
     * GET_PATIENT_HISTORY recibe el id del paciente (Integer). Responde OK + PatientHistory o ERROR + motivo.
     * El médico solo puede ver a sus pacientes. El paciente siempre recibe su propio historial (el id que mande da igual)
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleGetPatientHistory() throws IOException, ClassNotFoundException {
        Object idData = connection.receiveObject();

        if (loggedUser == null) {
            sendError("You must log in first");
            return;
        }

        Patient patient;
        if (loggedUser.getRole() == Role.DOCTOR) {
            if (!(idData instanceof Integer)) {
                sendError("Invalid patient id");
                return;
            }
            patient = patientManager.getPatientById((Integer) idData);
            if (patient == null) {
                sendError("Patient not found");
                return;
            }
            Doctor doctor = doctorManager.getDoctorByUserId(loggedUser.getId());
            if (doctor == null || !doctor.getId().equals(patient.getDoctorId())) {
                sendError("This patient is not yours");
                return;
            }
        } else {
            patient = patientManager.getPatientByUserId(loggedUser.getId()); //el paciente solo puede ver el suyo
            if (patient == null) {
                sendError("There is no patient profile for this account");
                return;
            }
        }

        try {
            ArrayList<SymptomReport> reports = new ArrayList<>(symptomManager.getReportsByPatient(patient.getId()));
            ArrayList<Recording> recordings = new ArrayList<>(recordingManager.getRecordingsByPatient(patient.getId()));
            PatientHistory history = new PatientHistory(patient, reports, recordings);
            connection.sendCommand(Commands.OK);
            connection.sendObject(history);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(e.getMessage());
        }
    }

    /**
     * ADD_NOTE recibe el id del informe (Integer) y el comentario del medico (String). Responde OK o ERROR + motivo.
     * El medico solo puede comentar informes de sus pacientes
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleAddNote() throws IOException, ClassNotFoundException {
        Object reportIdData = connection.receiveObject();
        Object commentData = connection.receiveObject();

        if (!hasRole(Role.DOCTOR)) {
            return;
        }
        if (!(reportIdData instanceof Integer) || !(commentData instanceof String)) {
            sendError("Invalid note data");
            return;
        }

        Doctor doctor = doctorManager.getDoctorByUserId(loggedUser.getId());
        if (doctor == null) {
            sendError("There is no doctor profile for this account");
            return;
        }

        Integer reportId = (Integer) reportIdData;
        if (!isReportOfDoctor(reportId, doctor)) {
            sendError("This report is not from one of your patients");
            return;
        }

        try {
            symptomManager.addDoctorComment(reportId, (String) commentData);
            connection.sendCommand(Commands.OK);
            System.out.println("Doctor " + doctor.getId() + " commented report " + reportId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(e.getMessage());
        }
    }

    /**
     * Comprueba si un informe es de alguno de los pacientes del medico
     * @param reportId id del informe
     * @param doctor medico que lo quiere comentar
     * @return true si el informe es de uno de sus pacientes
     */
    private boolean isReportOfDoctor(Integer reportId, Doctor doctor) {
        for (Patient patient : patientManager.getPatientsByDoctor(doctor.getId())) {
            for (SymptomReport report : symptomManager.getReportsByPatient(patient.getId())) {
                if (report.getId().equals(reportId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * UPDATE_PATIENT recibe el Patient con los datos corregidos. Responde OK + Patient actualizado o ERROR + motivo.
     * El medico solo puede editar a sus pacientes, y no puede cambiarles la cuenta ni el medico
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void handleUpdatePatient() throws IOException, ClassNotFoundException {
        Object patientData = connection.receiveObject();

        if (!hasRole(Role.DOCTOR)) {
            return;
        }
        if (!(patientData instanceof Patient)) {
            sendError("Invalid patient data");
            return;
        }

        Patient edited = (Patient) patientData;
        if (edited.getId() == null) {
            sendError("Patient id is required");
            return;
        }

        Patient saved = patientManager.getPatientById(edited.getId());
        Doctor doctor = doctorManager.getDoctorByUserId(loggedUser.getId());
        if (saved == null || doctor == null || !doctor.getId().equals(saved.getDoctorId())) {
            sendError("This patient is not yours");
            return;
        }

        edited.setUserId(saved.getUserId());
        edited.setDoctorId(saved.getDoctorId());
        try {
            patientManager.updatePatient(edited);
            connection.sendCommand(Commands.OK);
            connection.sendObject(edited);
            System.out.println("Patient " + edited.getId() + " updated by doctor " + doctor.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(e.getMessage());
        }
    }

    private void handleStartRecording() throws IOException {
        if (!hasRole(Role.PATIENT)) {
            return;
        }
        if (recordingFile.isOpen()) {
            sendError("There is already a recording in progress");
            return;
        }
        Patient patient = patientManager.getPatientByUserId(loggedUser.getId());
        if (patient == null) {
            sendError("There is no patient profile for this account");
            return;
        }

        Recording recording = new Recording(patient.getId(), LocalDateTime.now(), SAMPLING_RATE, CHANNEL, UNITS);
        try {
            recording.setFilePath(recordingFile.open(patient.getId()));
            recordingManager.addRecording(recording);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            recordingFile.close();
            System.out.println("Could not start recording: " + e.getMessage());
            sendError("Could not start the recording");
            return;
        }
        recordingId = recording.getId();
        connection.sendCommand(Commands.OK);
        System.out.println("Recording " + recordingId + " started for patient " + patient.getId());
    }

    private void handleSignalData() throws IOException, ClassNotFoundException {
        Object data = connection.receiveObject();

        if (!hasRole(Role.PATIENT)) {
            return;
        }
        if (!recordingFile.isOpen()) {
            sendError("There is no recording in progress");
            return;
        }
        if (!(data instanceof int[])) {
            sendError("Invalid signal data");
            return;
        }
        try {
            recordingFile.writeBlock((int[]) data);
            connection.sendCommand(Commands.OK);
        } catch (IOException e) {
            System.out.println("Could not write signal data: " + e.getMessage());
            sendError("Could not save the signal data");
        }
    }

    private void handleStopRecording() throws IOException {
        if (!hasRole(Role.PATIENT)) {
            return;
        }
        if (!recordingFile.isOpen()) {
            sendError("There is no recording in progress");
            return;
        }
        finishRecording();
        connection.sendCommand(Commands.OK);
    }

    private void finishRecording() {
        if (!recordingFile.isOpen()) {
            return;
        }
        try {
            recordingFile.close();
        } catch (IOException e) {
            System.out.println("Could not close recording file: " + e.getMessage());
        }
        try {
            recordingManager.setEndTime(recordingId, LocalDateTime.now());
            System.out.println("Recording " + recordingId + " finished");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Could not save end time: " + e.getMessage());
        }
        recordingId = null;
    }

    private void handleGetRecordingData() throws IOException, ClassNotFoundException {
        Object idData = connection.receiveObject();

        if (!hasRole(Role.DOCTOR)) {
            return;
        }
        if (!(idData instanceof Integer)) {
            sendError("Invalid recording id");
            return;
        }

        Recording recording = recordingManager.getRecordingById((Integer) idData);
        if (recording == null) {
            sendError("Recording not found");
            return;
        }
        Patient patient = patientManager.getPatientById(recording.getPatientId());
        Doctor doctor = doctorManager.getDoctorByUserId(loggedUser.getId());
        if (patient == null || doctor == null || !doctor.getId().equals(patient.getDoctorId())) {
            sendError("This recording is not from one of your patients");
            return;
        }

        try {
            int[] samples = RecordingFileWriter.readAll(recording.getFilePath());
            connection.sendCommand(Commands.OK);
            connection.sendObject(samples);
        } catch (IOException e) {
            System.out.println("Could not read recording " + recording.getId() + ": " + e.getMessage());
            sendError("Could not read the recording file");
        }
    }
}
