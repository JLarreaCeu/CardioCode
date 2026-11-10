package doctor;

import POJOs.User;
import commonInfo.Commands;
import commonInfo.Pings;
import commonInfo.SocketConnection;

import java.io.IOException;
import java.net.Socket;


/*conexion of doctor client with hospital server*/
public class DoctorClient implements Pings {

    private static final int PORT = 6767;
    private final SocketConnection con;
    private final Socket socket;


    /** Se conecta al servidor */
    public DoctorClient(String host) throws IOException {
        socket = new Socket(host, PORT);
        con = new SocketConnection(socket);
    }
    public boolean isConnected() {
        return socket.isConnected();
    }
    public synchronized User login(String email, String password) throws IOException {
        con.sendCommand(Commands.LOGIN);
        con.sendObject(email);
        con.sendObject(password);
        try {
        if(con.receiveCommand() == Commands.OK){
            return (User) con.receiveObject();}
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
   //public synchronized Patient getPatients(){

    //}


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
}

