package commonInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Envía el comando de Commands
     * @param command
     * @throws IOException
     */
    public void sendCommand(Commands command) throws IOException{
        out.writeObject(command);
        out.flush();
        out.reset();
    }

    /**
     * Envía un dato
     * @param data
     * @throws IOException
     */
    public void sendObject(Object data) throws IOException {
        out.writeObject(data);
        out.flush();
        out.reset(); //para que no reutilice copia vieja de un objeto viejo
    }

    /**
     * Lee dato y convierte en Commands
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Commands receiveCommand() throws IOException, ClassNotFoundException {
        return (Commands) in.readObject(); //cast a tipo Commands
    }

    /**
     * Lee dato sin convertir
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Object receiveObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    /**
     * Cierra los streams y el socket
     */
    public void close(){
        try {
            in.close();
        } catch (IOException e) {
            System.out.println("Error closing input stream: " + e.getMessage());
        }
        try {
            out.close();
        } catch (IOException e) {
            System.out.println("Error closing output stream: " + e.getMessage());
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}
