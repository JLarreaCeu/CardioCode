package server;

import commonInfo.Commands;
import commonInfo.Role;
import commonInfo.SocketConnection;
import commonInfo.User;
import server.db.interfaces.UserManager;
import server.db.jdbc.JDBCUserManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private SocketConnection connection;
    private  HospitalServer server;
    private UserManager userManager;
    private User loggedUser;

    public ClientHandler(Socket socket, HospitalServer server) {
        this.socket = socket;
        this.server = server;
        this.userManager = new JDBCUserManager();
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
                    }else if (command == Commands.GET_PATIENTS){
                        if(hasRole(Role.DOCTOR)){
                            sendError("Command not available yet");
                        }
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

}