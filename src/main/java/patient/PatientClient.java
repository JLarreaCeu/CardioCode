package patient;

import commonInfo.Commands;
import commonInfo.Pings;
import commonInfo.SocketConnection;

import java.io.IOException;
import java.net.Socket;


/*conexion of patient client with hospital server*/
public class PatientClient implements Pings {

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


}
