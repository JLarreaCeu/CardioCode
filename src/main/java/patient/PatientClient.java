package patient;

import commonInfo.Commands;
import commonInfo.SocketConnection;

import java.io.IOException;
import java.net.Socket;


/*conexion of patient client with hospital server*/
public class PatientClient {

    private static final int PORT = 6767;
    private final SocketConnection con;
    private final Socket socket;


    /** Se conecta al servidor. Si no puede, lanza IOException para que la GUI lo diga en pantalla. */
    public PatientClient(String host) throws IOException {
        socket = new Socket(host, PORT);
        con = new SocketConnection(socket);
    }

    public boolean isConnected() {
        return socket.isConnected();
    }




    public synchronized void logout() throws IOException {
        con.sendCommand(Commands.LOGOUT);
        con.close();
    }
}
