package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HospitalServer {

    private static final int PORT = 6767;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private volatile boolean running = true; //volatile me salio investigando es para que todos los hilos vean si el servidor sigue encendido tipo para que los cambios de una var se vean al momento

    public static void main(String[] args) {
        HospitalServer server = new HospitalServer();
        server.start(); //abre server
    }

    /**
     * Abre el puerto y espera clients, cada client con su thread
     */
    public void start() {
        try {
            serverSocket = new ServerSocket (PORT);
            System.out.println("Server listening on port: " + PORT);

            while(running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                addClient(handler);
                new Thread(handler).start();
            }
        }catch(IOException e) {
            if (running) {
                System.out.println("Server error: " + e.getMessage());
            } else {
                System.out.println("Server stopped");
            }
        }
    }

    /**
     * Añade cliente a list
     * @param client
     */
     //synchronized hace que solo un thread pueda tocar la lista a la vez , es para quitar race conditions por si 2 clientes entran o salen a la vez
    public synchronized void addClient(ClientHandler client){
        clients.add(client);
        System.out.println("Connected clients: " + clients.size());
    }

    /**
     * Quita cliente de la list
     * @param client
     */
    public synchronized void removeClient (ClientHandler client){
        clients.remove(client);
        System.out.println("Connected clients: " + clients.size());
    }

    public synchronized int getConnectedClients(){
        return clients.size();
    }

    /**
     * Apaga el server
     */
    public void shutdown() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing server: " + e.getMessage());
        }
    }


}
