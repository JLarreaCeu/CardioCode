package server;

import commonInfo.Commands;
import commonInfo.SocketConnection;
import commonInfo.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class SocketTest {

    private static final String HOST = "localhost";
    private static final int PORT = 6767;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); //no se cierra, cerraria System.in
            System.out.println("Email of an existing patient:");
            String email = reader.readLine();
            System.out.println("Password:");
            String password = reader.readLine();
            if (email == null || password == null) {
                System.out.println("No input");
                return;
            }

            SocketConnection connection = new SocketConnection(new Socket(HOST, PORT));
            System.out.println("Connected to " + HOST + ":" + PORT);
            System.out.println();

            // 1. PING
            connection.sendCommand(Commands.PING);
            check("PING answers OK", connection.receiveCommand() == Commands.OK);

            // 2. LOGIN con contraseña mala
            connection.sendCommand(Commands.LOGIN);
            connection.sendObject(email.trim());
            connection.sendObject("wrong password");
            check("LOGIN with wrong password answers ERROR", readError(connection));

            // 3. Comando de medico sin login
            connection.sendCommand(Commands.GET_PATIENTS);
            check("GET_PATIENTS without login answers ERROR", readError(connection));

            // 4. Algo que no es un comando
            connection.sendObject("this is not a command");
            check("Something that is not a command answers ERROR", readError(connection));

            connection.sendCommand(Commands.PING);
            check("The server is still alive after that", connection.receiveCommand() == Commands.OK);

            // 5. LOGIN bueno
            connection.sendCommand(Commands.LOGIN);
            connection.sendObject(email.trim());
            connection.sendObject(password);
            Commands answer = connection.receiveCommand();
            Object data = connection.receiveObject();
            if (answer == Commands.OK) {
                User user = (User) data;
                check("LOGIN with right password answers OK + User", user.getEmail() != null);
                check("The User does not carry the password", user.getPassword() == null);
            } else {
                check("LOGIN with right password answers OK + User (server said: " + data + ")", false);
            }

            // 6. LOGIN otra vez
            connection.sendCommand(Commands.LOGIN);
            connection.sendObject(email.trim());
            connection.sendObject(password);
            check("Second LOGIN answers ERROR", readError(connection));

            // 7. Comando de medico siendo paciente
            connection.sendCommand(Commands.GET_PATIENTS);
            check("GET_PATIENTS as a patient answers ERROR", readError(connection));

            // 8. LOGOUT
            connection.sendCommand(Commands.LOGOUT);
            connection.close();

            System.out.println();
            System.out.println("Passed: " + passed + "  Failed: " + failed);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not talk to the server, is it running? " + e.getMessage());
        }
    }

    /**
     * Lee la respuesta y comprueba que es ERROR + motivo
     * @return true si el servidor respondio ERROR
     */
    private static boolean readError(SocketConnection connection) throws IOException, ClassNotFoundException {
        Commands answer = connection.receiveCommand();
        if (answer != Commands.ERROR) {
            return false;
        }
        Object reason = connection.receiveObject(); //despues de ERROR siempre va el motivo
        System.out.println("   server said: " + reason);
        return true;
    }

    /**
     * Imprime si la prueba ha pasado o no y lleva la cuenta
     */
    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }
}