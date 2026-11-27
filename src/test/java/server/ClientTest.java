package server;

import commonInfo.*;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;

public class ClientTest {

    private final SocketConnection connection;

    public ClientTest() throws IOException {
        connection = new SocketConnection(new Socket(Config.getServerHost(), Config.getServerPort()));
    }

    public static ClientTest newPatient(String surname, Integer doctorId) throws IOException, ClassNotFoundException {
        String email = surname.toLowerCase() + System.nanoTime() + "@test.com";
        ClientTest client = new ClientTest();
        client.connection.sendCommand(Commands.REGISTER_PATIENT);
        client.connection.sendObject(email);
        client.connection.sendObject("testpassword");
        client.connection.sendObject(new Patient(null, "Synthetic", surname, LocalDate.of(2000, 1, 1), "F", "600000000", doctorId));
        client.answer();
        client.login(email, "testpassword");
        return client;
    }

    public Object login(String email, String password) throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.LOGIN);
        connection.sendObject(email);
        connection.sendObject(password);
        return answer();
    }

    public boolean ping() throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.PING);
        return connection.receiveCommand() == Commands.OK;
    }

    public void startRecording() throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.START_RECORDING);
        answerWithoutData();
    }

    public void sendBlock(int value, int size) throws IOException, ClassNotFoundException {
        int[][] block = new int[2][size];
        for (int i = 0; i < size; i++) {
            block[0][i] = value;
            block[1][i] = value;
        }
        connection.sendCommand(Commands.SIGNAL_DATA);
        connection.sendObject(block);
        answerWithoutData();
    }

    public void stopRecording() throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.STOP_RECORDING);
        answerWithoutData();
    }

    public PatientHistory history(Integer patientId) throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.GET_PATIENT_HISTORY);
        connection.sendObject(patientId);
        return (PatientHistory) answer();
    }

    public int[][] recordingData(Integer recordingId) throws IOException, ClassNotFoundException {
        connection.sendCommand(Commands.GET_RECORDING_DATA);
        connection.sendObject(recordingId);
        return (int[][]) answer();
    }

    public void logout() throws IOException {
        connection.sendCommand(Commands.LOGOUT);
        connection.close();
    }

    public void cut() throws IOException {
        connection.close();
    }

    private Object answer() throws IOException, ClassNotFoundException {
        Commands command = connection.receiveCommand();
        Object data = connection.receiveObject();
        if (command != Commands.OK) {
            throw new IllegalStateException("Server said: " + data);
        }
        return data;
    }

    private void answerWithoutData() throws IOException, ClassNotFoundException {
        Commands command = connection.receiveCommand();
        if (command != Commands.OK) {
            throw new IllegalStateException("Server said: " + connection.receiveObject());
        }
    }
}
