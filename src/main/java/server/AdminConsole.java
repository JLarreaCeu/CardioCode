package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdminConsole implements Runnable {

    private static final Path ADMIN_FILE = Paths.get("db","admin.key");
    private HospitalServer server;

    public AdminConsole(HospitalServer server) {
        this.server = server;
    }

    /**
     * La primera vez que arranca el server, pide crear la contraseña del admin y verifica que hace match con su segunda instancia
     * @throws IOException
     */
    public static void setupAdminPassword() throws IOException {
        if(Files.exists(ADMIN_FILE)){
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        boolean creada = false;
        while(!creada) {
            System.out.println("No admin password yet. Create one:");
            String firstConfirmation = reader.readLine();
            System.out.println("Repeat the password:");
            String secondConfirmation = reader.readLine();

            if(firstConfirmation == null || secondConfirmation == null){
                throw new IOException("No password introduced in first or second time");
            } else if(firstConfirmation.length() < 4) {
                System.out.println("The password must have at least 4 characters");
            } else if(!firstConfirmation.equals(secondConfirmation)){
                System.out.println("Passwords dont match");
            } else {
                Files.createDirectories(ADMIN_FILE.getParent());
                Files.write(ADMIN_FILE, PasswordHasher.hash(firstConfirmation));
                System.out.println("Admin password created");
                creada = true;
            }
        }
    }

    /**
     * Pide contraseña de admin y la compara con el hash guardado
     * @return
     * @throws IOException
     */
    public static boolean checkAdminPassword() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Admin password:");
        String password = reader.readLine();
        if(password == null) {
            return false;
        }
        byte[] stored = Files.readAllBytes(ADMIN_FILE);
        return PasswordHasher.verify(password, stored);
    }



    public static boolean doubleCheck(String keyword) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Type " + keyword + ": ");
        String first = reader.readLine();
        if(first == null || !first.trim().equals(keyword)){
            return false;
        }

        System.out.println("Type CONFIRM:");
        String second = reader.readLine();
        if (second == null || !second.trim().equals("CONFIRM")) {
            return false;
        }
        return true;
    }

    @Override
    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); //no se cierra, cerraria System.in
        boolean active = true;
        try {
            while (active) {
                printMenu();
                String option = reader.readLine();

                if (option == null) {
                    active = false;
                } else {
                    option = option.trim();

                    if (option.equals("1")) {
                        System.out.println("Connected clients: " + server.getConnectedClients());
                    } else if (option.equals("2")) {
                        if(!checkAdminPassword()) {
                            System.out.println("Wrong password. Shutdown cancelled");
                        } else {
                            int connected = server.getConnectedClients();
                            if(connected > 0){
                                System.out.println("Warning: there are " +connected + "clientes connected. They will be disconnected");
                            }
                            if (doubleCheck("SHUTDOWN")) {
                                server.shutdown();
                                active = false;
                            } else {
                                System.out.println("Shutdown cancelled");
                            }
                        }
                    } else {
                        System.out.println("Invalid option");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading admin input: " + e.getMessage());
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("ADMIN MENU");
        System.out.println("1. Show connected clients");
        System.out.println("2. Shut down server");
        System.out.println("Choose an option:");
    }
}

