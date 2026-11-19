package patient;

import commonInfo.*;

import java.io.IOError;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection between the patient application and the hospital server.
 */
public class PatientClient implements Pings {

    private static final int PORT = 6767;
    private final SocketConnection con;
    private final Socket socket;
    //User looged in through this connection.
    private User loggedUser;


    /** Se conecta al servidor. Si no puede, lanza IOException para que la GUI lo diga en pantalla. */
    public PatientClient(String host) throws IOException {
        socket = new Socket(host, PORT);
        con = new SocketConnection(socket);
    }

    public boolean isConnected() {
        return socket.isConnected();
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




    public synchronized void logout() throws IOException {
        con.sendCommand(Commands.LOGOUT);
        con.close();
    }

    /**
     * Error reported by the server, like a wrong password or duplicated email.
     */
    public static class ServerException extends Exception{
        public ServerException(String message){
            super(message);
        }
    }

    /**
     * Retrun the user logged in through this connection.
     * @return logged user or null if no one has logged in yet.
     */
    public User getLoggedUser(){
        return loggedUser;
    }

    /** Creates a new patient account
     *
     * @param email
     * @param password
     * @return
     * @throws IOException
     * @throws ServerException
     */
    public synchronized User login(String email,String password)throws IOException,ServerException{
        con.sendCommand(Commands.LOGIN);
        con.sendObject(email);
        con.sendObject(password);
        expectConfirmation();
        return (User) receive();
    }


    public synchronized List<Doctor>getDoctors()throws IOException,ServerException{
        con.sendCommand(Commands.GET_DOCTORS);
        expectConfirmation();
        return (List<Doctor>) receive();
    }

    public synchronized Patient register(String email,String password,Patient patient)throws IOException,ServerException{
        con.sendCommand(Commands.REGISTER_PATIENT);
        con.sendObject(email);
        con.sendObject(password);
        con.sendObject(patient);
        expectConfirmation();
        return (Patient) receive();
    }



    public synchronized void sendSymptoms(SymptomReport report)throws IOException,ServerException{
        con.sendCommand(Commands.SEND_SYMPTOMS);
        con.sendObject(report);
        expectConfirmation();
    }

    public synchronized void startRecording()throws IOException,ServerException{
        con.sendCommand(Commands.START_RECORDING);
        expectConfirmation();
    }

    public synchronized void sendSignalBlock(int []samples)throws IOException,ServerException{
        con.sendCommand(Commands.SIGNAL_DATA);
        con.sendObject(samples);
        expectConfirmation();
    }

    public synchronized void stopRecording()throws IOException,ServerException{
        con.sendCommand(Commands.STOP_RECORDING);
        expectConfirmation();
    }

    private void expectConfirmation()throws IOException,ServerException {
        Object answer=receive();
        if(answer==Commands.OK){
            return;

        }if(answer==Commands.ERROR){
            throw new ServerException(String.valueOf(receive()));
        }
        throw new IOException("Unexpected answer from server: "+answer);

    }

    private Object receive() throws IOError, IOException {
        try{
            return con.receiveObject();

        }catch (ClassNotFoundException e){
            throw new IOException("Invalid data received from server", e);
        }

    }

}
