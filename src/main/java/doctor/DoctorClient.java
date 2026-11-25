package doctor;

import commonInfo.Commands;
import commonInfo.Doctor;
import commonInfo.Patient;
import commonInfo.PatientHistory;
import commonInfo.Pings;
import commonInfo.SocketConnection;
import commonInfo.User;

import java.io.IOException;
import java.net.Socket;
import java.util.List;


public class DoctorClient implements Pings {

    private static final int PORT = 6767;
    private final SocketConnection con;
    private final Socket socket;
    private User loggedUser;

    /** Se conecta al servidor.*/
    public DoctorClient(String host) throws IOException {
        socket = new Socket(host, PORT);
        con = new SocketConnection(socket);
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    /**
     * Error que manda el servidor
     */
    public static class ServerException extends Exception {
        public ServerException(String message) {
            super(message);
        }
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    /** LOGIN:¡ OK + User*/
    public synchronized User login(String email, String password) throws IOException, ServerException {
        con.sendCommand(Commands.LOGIN);
        con.sendObject(email);
        con.sendObject(password);
        expectConfirmation();
        loggedUser = (User) receive();
        return loggedUser;
    }

    /** GET_DOCTORS */
    @SuppressWarnings("unchecked")
    public synchronized List<Doctor> getDoctors() throws IOException, ServerException {
        con.sendCommand(Commands.GET_DOCTORS);
        expectConfirmation();
        return (List<Doctor>) receive();
    }

    /** GET_PATIENTS */
    @SuppressWarnings("unchecked")
    public synchronized List<Patient> getPatients() throws IOException, ServerException {
        con.sendCommand(Commands.GET_PATIENTS);
        expectConfirmation();
        return (List<Patient>) receive();
    }

    /** GET_PATIENT_HISTORY*/
    public synchronized PatientHistory getPatientHistory(Integer patientId) throws IOException, ServerException {
        con.sendCommand(Commands.GET_PATIENT_HISTORY);
        con.sendObject(patientId);
        expectConfirmation();
        return (PatientHistory) receive();
    }

    /** ADD_NOTE*/
    public synchronized void addNote(Integer reportId, String comment) throws IOException, ServerException {
        con.sendCommand(Commands.ADD_NOTE);
        con.sendObject(reportId);
        con.sendObject(comment);
        expectConfirmation();
    }

    /** UPDATE_PATIENT */
    public synchronized Patient updatePatient(Patient patient) throws IOException, ServerException {
        con.sendCommand(Commands.UPDATE_PATIENT);
        con.sendObject(patient);
        expectConfirmation();
        return (Patient) receive();
    }

    /** GET_RECORDING_DATA valores del BITalino 0-1023 */
    public synchronized int[] getRecordingData(Integer recordingId) throws IOException, ServerException {
        con.sendCommand(Commands.GET_RECORDING_DATA);
        con.sendObject(recordingId);
        expectConfirmation();
        return (int[]) receive();
    }

    public synchronized void logout() throws IOException {
        con.sendCommand(Commands.LOGOUT);
        con.close();
    }

    @Override
    public synchronized void ping() throws IOException {
        try {
            con.sendCommand(Commands.PING);
            Commands answer = con.receiveCommand();
            if(answer != Commands.OK){
                throw new IOException("Unexpected answer to PING: " + answer);
            }
        } catch (ClassNotFoundException e) {
            con.close();
            throw new IOException ("Invalid answer from server");
        } catch(IOException e) {
            con.close();
            throw e;
        }
    }

    /** Lee OK, o ERROR . */
    private void expectConfirmation() throws IOException, ServerException {
        Object answer = receive();
        if (answer == Commands.OK) {
            return;
        }
        if (answer == Commands.ERROR) {
            throw new ServerException(String.valueOf(receive()));
        }
        throw new IOException("Unexpected answer from server: " + answer);
    }

    private Object receive() throws IOException {
        try {
            return con.receiveObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Invalid answer from server", e);
        }
    }
}
