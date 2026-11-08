package server;

import commonInfo.Commands;
import commonInfo.SocketConnection;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private SocketConnection connection;
    private  HospitalServer server;

    public ClientHandler(Socket socket, HospitalServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            connection = new SocketConnection(socket);
            System.out.println("Client connected from " + socket.getInetAddress());

            boolean connected = true;
            while(connected) {
                Commands command = connection.receiveCommand();
                System.out.println("Received: " + command);

                if(command == Commands.LOGOUT) {
                    connected = false;
                } else {
                    connection.sendCommand(Commands.OK);
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
}