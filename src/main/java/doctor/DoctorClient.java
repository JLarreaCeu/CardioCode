package doctor;

import POJOs.User;
import commonInfo.Commands;
import commonInfo.SocketConnection;

import java.io.IOException;
import java.net.Socket;


/*conexion of doctor client with hospital server*/
public class DoctorClient {

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
}

