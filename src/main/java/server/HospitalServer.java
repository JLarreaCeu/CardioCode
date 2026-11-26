package server;

import commonInfo.Config;
import commonInfo.ServerFinder;
import server.db.jdbc.JDBCManager;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HospitalServer {

    private static final int PORT = Config.getServerPort();
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private volatile boolean running = true; //volatile me salio investigando es para que todos los hilos vean si el servidor sigue encendido tipo para que los cambios de una var se vean al momento
    private DiscoveryResponder responder;

    public static void main(String[] args) {
        try {
            new JDBCManager();
            AdminConsole.setupAdminPassword();
            if (AdminConsole.doubleCheck("OPEN")) {
                String otherServer = ServerFinder.findServer();
                if (otherServer != null) {
                    System.out.println("There is already a server running at " + otherServer);
                    return;
                }
                HospitalServer server = new HospitalServer();
                if (server.open()) {
                    AdminConsole console = new AdminConsole(server);
                    new Thread(console).start();
                    new Thread(server.responder).start();
                    server.start();
                }
            } else {
                System.out.println("Server not started");
            }
        } catch (IOException e) {
            System.out.println("Error reading input, server not started: " + e.getMessage());
        }
    }

    /**
     * Abre el puerto y espera clients, cada client con su thread
     */
    public void start() {
        try {
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                addClient(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Server error: " + e.getMessage());
            } else {
                System.out.println("Server stopped");
            }
        }
    }

    /**
     * Añade cliente a list
     *
     * @param client
     */
    //synchronized hace que solo un thread pueda tocar la lista a la vez , es para quitar race conditions por si 2 clientes entran o salen a la vez
    public synchronized void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Connected clients: " + clients.size());
    }

    /**
     * Quita cliente de la list
     *
     * @param client
     */
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Connected clients: " + clients.size());
    }

    public synchronized int getConnectedClients() {
        return clients.size();
    }

    /**
     * Apaga el server
     */
    public void shutdown() {
        running = false;
        if (responder != null) {
            responder.stop();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server: " + e.getMessage());
            }
        }

        List<ClientHandler> copy;
        synchronized (this) {
            copy = new ArrayList<>(clients);
        }
        for (ClientHandler client : copy) {
            client.closeConnection();
        }
    }

    public boolean open() {
        try {
            serverSocket = new ServerSocket(PORT);
            responder = new DiscoveryResponder();
            System.out.println("Server listening on port: " + PORT);
            return true;
        } catch (BindException e) {
            System.out.println("Port already in use: another server is already running on this computer");
        } catch (IOException e) {
            System.out.println("Could not open the server: " + e.getMessage());
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server: " + e.getMessage());
            }
        }
        return false;
    }
}
