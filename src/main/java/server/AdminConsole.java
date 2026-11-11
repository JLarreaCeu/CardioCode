package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdminConsole implements Runnable {
    private HospitalServer server;

    public AdminConsole(HospitalServer server) {
        this.server = server;
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
                        if (doubleCheck("SHUTDOWN")) {
                            server.shutdown();
                            active = false;
                        } else {
                            System.out.println("Shutdown cancelled");
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
    public void pruebaVideo(){}
}
